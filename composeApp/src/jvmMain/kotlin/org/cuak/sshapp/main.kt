package org.cuak.sshapp

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.cuak.sshapp.database.DatabaseDriverFactory
import org.cuak.sshapp.database.JvmDatabaseDriverFactory
import org.cuak.sshapp.di.initKoin
import org.cuak.sshapp.domain.files.JvmLocalFileSystem
import org.cuak.sshapp.domain.files.LocalFileSystem
import org.cuak.sshapp.domain.ssh.JvmSshClient // Asegúrate de importar la implementación
import org.cuak.sshapp.domain.ssh.SshClient    // y la interfaz
import org.koin.dsl.module

fun main() {
    initKoin(
        // Inyectamos las dependencias específicas de Desktop (JVM)
        platformModules = listOf(module {
            single<DatabaseDriverFactory> { JvmDatabaseDriverFactory() }
            // Registramos el cliente SSH para Desktop
            single<SshClient> { JvmSshClient() }
            single<LocalFileSystem> { JvmLocalFileSystem() }
        })
    )

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "sshApp",
        ) {
            App()
        }
    }
}