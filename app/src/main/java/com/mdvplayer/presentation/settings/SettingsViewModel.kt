package com.mdvplayer.presentation.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mdvplayer.domain.repository.MusicRepository
import com.mdvplayer.service.MusicController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val musicController: MusicController
) : ViewModel() {

    private val _folderPath = MutableStateFlow<String?>(null)
    val folderPath: StateFlow<String?> = _folderPath.asStateFlow()

    init {
        viewModelScope.launch {
            musicRepository.getFolderUri().collect { uri ->
                _folderPath.value = uri?.path
            }
        }
    }

    fun onFolderSelected(uri: Uri) {
        viewModelScope.launch {
            try {
                musicRepository.saveFolderUri(uri)
                musicRepository.scanAndSaveSongs(uri)
                
                // Get the newly scanned songs and update the player's playlist
                val songs = musicRepository.getSongs().first()
                if (songs.isNotEmpty()) {
                    musicController.playFromList(songs, 0)
                }
            } catch (_: Exception) {
                // Handle error if needed
            }
        }
    }
}
