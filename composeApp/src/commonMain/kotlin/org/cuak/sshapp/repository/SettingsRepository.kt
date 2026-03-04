package org.cuak.sshapp.repository

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AppSettings(
    val metricsRetentionDays: Int = 7,
    val pingTimeoutMs: Int = 2000,
    val isDarkTheme: Boolean = true,
    val language: String = "Español",
    val databasePath: String = "default"
)

class SettingsRepository {
    
    private val preferences = Settings()

    
    private val _settingsState = MutableStateFlow(
        AppSettings(
            metricsRetentionDays = preferences.getInt("metricsRetentionDays", 7),
            pingTimeoutMs = preferences.getInt("pingTimeoutMs", 2000),
            isDarkTheme = preferences.getBoolean("isDarkTheme", true),
            language = preferences.getString("language", "Español"),
            databasePath = preferences.getString("databasePath", "default")
        )
    )

    
    val settings: StateFlow<AppSettings> = _settingsState.asStateFlow()

    fun updateSettings(newSettings: AppSettings) {
        
        preferences.putInt("metricsRetentionDays", newSettings.metricsRetentionDays)
        preferences.putInt("pingTimeoutMs", newSettings.pingTimeoutMs)
        preferences.putBoolean("isDarkTheme", newSettings.isDarkTheme)
        preferences.putString("language", newSettings.language)
        preferences.putString("databasePath", newSettings.databasePath)

        _settingsState.value = newSettings
    }
}