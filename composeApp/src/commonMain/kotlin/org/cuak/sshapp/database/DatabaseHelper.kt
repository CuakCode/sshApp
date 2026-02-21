package org.cuak.sshapp.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.ColumnAdapter
import org.cuak.sshapp.CameraEntity
import org.cuak.sshapp.ServerDatabase
import org.cuak.sshapp.ServerEntity

interface DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}

fun createDatabase(factory: DatabaseDriverFactory): ServerDatabase {
    val driver = factory.createDriver()

    // Adaptador reutilizable para convertir Long (SQL) a Int (Kotlin)
    // Sirve tanto para el puerto SSH como para el puerto de la Cámara
    val intToLongAdapter = object : ColumnAdapter<Int, Long> {
        override fun decode(databaseValue: Long): Int = databaseValue.toInt()
        override fun encode(value: Int): Long = value.toLong()
    }

    return ServerDatabase(
        driver = driver,
        // Adaptador para la tabla principal
        ServerEntityAdapter = ServerEntity.Adapter(
            portAdapter = intToLongAdapter
        ),
        // Adaptador para la tabla débil (Cámara)
        CameraEntityAdapter = CameraEntity.Adapter(
            camera_portAdapter = intToLongAdapter
        )
    )
}