package com.mdvplayer.presentation.navigation

sealed class Screen(val route: String) {
    object Player : Screen("player")
    object SongList : Screen("song_list")
    object Equalizer : Screen("equalizer")
}
