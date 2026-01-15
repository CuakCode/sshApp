package org.cuak.sshapp.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import org.cuak.sshapp.ServerDatabase
import java.io.File

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        // Definimos una ruta de archivo (puedes usar una carpeta de datos de usuario)
        val databasePath = File(System.getProperty("user.home"), "sshapp_server.db")
        val driver: SqlDriver = JdbcSqliteDriver("jdbc:sqlite:${databasePath.absolutePath}")

        // Solo creamos el esquema si el archivo no existía o está vacío
        // SQLDelight lanzará un error si intentas crear tablas que ya existen
        try {
            ServerDatabase.Schema.create(driver)
        } catch (e: Exception) {
            // La base de datos ya existe
        }

        return driver
    }
}