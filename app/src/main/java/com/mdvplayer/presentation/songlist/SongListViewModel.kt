package com.mdvplayer.presentation.songlist

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mdvplayer.domain.model.Song
import com.mdvplayer.domain.repository.MusicRepository
import com.mdvplayer.service.MusicController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SongListViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val musicController: MusicController
) : ViewModel() {

    private val _uiState = MutableStateFlow(SongListUiState(isLoading = true))
    val uiState: StateFlow<SongListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            musicRepository.getSongs().collect { songs ->
                _uiState.update { it.copy(songs = songs, isLoading = false) }
            }
        }
        viewModelScope.launch {
            musicRepository.getFolderUri().collect { uri ->
                _uiState.update { it.copy(folderPath = uri?.lastPathSegment) }
            }
        }
    }

    fun onFolderSelected(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true, error = null) }
            try {
                musicRepository.saveFolderUri(uri)
                musicRepository.scanAndSaveSongs(uri)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Erro ao verificar pasta: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isScanning = false) }
            }
        }
    }

    fun onSongSelected(song: Song) {
        val songs = _uiState.value.songs
        val index = songs.indexOfFirst { it.uri == song.uri }.coerceAtLeast(0)
        musicController.playFromList(songs, index)
    }
}
