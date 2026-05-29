package com.mdvplayer.presentation.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mdvplayer.domain.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val musicRepository: MusicRepository
) : ViewModel() {

    fun onFolderSelected(uri: Uri) {
        viewModelScope.launch {
            try {
                musicRepository.saveFolderUri(uri)
                musicRepository.scanAndSaveSongs(uri)
            } catch (_: Exception) {
                // Handle error if needed
            }
        }
    }
}
