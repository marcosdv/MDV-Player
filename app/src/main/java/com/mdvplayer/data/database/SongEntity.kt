package com.mdvplayer.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mdvplayer.domain.model.Song

@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val artist: String,
    val album: String,
    val uri: String,
    val duration: Long,
    val hasAlbumArt: Boolean
) {
    fun toSong() = Song(
        id = id,
        title = title,
        artist = artist,
        album = album,
        uri = uri,
        duration = duration,
        hasAlbumArt = hasAlbumArt
    )
}

fun Song.toEntity() = SongEntity(
    id = 0, // auto-generate
    title = title,
    artist = artist,
    album = album,
    uri = uri,
    duration = duration,
    hasAlbumArt = hasAlbumArt
)
