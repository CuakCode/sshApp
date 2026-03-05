package org.cuak.sshapp

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.cuak.sshapp.database.DatabaseDriverFactory
import org.cuak.sshapp.database.JvmDatabaseDriverFactory
import org.cuak.sshapp.di.initKoin
import org.cuak.sshapp.domain.files.JvmLocalFileSystem
import org.cuak.sshapp.domain.files.LocalFileSystem
import org.cuak.sshapp.domain.ssh.JvmSshClient
import org.cuak.sshapp.domain.ssh.SshClient
import org.cuak.sshapp.repository.SettingsRepository
import org.koin.dsl.module
import java.util.Locale

fun main() {
    val koinApp = initKoin(
        platformModules = listOf(module {
            single<DatabaseDriverFactory> { JvmDatabaseDriverFactory() }
            single<SshClient> { JvmSshClient() }
            single<LocalFileSystem> { JvmLocalFileSystem() }
        })
    )

    val settingsRepo = koinApp.koin.get<SettingsRepository>()
    val currentLang = settingsRepo.settings.value.language

    val localeToApply = when (currentLang) {
        "es" -> Locale.Builder().setLanguage("es").setRegion("ES").build()
        "en" -> Locale.Builder().setLanguage("en").setRegion("US").build()
        else -> {
            val sysLang = Locale.getDefault().language
            if (sysLang == "es") {
                Locale.Builder().setLanguage("es").setRegion("ES").build()
            } else {
                Locale.Builder().setLanguage("en").setRegion("US").build()
            }
        }
    }
    Locale.setDefault(localeToApply)

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "sshApp",
        ) {
            App()
        }
    }
}