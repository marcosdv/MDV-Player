package com.mdvplayer.presentation.equalizer

import android.media.audiofx.Equalizer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mdvplayer.data.datastore.PreferencesDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EqualizerViewModel @Inject constructor(
    private val preferencesDataStore: PreferencesDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(EqualizerUiState())
    val uiState: StateFlow<EqualizerUiState> = _uiState.asStateFlow()

    private var equalizer: Equalizer? = null

    init {
        viewModelScope.launch {
            preferencesDataStore.audioSessionId
                .distinctUntilChanged()
                .collect { sessionId ->
                    if (sessionId != 0) {
                        setupEqualizer(sessionId)
                    }
                }
        }
    }

    private fun setupEqualizer(audioSessionId: Int) {
        try {
            equalizer?.release()
            val eq = Equalizer(0, audioSessionId)
            eq.enabled = true
            equalizer = eq

            val numBands = eq.numberOfBands.toInt()
            val levelRange = eq.bandLevelRange // [minLevel, maxLevel] in millibels

            val bands = (0 until numBands).map { i ->
                val band = i.toShort()
                EqualizerBand(
                    index = i,
                    centerFrequencyHz = eq.getCenterFreq(band) / 1000, // milliHz → Hz
                    levelMillibels = eq.getBandLevel(band).toInt(),
                    minLevelMillibels = levelRange[0].toInt(),
                    maxLevelMillibels = levelRange[1].toInt()
                )
            }

            val numPresets = eq.numberOfPresets.toInt()
            val presets = (0 until numPresets).map { i -> eq.getPresetName(i.toShort()) }

            _uiState.update {
                it.copy(
                    isEnabled = true,
                    bands = bands,
                    presets = presets,
                    isAvailable = true,
                    isLoading = false
                )
            }
        } catch (_: Exception) {
            _uiState.update { it.copy(isAvailable = false, isLoading = false) }
        }
    }

    fun setEnabled(enabled: Boolean) {
        equalizer?.enabled = enabled
        _uiState.update { it.copy(isEnabled = enabled) }
    }

    fun setBandLevel(bandIndex: Int, levelMillibels: Int) {
        equalizer?.setBandLevel(bandIndex.toShort(), levelMillibels.toShort())
        _uiState.update { state ->
            state.copy(
                bands = state.bands.map { band ->
                    if (band.index == bandIndex) band.copy(levelMillibels = levelMillibels) else band
                },
                currentPresetIndex = -1 // custom
            )
        }
    }

    fun usePreset(presetIndex: Int) {
        try {
            equalizer?.usePreset(presetIndex.toShort())
            _uiState.update { state ->
                val eq = equalizer ?: return@update state
                val updatedBands = state.bands.map { band ->
                    band.copy(levelMillibels = eq.getBandLevel(band.index.toShort()).toInt())
                }
                state.copy(bands = updatedBands, currentPresetIndex = presetIndex)
            }
        } catch (_: Exception) {}
    }

    override fun onCleared() {
        equalizer?.release()
        equalizer = null
        super.onCleared()
    }
}
