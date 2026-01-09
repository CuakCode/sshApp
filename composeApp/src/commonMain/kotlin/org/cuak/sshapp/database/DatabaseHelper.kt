package org.cuak.sshapp.database

import app.cash.sqldelight.db.SqlDriver
import org.cuak.sshapp.ServerDatabase

expect class DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}

fun createDatabase(factory: DatabaseDriverFactory): ServerDatabase {
    val driver = factory.createDriver()
    return ServerDatabase(driver)
}