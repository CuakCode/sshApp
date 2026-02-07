package org.cuak.sshapp.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.ColumnAdapter
import org.cuak.sshapp.ServerDatabase
import org.cuak.sshapp.ServerEntity
import org.cuak.sshapp.models.DeviceType
import org.cuak.sshapp.repository.ServerRepository

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

    // Adaptador para convertir String (SQL) a Enum (Kotlin)
    val deviceTypeAdapter = object : ColumnAdapter<DeviceType, String> {
        override fun decode(databaseValue: String): DeviceType {
            return try {
                DeviceType.valueOf(databaseValue)
            } catch (e: Exception) {
                DeviceType.SERVER
            }
        }

        override fun encode(value: DeviceType): String {
            return value.name
        }
    }

    return ServerDatabase(
        driver = driver,
        ServerEntityAdapter = ServerEntity.Adapter(
            portAdapter = intToLongAdapter,
            typeAdapter = deviceTypeAdapter,
            camera_portAdapter = intToLongAdapter
        )
    )
}