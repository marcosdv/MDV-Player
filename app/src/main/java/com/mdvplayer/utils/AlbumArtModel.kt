package com.mdvplayer.utils

import android.net.Uri

/**
 * Wrapper used as a Coil model to load album art embedded in audio files.
 */
data class AlbumArtModel(val audioUri: Uri)
