package org.cuak.sshapp.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.ColumnAdapter
import org.cuak.sshapp.ServerDatabase
import org.cuak.sshapp.ServerEntity

// 1. Definimos la Interfaz (Contrato)
interface DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}

// 2. Función helper para crear la base de datos usando la fábrica
fun createDatabase(factory: DatabaseDriverFactory): ServerDatabase {
    val driver = factory.createDriver()

    // Adaptador para convertir Long (SQL) a Int (Kotlin)
    val portAdapter = object : ColumnAdapter<Int, Long> {
        override fun decode(databaseValue: Long): Int = databaseValue.toInt()
        override fun encode(value: Int): Long = value.toLong()
    }

    return ServerDatabase(
        driver = driver,
        ServerEntityAdapter = ServerEntity.Adapter(
            portAdapter = portAdapter
        )
    )
}