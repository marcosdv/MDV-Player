package com.mdvplayer.utils

import android.media.MediaMetadataRetriever
import coil.ImageLoader
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.request.Options
import okio.Buffer

/**
 * Coil Fetcher that extracts embedded album art from audio files using
 * [MediaMetadataRetriever]. Works fully offline – no network access.
 */
class AlbumArtFetcher(
    private val data: AlbumArtModel,
    private val options: Options
) : Fetcher {

    override suspend fun fetch(): FetchResult? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(options.context, data.audioUri)
            val bytes = retriever.embeddedPicture ?: return null
            SourceResult(
                source = ImageSource(
                    source = Buffer().apply { write(bytes) },
                    context = options.context
                ),
                mimeType = "image/jpeg",
                dataSource = DataSource.DISK
            )
        } catch (_: Exception) {
            null
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }
    }

    class Factory : Fetcher.Factory<AlbumArtModel> {
        override fun create(
            data: AlbumArtModel,
            options: Options,
            imageLoader: ImageLoader
        ): Fetcher = AlbumArtFetcher(data, options)
    }
}
