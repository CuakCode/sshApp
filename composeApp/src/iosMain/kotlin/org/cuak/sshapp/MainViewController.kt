package org.cuak.sshapp

import androidx.compose.ui.window.ComposeUIViewController
import org.cuak.sshapp.database.DatabaseDriverFactory
import org.cuak.sshapp.di.initKoin
import org.koin.dsl.module

import org.cuak.sshapp.database.IosDatabaseDriverFactory

fun MainViewController() = ComposeUIViewController(
    configure = {
        initKoin(
            platformModules = listOf(module {
                // Vinculamos la interfaz con la implementación de iOS
                single<DatabaseDriverFactory> { IosDatabaseDriverFactory() }
            })
        )
    }
) { App() }