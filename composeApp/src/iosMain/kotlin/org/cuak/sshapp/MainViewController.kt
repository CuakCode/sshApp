package org.cuak.sshapp

import androidx.compose.ui.window.ComposeUIViewController
import org.cuak.sshapp.database.DatabaseDriverFactory
import org.cuak.sshapp.di.initKoin
import org.koin.dsl.module

fun MainViewController() = ComposeUIViewController(
    configure = {
        initKoin(
            platformModules = listOf(module { single { DatabaseDriverFactory() } })
        )
    }
) {
    App()
}