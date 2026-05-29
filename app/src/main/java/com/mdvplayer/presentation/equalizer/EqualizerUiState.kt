package com.mdvplayer.presentation.equalizer

data class EqualizerBand(
    val index: Int,
    val centerFrequencyHz: Int,
    val levelMillibels: Int,
    val minLevelMillibels: Int,
    val maxLevelMillibels: Int
) {
    fun frequencyLabel(): String {
        return if (centerFrequencyHz >= 1000) {
            "${centerFrequencyHz / 1000}kHz"
        } else {
            "${centerFrequencyHz}Hz"
        }
    }

    fun levelDb(): Float = levelMillibels / 100f
}

data class EqualizerUiState(
    val isEnabled: Boolean = false,
    val bands: List<EqualizerBand> = emptyList(),
    val presets: List<String> = emptyList(),
    val currentPresetIndex: Int = -1,
    val isAvailable: Boolean = false,
    val isLoading: Boolean = true
)
