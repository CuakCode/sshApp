package org.cuak.sshapp.database

import app.cash.sqldelight.db.SqlDriver

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        return NativeSqliteDriver(ServerDatabase.Schema, "server.db")
    }
}