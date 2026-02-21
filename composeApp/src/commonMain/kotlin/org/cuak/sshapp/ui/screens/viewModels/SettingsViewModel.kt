package org.cuak.sshapp.ui.screens.viewModels

import cafe.adriel.voyager.core.model.ScreenModel
import kotlinx.coroutines.flow.StateFlow
import org.cuak.sshapp.repository.AppSettings
import org.cuak.sshapp.repository.SettingsRepository

class SettingsViewModel(
    private val settingsRepository: SettingsRepository
) : ScreenModel {

    val settings: StateFlow<AppSettings> = settingsRepository.settings

    fun updateRetentionDays(days: Int) {
        val current = settings.value
        settingsRepository.updateSettings(current.copy(metricsRetentionDays = days))
    }

    fun updatePingTimeout(timeout: Int) {
        val current = settings.value
        settingsRepository.updateSettings(current.copy(pingTimeoutMs = timeout))
    }

    fun toggleTheme(isDark: Boolean) {
        val current = settings.value
        settingsRepository.updateSettings(current.copy(isDarkTheme = isDark))
    }

    fun updateDatabasePath(path: String) {
        val current = settings.value
        settingsRepository.updateSettings(current.copy(databasePath = path))
    }
}