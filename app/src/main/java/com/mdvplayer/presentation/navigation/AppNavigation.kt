package com.mdvplayer.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.mdvplayer.presentation.equalizer.EqualizerScreen
import com.mdvplayer.presentation.player.PlayerScreen
import com.mdvplayer.presentation.songlist.SongListScreen

@Composable
fun AppNavigation(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Player.route
    ) {
        composable(Screen.Player.route) {
            PlayerScreen(
                onNavigateToSongList = { navController.navigate(Screen.SongList.route) },
                onNavigateToEqualizer = { navController.navigate(Screen.Equalizer.route) }
            )
        }
        composable(Screen.SongList.route) {
            SongListScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPlayer = {
                    navController.navigate(Screen.Player.route) {
                        popUpTo(Screen.Player.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Equalizer.route) {
            EqualizerScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
