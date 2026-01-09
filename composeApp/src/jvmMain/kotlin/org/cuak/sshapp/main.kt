package org.cuak.sshapp

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.cuak.sshapp.database.DatabaseDriverFactory
import org.cuak.sshapp.database.createDatabase

fun main() = application {
    val driverFactory = DatabaseDriverFactory()
    val database = createDatabase(driverFactory)

    Window(
        onCloseRequest = ::exitApplication,
        title = "sshApp",
    ) {
        App(database = database)
    }
}