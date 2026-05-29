package com.mdvplayer.presentation.songlist

import com.mdvplayer.domain.model.Song

data class SongListUiState(
    val songs: List<Song> = emptyList(),
    val isLoading: Boolean = false,
    val isScanning: Boolean = false,
    val folderPath: String? = null,
    val error: String? = null
)
