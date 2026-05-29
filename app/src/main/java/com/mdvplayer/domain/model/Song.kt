package com.mdvplayer.domain.model

data class Song(
    val id: Long = 0,
    val title: String,
    val artist: String,
    val album: String,
    val uri: String,
    val duration: Long,
    val hasAlbumArt: Boolean = false
) {
    fun formattedDuration(): String {
        val totalSeconds = duration / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%d:%02d".format(minutes, seconds)
    }
}
