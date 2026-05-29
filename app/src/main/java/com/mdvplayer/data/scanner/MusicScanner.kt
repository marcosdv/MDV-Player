package com.mdvplayer.data.scanner

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.mdvplayer.domain.model.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicScanner @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val AUDIO_MIME_PREFIXES = listOf("audio/")
        private val AUDIO_EXTENSIONS = setOf(
            "mp3", "flac", "aac", "ogg", "wav", "m4a", "opus", "wma", "aiff"
        )
    }

    suspend fun scanFolder(folderUri: Uri): List<Song> = withContext(Dispatchers.IO) {
        val songs = mutableListOf<Song>()
        val rootDocument = DocumentFile.fromTreeUri(context, folderUri) ?: return@withContext songs
        scanDocumentDirectory(rootDocument, songs)
        songs.sortedBy { it.title }
    }

    private fun scanDocumentDirectory(folder: DocumentFile, songs: MutableList<Song>) {
        folder.listFiles().forEach { file ->
            when {
                file.isDirectory -> scanDocumentDirectory(file, songs)
                isAudioFile(file) -> extractSongInfo(file)?.let { songs.add(it) }
            }
        }
    }

    private fun isAudioFile(file: DocumentFile): Boolean {
        val mimeType = file.type ?: return false
        if (AUDIO_MIME_PREFIXES.any { mimeType.startsWith(it) }) return true
        val name = file.name ?: return false
        val ext = name.substringAfterLast('.', "").lowercase()
        return ext in AUDIO_EXTENSIONS
    }

    private fun extractSongInfo(file: DocumentFile): Song? {
        val uri = file.uri
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val rawTitle = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            val title = if (rawTitle.isNullOrBlank()) {
                file.name?.substringBeforeLast('.') ?: "Unknown"
            } else rawTitle.trim()

            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                ?.takeIf { it.isNotBlank() } ?: "Artista Desconhecido"
            val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                ?.takeIf { it.isNotBlank() } ?: "Álbum Desconhecido"
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
            val hasAlbumArt = retriever.embeddedPicture != null

            Song(
                title = title,
                artist = artist,
                album = album,
                uri = uri.toString(),
                duration = duration,
                hasAlbumArt = hasAlbumArt
            )
        } catch (e: Exception) {
            null
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }
    }
}
