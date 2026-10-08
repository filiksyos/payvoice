package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AppPreferences(
    val isServiceEnabled: Boolean = true,
    val repeatCount: Int = 1,
    val speechRate: Float = 1.0f,
    val speechPitch: Float = 1.0f,
    val maxVolumeOverride: Boolean = true,
    val keepScreenOn: Boolean = true,
    val cbeEnabled: Boolean = true,
    val telebirrEnabled: Boolean = true,
    val customTemplate: String = "{service} payment! Received {amount} Birr from {sender}."
)

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("payvoice_settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<AppPreferences> = _settings.asStateFlow()

    private fun loadSettings(): AppPreferences {
        return AppPreferences(
            isServiceEnabled = prefs.getBoolean("service_enabled", true),
            repeatCount = prefs.getInt("repeat_count", 1),
            speechRate = prefs.getFloat("speech_rate", 1.0f),
            speechPitch = prefs.getFloat("speech_pitch", 1.0f),
            maxVolumeOverride = prefs.getBoolean("max_volume", true),
            keepScreenOn = prefs.getBoolean("keep_screen_on", true),
            cbeEnabled = prefs.getBoolean("cbe_enabled", true),
            telebirrEnabled = prefs.getBoolean("telebirr_enabled", true),
            customTemplate = prefs.getString(
                "custom_template",
                "{service} payment! Received {amount} Birr from {sender}."
            ) ?: "{service} payment! Received {amount} Birr from {sender}."
        )
    }

    fun updateServiceEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("service_enabled", enabled).apply()
        _settings.value = _settings.value.copy(isServiceEnabled = enabled)
    }

    fun updateRepeatCount(count: Int) {
        prefs.edit().putInt("repeat_count", count.coerceIn(1, 3)).apply()
        _settings.value = _settings.value.copy(repeatCount = count.coerceIn(1, 3))
    }

    fun updateSpeechRate(rate: Float) {
        prefs.edit().putFloat("speech_rate", rate).apply()
        _settings.value = _settings.value.copy(speechRate = rate)
    }

    fun updateSpeechPitch(pitch: Float) {
        prefs.edit().putFloat("speech_pitch", pitch).apply()
        _settings.value = _settings.value.copy(speechPitch = pitch)
    }

    fun updateMaxVolumeOverride(override: Boolean) {
        prefs.edit().putBoolean("max_volume", override).apply()
        _settings.value = _settings.value.copy(maxVolumeOverride = override)
    }

    fun updateKeepScreenOn(keep: Boolean) {
        prefs.edit().putBoolean("keep_screen_on", keep).apply()
        _settings.value = _settings.value.copy(keepScreenOn = keep)
    }

    fun updateCbeEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("cbe_enabled", enabled).apply()
        _settings.value = _settings.value.copy(cbeEnabled = enabled)
    }

    fun updateTelebirrEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("telebirr_enabled", enabled).apply()
        _settings.value = _settings.value.copy(telebirrEnabled = enabled)
    }

    fun updateCustomTemplate(template: String) {
        prefs.edit().putString("custom_template", template).apply()
        _settings.value = _settings.value.copy(customTemplate = template)
    }
}
