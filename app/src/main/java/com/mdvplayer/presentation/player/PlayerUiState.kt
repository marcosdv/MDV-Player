package com.mdvplayer.presentation.player

import com.mdvplayer.domain.model.Song

data class PlayerUiState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val shuffleEnabled: Boolean = false,
    val isLoading: Boolean = true,
    val playbackState: Int = 0 // Player.STATE_*
) {
    val progress: Float
        get() = if (durationMs > 0) currentPositionMs.toFloat() / durationMs.toFloat() else 0f
}
