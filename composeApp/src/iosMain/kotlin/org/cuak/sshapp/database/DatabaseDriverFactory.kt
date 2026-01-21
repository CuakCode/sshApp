package org.cuak.sshapp.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import org.cuak.sshapp.ServerDatabase

// Implementación específica para iOS
class IosDatabaseDriverFactory : DatabaseDriverFactory {
    override fun createDriver(): SqlDriver {
        return NativeSqliteDriver(ServerDatabase.Schema, "server.db")
    }
}