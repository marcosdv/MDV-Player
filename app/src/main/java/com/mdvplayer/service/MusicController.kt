package com.mdvplayer.service

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.mdvplayer.di.ApplicationScope
import com.mdvplayer.domain.model.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicController @Inject constructor(
    @ApplicationContext private val context: Context,
    @ApplicationScope private val scope: CoroutineScope
) {
    private var _controller: MediaController? = null
    val controller: MediaController? get() = _controller

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected

    init {
        scope.launch { connect() }
    }

    private suspend fun connect() {
        try {
            val token = SessionToken(
                context,
                ComponentName(context, MediaPlaybackService::class.java)
            )
            _controller = MediaController.Builder(context, token).buildAsync().await()
            _isConnected.value = true
        } catch (_: Exception) {}
    }

    // ── Playback controls ────────────────────────────────────────────────────

    fun play() = _controller?.play()
    fun pause() = _controller?.pause()
    fun seekForward() = _controller?.let { it.seekTo(it.currentPosition + 30_000L) }
    fun seekBack() = _controller?.let { it.seekTo((it.currentPosition - 30_000L).coerceAtLeast(0L)) }
    fun next() = _controller?.seekToNextMediaItem()
    fun previous() = _controller?.seekToPreviousMediaItem()
    fun seekTo(positionMs: Long) = _controller?.seekTo(positionMs)

    fun setShuffleMode(enabled: Boolean) {
        _controller?.shuffleModeEnabled = enabled
    }

    fun playFromList(songs: List<Song>, startIndex: Int) {
        _controller?.let { ctrl ->
            val items = songs.map { it.toMediaItem() }
            ctrl.setMediaItems(items, startIndex, 0L)
            ctrl.prepare()
            ctrl.play()
        }
    }

    fun addListener(listener: Player.Listener) = _controller?.addListener(listener)
    fun removeListener(listener: Player.Listener) = _controller?.removeListener(listener)

    fun release() {
        _controller?.release()
        _controller = null
        _isConnected.value = false
    }
}
