package com.mdvplayer.domain.repository

import android.net.Uri
import com.mdvplayer.domain.model.Song
import kotlinx.coroutines.flow.Flow

interface MusicRepository {
    fun getSongs(): Flow<List<Song>>
    fun getFolderUri(): Flow<Uri?>
    fun getShuffleEnabled(): Flow<Boolean>
    fun getDarkTheme(): Flow<Boolean>
    suspend fun getSongByUri(uri: String): Song?
    suspend fun scanAndSaveSongs(folderUri: Uri)
    suspend fun saveFolderUri(uri: Uri)
    suspend fun saveShuffleEnabled(enabled: Boolean)
    suspend fun saveDarkTheme(darkTheme: Boolean)
}
