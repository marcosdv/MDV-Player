package com.mdvplayer.presentation.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.mdvplayer.domain.model.Song
import com.mdvplayer.domain.repository.MusicRepository
import com.mdvplayer.service.MusicController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val musicController: MusicController,
    private val musicRepository: MusicRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _uiState.update { it.copy(isPlaying = isPlaying) }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            mediaItem?.mediaId?.let { uri -> loadSongByUri(uri) }
            updateDuration()
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            _uiState.update { it.copy(shuffleEnabled = shuffleModeEnabled) }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            _uiState.update { it.copy(playbackState = playbackState) }
            if (playbackState == Player.STATE_READY) updateDuration()
        }
    }

    init {
        // Wait for the controller and then attach the listener
        viewModelScope.launch {
            musicController.isConnected.collect { connected ->
                if (connected) {
                    val ctrl = musicController.controller ?: return@collect
                    musicController.addListener(playerListener)
                    // Sync initial state
                    _uiState.update {
                        it.copy(
                            isPlaying = ctrl.isPlaying,
                            shuffleEnabled = ctrl.shuffleModeEnabled,
                            durationMs = ctrl.duration.coerceAtLeast(0L),
                            isLoading = false
                        )
                    }
                    ctrl.currentMediaItem?.mediaId?.let { loadSongByUri(it) }
                }
            }
        }

        // Restore shuffle from DataStore
        viewModelScope.launch {
            musicRepository.getShuffleEnabled().collect { enabled ->
                _uiState.update { it.copy(shuffleEnabled = enabled) }
            }
        }

        // Periodic position update
        viewModelScope.launch {
            while (isActive) {
                val ctrl = musicController.controller
                if (ctrl != null && ctrl.isPlaying) {
                    _uiState.update {
                        it.copy(
                            currentPositionMs = ctrl.currentPosition.coerceAtLeast(0L),
                            durationMs = ctrl.duration.coerceAtLeast(0L)
                        )
                    }
                }
                delay(500)
            }
        }
    }

    private fun loadSongByUri(uri: String) {
        viewModelScope.launch {
            val song = musicRepository.getSongByUri(uri)
            _uiState.update { it.copy(currentSong = song) }
        }
    }

    private fun updateDuration() {
        val duration = musicController.controller?.duration?.coerceAtLeast(0L) ?: 0L
        _uiState.update { it.copy(durationMs = duration) }
    }

    fun playPause() {
        val ctrl = musicController.controller ?: return
        if (ctrl.isPlaying) musicController.pause() else musicController.play()
    }

    fun seekForward() = musicController.seekForward()
    fun seekBack() = musicController.seekBack()
    fun next() = musicController.next()
    fun previous() = musicController.previous()

    fun seekTo(positionMs: Long) {
        musicController.seekTo(positionMs)
        _uiState.update { it.copy(currentPositionMs = positionMs) }
    }

    fun toggleShuffle() {
        val newValue = !_uiState.value.shuffleEnabled
        musicController.setShuffleMode(newValue)
        _uiState.update { it.copy(shuffleEnabled = newValue) }
        viewModelScope.launch { musicRepository.saveShuffleEnabled(newValue) }
    }

    fun toggleDarkTheme(isDark: Boolean) {
        viewModelScope.launch { musicRepository.saveDarkTheme(isDark) }
    }

    fun playSong(songs: List<Song>, startIndex: Int) {
        musicController.playFromList(songs, startIndex)
    }

    override fun onCleared() {
        musicController.removeListener(playerListener)
        super.onCleared()
    }
}
