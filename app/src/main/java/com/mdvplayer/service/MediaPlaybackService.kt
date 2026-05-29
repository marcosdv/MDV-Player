package com.mdvplayer.service

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.*
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.mdvplayer.MainActivity
import com.mdvplayer.data.datastore.PreferencesDataStore
import com.mdvplayer.domain.model.Song
import com.mdvplayer.domain.repository.MusicRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@AndroidEntryPoint
class MediaPlaybackService : MediaLibraryService() {

    @Inject lateinit var musicRepository: MusicRepository
    @Inject lateinit var preferencesDataStore: PreferencesDataStore

    private lateinit var player: ExoPlayer
    private lateinit var mediaLibrarySession: MediaLibrarySession
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var cachedSongs: List<Song> = emptyList()

    companion object {
        const val ROOT_ID = "MDV_ROOT"
        const val SONGS_ID = "MDV_SONGS"
        const val CUSTOM_COMMAND_REWIND_30 = "REWIND_30"
        const val CUSTOM_COMMAND_FORWARD_30 = "FORWARD_30"
        const val CUSTOM_COMMAND_SHUFFLE = "SHUFFLE"
    }

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, /* handleAudioFocus= */ true)
            .setHandleAudioBecomingNoisy(true)
            .setSeekBackIncrementMs(30_000L)
            .setSeekForwardIncrementMs(30_000L)
            .build()

        // Persist audio session ID so Equalizer can connect
        serviceScope.launch {
            preferencesDataStore.saveAudioSessionId(player.audioSessionId)
        }

        // Restore shuffle state
        serviceScope.launch {
            val shuffleEnabled = preferencesDataStore.shuffleEnabled.first()
            player.shuffleModeEnabled = shuffleEnabled
        }

        // Listen for shuffle changes to persist them
        player.addListener(object : Player.Listener {
            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                serviceScope.launch {
                    preferencesDataStore.saveShuffleEnabled(shuffleModeEnabled)
                }
            }
        })

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        mediaLibrarySession = MediaLibrarySession.Builder(this, player, LibrarySessionCallback())
            .setSessionActivity(pendingIntent)
            .build()

        // Observe songs for Android Auto browsing and initial loading
        serviceScope.launch {
            musicRepository.getSongs()
                .distinctUntilChanged()
                .collect { songs ->
                    if (songs == cachedSongs) return@collect
                    val wasEmpty = cachedSongs.isEmpty()
                    cachedSongs = songs

                    // Load items if the player is currently empty
                    if (player.mediaItemCount == 0 && songs.isNotEmpty()) {
                        val mediaItems = songs.map { it.toMediaItem() }
                        player.setMediaItems(mediaItems)
                        player.prepare()
                    }

                    // Notify browser if list changed
                    if (!wasEmpty) {
                        mediaLibrarySession.notifyChildrenChanged(SONGS_ID, songs.size, null)
                    }
                }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession {
        return mediaLibrarySession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        player.pause()
        stopSelf()
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        mediaLibrarySession.release()
        player.release()
        serviceScope.cancel()
        super.onDestroy()
    }

    private inner class LibrarySessionCallback : MediaLibrarySession.Callback {

        @OptIn(UnstableApi::class)
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            val connectionResult = super.onConnect(session, controller)
            val sessionCommands = connectionResult.availableSessionCommands.buildUpon()
                .add(SessionCommand(CUSTOM_COMMAND_REWIND_30, Bundle.EMPTY))
                .add(SessionCommand(CUSTOM_COMMAND_FORWARD_30, Bundle.EMPTY))
                .add(SessionCommand(CUSTOM_COMMAND_SHUFFLE, Bundle.EMPTY))
                .build()

            val playerCommands = connectionResult.availablePlayerCommands.buildUpon()
                .add(Player.COMMAND_SET_SHUFFLE_MODE)
                .build()

            val rewindButton = CommandButton.Builder()
                .setSessionCommand(SessionCommand(CUSTOM_COMMAND_REWIND_30, Bundle.EMPTY))
                .setIconResId(com.mdvplayer.R.drawable.ic_rewind_30)
                .setDisplayName("Voltar 30s")
                .build()

            val shuffleButton = CommandButton.Builder()
                .setSessionCommand(SessionCommand(CUSTOM_COMMAND_SHUFFLE, Bundle.EMPTY))
                .setIconResId(com.mdvplayer.R.drawable.ic_shuffle)
                .setDisplayName("Aleatório")
                .build()

            val forwardButton = CommandButton.Builder()
                .setSessionCommand(SessionCommand(CUSTOM_COMMAND_FORWARD_30, Bundle.EMPTY))
                .setIconResId(com.mdvplayer.R.drawable.ic_forward_30)
                .setDisplayName("Avançar 30s")
                .build()

            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(sessionCommands)
                .setAvailablePlayerCommands(playerCommands)
                .setCustomLayout(ImmutableList.of(rewindButton, shuffleButton, forwardButton))
                .build()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            when (customCommand.customAction) {
                CUSTOM_COMMAND_REWIND_30 -> player.seekBack()
                CUSTOM_COMMAND_FORWARD_30 -> player.seekForward()
                CUSTOM_COMMAND_SHUFFLE -> player.shuffleModeEnabled = !player.shuffleModeEnabled
                else -> return super.onCustomCommand(session, controller, customCommand, args)
            }
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val rootItem = MediaItem.Builder()
                .setMediaId(ROOT_ID)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .setTitle("MDV Player")
                        .build()
                )
                .build()
            return Futures.immediateFuture(LibraryResult.ofItem(rootItem, params))
        }

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            return when (parentId) {
                ROOT_ID -> {
                    val rootChildren = ImmutableList.of(
                        MediaItem.Builder()
                            .setMediaId(SONGS_ID)
                            .setMediaMetadata(
                                MediaMetadata.Builder()
                                    .setIsBrowsable(true)
                                    .setIsPlayable(false)
                                    .setTitle("Músicas")
                                    .build()
                            )
                            .build()
                    )
                    Futures.immediateFuture(LibraryResult.ofItemList(rootChildren, params))
                }
                SONGS_ID -> {
                    val start = page * pageSize
                    val items = cachedSongs
                        .drop(start)
                        .take(pageSize)
                        .map { it.toMediaItem() }
                    Futures.immediateFuture(
                        LibraryResult.ofItemList(ImmutableList.copyOf(items), params)
                    )
                }
                else -> Futures.immediateFuture(
                    LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE)
                )
            }
        }

        override fun onGetItem(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            mediaId: String
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val song = cachedSongs.find { it.uri == mediaId }
            return if (song != null) {
                Futures.immediateFuture(LibraryResult.ofItem(song.toMediaItem(), null))
            } else {
                Futures.immediateFuture(LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE))
            }
        }

        override fun onAddMediaItems(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: List<MediaItem>
        ): ListenableFuture<List<MediaItem>> {
            // Resolve media items with playback URIs (needed for Android Auto)
            val resolved = mediaItems.map { item ->
                val song = cachedSongs.find { it.uri == item.mediaId }
                if (song != null) {
                    item.buildUpon()
                        .setUri(Uri.parse(song.uri))
                        .build()
                } else item
            }
            return Futures.immediateFuture(resolved)
        }
    }
}

fun Song.toMediaItem(): MediaItem {
    val artworkUri = if (hasAlbumArt) {
        Uri.parse(uri)
    } else {
        Uri.parse("android.resource://com.mdvplayer/drawable/logo_mdv")
    }

    return MediaItem.Builder()
        .setMediaId(uri)
        .setUri(Uri.parse(uri))
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setAlbumTitle(album)
                .setArtworkUri(artworkUri)
                .setIsBrowsable(false)
                .setIsPlayable(true)
                .build()
        )
        .build()
}
