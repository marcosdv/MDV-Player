package com.mdvplayer.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "mdv_settings")

@Singleton
class PreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val FOLDER_URI_KEY = stringPreferencesKey("folder_uri")
        private val SHUFFLE_ENABLED_KEY = booleanPreferencesKey("shuffle_enabled")
        private val DARK_THEME_KEY = booleanPreferencesKey("dark_theme")
        private val AUDIO_SESSION_ID_KEY = intPreferencesKey("audio_session_id")
    }

    val folderUri: Flow<String?> = context.dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs -> prefs[FOLDER_URI_KEY] }

    val shuffleEnabled: Flow<Boolean> = context.dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs -> prefs[SHUFFLE_ENABLED_KEY] ?: false }

    val darkTheme: Flow<Boolean> = context.dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs -> prefs[DARK_THEME_KEY] ?: false }

    val audioSessionId: Flow<Int> = context.dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs -> prefs[AUDIO_SESSION_ID_KEY] ?: 0 }

    suspend fun saveFolderUri(uri: String) {
        context.dataStore.edit { prefs -> prefs[FOLDER_URI_KEY] = uri }
    }

    suspend fun saveShuffleEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[SHUFFLE_ENABLED_KEY] = enabled }
    }

    suspend fun saveDarkTheme(isDark: Boolean) {
        context.dataStore.edit { prefs -> prefs[DARK_THEME_KEY] = isDark }
    }

    suspend fun saveAudioSessionId(sessionId: Int) {
        context.dataStore.edit { prefs -> prefs[AUDIO_SESSION_ID_KEY] = sessionId }
    }
}
