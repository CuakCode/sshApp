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

// 2. Función helper para crear la base de datos usando la fábrica
fun createDatabase(factory: DatabaseDriverFactory): ServerDatabase {
    val driver = factory.createDriver()

    // Adaptador para convertir Long (SQL) a Int (Kotlin)
    val portAdapter = object : ColumnAdapter<Int, Long> {
        override fun decode(databaseValue: Long): Int = databaseValue.toInt()
        override fun encode(value: Int): Long = value.toLong()
    }

    // Adaptador para convertir String (SQL) a Enum (Kotlin)
    val deviceTypeAdapter = object : ColumnAdapter<DeviceType, String> {
        override fun decode(databaseValue: String): DeviceType {
            return try {
                DeviceType.valueOf(databaseValue)
            } catch (e: Exception) {
                DeviceType.SERVER // Valor por defecto
            }
        }

        override fun encode(value: DeviceType): String {
            return value.name
        }
    }

    return ServerDatabase(
        driver = driver,
        ServerEntityAdapter = ServerEntity.Adapter(
            portAdapter = portAdapter,
            typeAdapter = deviceTypeAdapter // <--- ¡AQUÍ FALTABA ESTA LÍNEA!
        )
    )
}