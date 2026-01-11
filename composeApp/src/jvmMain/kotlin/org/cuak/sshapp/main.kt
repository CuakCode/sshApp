package org.cuak.sshapp

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.cuak.sshapp.database.DatabaseDriverFactory
import org.cuak.sshapp.di.initKoin
import org.koin.dsl.module

fun main() {
    initKoin(
        platformModules = listOf(module { single { DatabaseDriverFactory() } })
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