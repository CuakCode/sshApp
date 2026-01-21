package org.cuak.sshapp.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import org.cuak.sshapp.ServerDatabase
import java.io.File

// Implementación específica para Desktop
class JvmDatabaseDriverFactory : DatabaseDriverFactory {
    override fun createDriver(): SqlDriver {
        // Guardamos la DB en la carpeta de usuario
        val databasePath = File(System.getProperty("user.home"), "sshapp_server.db")
        val driver: SqlDriver = JdbcSqliteDriver("jdbc:sqlite:${databasePath.absolutePath}")

        // Creamos las tablas si no existen
        try {
            ServerDatabase.Schema.create(driver)
        } catch (e: Exception) {
            // La base de datos ya existe, ignoramos el error
        }
        return driver
    }
}