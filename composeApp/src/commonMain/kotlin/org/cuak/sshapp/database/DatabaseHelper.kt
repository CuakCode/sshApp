package org.cuak.sshapp.database

import app.cash.sqldelight.db.SqlDriver
import org.cuak.sshapp.ServerDatabase
import app.cash.sqldelight.ColumnAdapter // Importante
import org.cuak.sshapp.ServerEntity    // Importante

expect class DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}

fun createDatabase(factory: DatabaseDriverFactory): ServerDatabase {
    val driver = factory.createDriver()

    // Definimos el adaptador para convertir de Long (DB) a Int (Kotlin)
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