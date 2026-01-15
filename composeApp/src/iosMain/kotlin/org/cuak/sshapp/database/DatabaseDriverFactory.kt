package org.cuak.sshapp.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver // <--- Añade este import
import org.cuak.sshapp.ServerDatabase

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        // En SQLDelight 2.x, el driver nativo se encuentra en este paquete
        return NativeSqliteDriver(ServerDatabase.Schema, "server.db")
    }
}