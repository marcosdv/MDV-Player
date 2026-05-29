package com.mdvplayer.presentation.player

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.res.stringResource
import com.mdvplayer.R
import com.mdvplayer.presentation.theme.LocalDarkTheme
import com.mdvplayer.utils.AlbumArtModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    onNavigateToSongList: () -> Unit,
    onNavigateToEqualizer: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val colorScheme = MaterialTheme.colorScheme
    val isDark = LocalDarkTheme.current
    var showMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MDV Player") },
                actions = {
                    // Dark / Light theme toggle
                    IconButton(onClick = { viewModel.toggleDarkTheme(!isDark) }) {
                        Icon(
                            imageVector = if (isDark) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                            contentDescription = "Alternar tema"
                        )
                    }
                    IconButton(onClick = onNavigateToEqualizer) {
                        Icon(
                            imageVector = Icons.Rounded.Tune,
                            contentDescription = "Equalizador"
                        )
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                imageVector = Icons.Rounded.MoreVert,
                                contentDescription = "Menu"
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.settings)) },
                                onClick = {
                                    showMenu = false
                                    onNavigateToSettings()
                                },
                                leadingIcon = {
                                    Icon(Icons.Rounded.Settings, contentDescription = null)
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colorScheme.background
                )
            )
        },
        containerColor = colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Album Art
            AlbumArtCard(
                songUri = uiState.currentSong?.uri,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(20.dp))
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Song Title & Artist
            Text(
                text = uiState.currentSong?.title ?: "Nenhuma música",
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                color = colorScheme.onBackground,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = uiState.currentSong?.artist ?: "—",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                color = colorScheme.onBackground.copy(alpha = 0.7f),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Progress Slider
            ProgressSection(
                positionMs = uiState.currentPositionMs,
                durationMs = uiState.durationMs,
                onSeek = { viewModel.seekTo(it) }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Controls
            ControlsSection(
                isPlaying = uiState.isPlaying,
                shuffleEnabled = uiState.shuffleEnabled,
                onPlayPause = { viewModel.playPause() },
                onNext = { viewModel.next() },
                onPrevious = { viewModel.previous() },
                onSeekForward = { viewModel.seekForward() },
                onSeekBack = { viewModel.seekBack() },
                onToggleShuffle = { viewModel.toggleShuffle() }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Open Song List
            OutlinedButton(
                onClick = onNavigateToSongList,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Rounded.LibraryMusic, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Lista de Músicas")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun AlbumArtCard(songUri: String?, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Box(
        modifier = modifier
            .shadow(8.dp, RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(songUri?.let { AlbumArtModel(Uri.parse(it)) } ?: R.drawable.logo_mdv)
                .error(R.drawable.logo_mdv)
                .fallback(R.drawable.logo_mdv)
                .crossfade(true)
                .build(),
            contentDescription = "Capa do álbum",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun ProgressSection(
    positionMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit
) {
    val progress = if (durationMs > 0) positionMs.toFloat() / durationMs.toFloat() else 0f

    Column(modifier = Modifier.fillMaxWidth()) {
        Slider(
            value = progress.coerceIn(0f, 1f),
            onValueChange = { fraction ->
                onSeek((fraction * durationMs).toLong())
            },
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatDuration(positionMs),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
            Text(
                text = formatDuration(durationMs),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun ControlsSection(
    isPlaying: Boolean,
    shuffleEnabled: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeekForward: () -> Unit,
    onSeekBack: () -> Unit,
    onToggleShuffle: () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    val onBackground = MaterialTheme.colorScheme.onBackground

    // Main controls row
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Seek back 30s
        IconButton(onClick = onSeekBack) {
            Icon(
                imageVector = Icons.Rounded.Replay30,
                contentDescription = "Retroceder 30s",
                tint = onBackground,
                modifier = Modifier.size(32.dp)
            )
        }

        // Previous
        IconButton(onClick = onPrevious) {
            Icon(
                imageVector = Icons.Rounded.SkipPrevious,
                contentDescription = "Anterior",
                tint = onBackground,
                modifier = Modifier.size(40.dp)
            )
        }

        // Play / Pause
        FilledIconButton(
            onClick = onPlayPause,
            modifier = Modifier.size(64.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = primary
            )
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = if (isPlaying) "Pausar" else "Play",
                modifier = Modifier.size(36.dp)
            )
        }

        // Next
        IconButton(onClick = onNext) {
            Icon(
                imageVector = Icons.Rounded.SkipNext,
                contentDescription = "Próxima",
                tint = onBackground,
                modifier = Modifier.size(40.dp)
            )
        }

        // Seek forward 30s
        IconButton(onClick = onSeekForward) {
            Icon(
                imageVector = Icons.Rounded.Forward30,
                contentDescription = "Avançar 30s",
                tint = onBackground,
                modifier = Modifier.size(32.dp)
            )
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Shuffle row
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onToggleShuffle) {
            Icon(
                imageVector = Icons.Rounded.Shuffle,
                contentDescription = "Aleatório",
                tint = if (shuffleEnabled) primary else onBackground.copy(alpha = 0.4f),
                modifier = Modifier.size(28.dp)
            )
        }
        Text(
            text = "Aleatório",
            style = MaterialTheme.typography.labelLarge,
            color = if (shuffleEnabled) primary else onBackground.copy(alpha = 0.4f)
        )
    }
}

private fun formatDuration(ms: Long): String {
    if (ms <= 0) return "0:00"
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
