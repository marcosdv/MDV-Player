package com.mdvplayer.data.repository

import android.content.Intent
import android.net.Uri
import com.mdvplayer.data.database.SongDao
import com.mdvplayer.data.database.toEntity
import com.mdvplayer.data.datastore.PreferencesDataStore
import com.mdvplayer.data.scanner.MusicScanner
import com.mdvplayer.domain.model.Song
import com.mdvplayer.domain.repository.MusicRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext

@Singleton
class MusicRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val songDao: SongDao,
    private val preferencesDataStore: PreferencesDataStore,
    private val musicScanner: MusicScanner
) : MusicRepository {

    override fun getSongs(): Flow<List<Song>> =
        songDao.getAllSongs().map { entities -> entities.map { it.toSong() } }

    override fun getFolderUri(): Flow<Uri?> =
        preferencesDataStore.folderUri.map { uriString ->
            uriString?.let { Uri.parse(it) }
        }

    override fun getShuffleEnabled(): Flow<Boolean> = preferencesDataStore.shuffleEnabled

    override fun getDarkTheme(): Flow<Boolean> = preferencesDataStore.darkTheme

    override suspend fun getSongByUri(uri: String): Song? =
        songDao.getSongByUri(uri)?.toSong()

    override suspend fun scanAndSaveSongs(folderUri: Uri) {
        val songs = musicScanner.scanFolder(folderUri)
        songDao.deleteAll()
        songDao.insertAll(songs.map { it.toEntity() })
    }

    override suspend fun saveFolderUri(uri: Uri) {
        // Take persistent permission so we can access the URI across restarts
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: SecurityException) {}
        preferencesDataStore.saveFolderUri(uri.toString())
    }

    override suspend fun saveShuffleEnabled(enabled: Boolean) {
        preferencesDataStore.saveShuffleEnabled(enabled)
    }

    override suspend fun saveDarkTheme(darkTheme: Boolean) {
        preferencesDataStore.saveDarkTheme(darkTheme)
    }
}
