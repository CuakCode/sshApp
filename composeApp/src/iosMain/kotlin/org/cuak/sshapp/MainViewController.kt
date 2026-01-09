package org.cuak.sshapp

import androidx.compose.ui.window.ComposeUIViewController
import org.cuak.sshapp.database.DatabaseDriverFactory
import org.cuak.sshapp.database.createDatabase

fun MainViewController() = ComposeUIViewController {
    val driverFactory = DatabaseDriverFactory()
    val database = createDatabase(driverFactory)

    App(database = database)
}