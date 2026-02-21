package org.cuak.sshapp.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.cuak.sshapp.ServerDatabase
import org.cuak.sshapp.models.Device
import org.cuak.sshapp.models.Server
import org.cuak.sshapp.models.Camera
import org.cuak.sshapp.models.ServerMetrics
import org.cuak.sshapp.models.ServerStatus
import org.cuak.sshapp.utils.getCurrentTimeMillis

class ServerRepository(private val database: ServerDatabase) {
    private val queries = database.serverDatabaseQueries

    // Obtenemos un Flow de 'Device' (que por debajo serán instancias de Server o Camera)
    fun getAllServers(): Flow<List<Device>> {
        return queries.selectAllDevices()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { entities ->
                entities.map { it.toDomain() }
            }
    }

    fun addServer(device: Device) {
        database.transaction {
            // 1. Siempre insertamos los datos base en ServerEntity
            queries.insertServer(
                name = device.name,
                ip = device.ip,
                port = device.port,
                username = device.username,
                password = device.password,
                sshKeyPath = device.sshKeyPath,
                iconName = device.iconName
            )

            // 2. Si el dispositivo es una Cámara, insertamos sus datos específicos
            if (device is Camera) {
                val newId = queries.lastInsertRowId().executeAsOne()
                queries.insertCamera(
                    serverId = newId,
                    camera_protocol = device.cameraProtocol,
                    camera_port = device.cameraPort,
                    camera_stream = device.cameraStream
                )
            }
        }
    }

    fun updateServer(device: Device) {
        database.transaction {
            // 1. Actualizamos los datos base en ServerEntity
            queries.updateServer(
                name = device.name,
                ip = device.ip,
                port = device.port,
                username = device.username,
                password = device.password,
                sshKeyPath = device.sshKeyPath,
                iconName = device.iconName,
                id = device.id
            )

            // 2. Si es una cámara, actualizamos CameraEntity
            if (device is Camera) {
                queries.updateCamera(
                    camera_protocol = device.cameraProtocol,
                    camera_port = device.cameraPort,
                    camera_stream = device.cameraStream,
                    serverId = device.id
                )
            }
        }
    }

    fun deleteServer(id: Long) {
        // Al eliminar el Servidor, el ON DELETE CASCADE elimina la Cámara asociada si existe
        queries.deleteServer(id)
    }

    suspend fun getServerById(id: Long): Device? {
        return queries.selectDeviceById(id).executeAsOneOrNull()?.toDomain()
    }

    // ==========================================
    // MAPPERS: Transforman de SQL (LEFT JOIN) a Kotlin
    // ==========================================

    private fun org.cuak.sshapp.SelectAllDevices.toDomain(): Device {
        return if (camera_protocol != null) {
            Camera(
                id = id,
                name = name,
                ip = ip,
                port = port,
                username = username,
                password = password,
                sshKeyPath = sshKeyPath,
                iconName = iconName,
                status = ServerStatus.UNKNOWN,
                cameraProtocol = camera_protocol,
                cameraPort = camera_port ?: 8554, // Proporcionamos fallback seguro
                cameraStream = camera_stream ?: "ch0_0.h264"
            )
        } else {
            Server(
                id = id,
                name = name,
                ip = ip,
                port = port,
                username = username,
                password = password,
                sshKeyPath = sshKeyPath,
                iconName = iconName,
                status = ServerStatus.UNKNOWN
            )
        }
    }

    private fun org.cuak.sshapp.SelectDeviceById.toDomain(): Device {
        return if (camera_protocol != null) {
            Camera(
                id = id,
                name = name,
                ip = ip,
                port = port,
                username = username,
                password = password,
                sshKeyPath = sshKeyPath,
                iconName = iconName,
                status = ServerStatus.UNKNOWN,
                cameraProtocol = camera_protocol,
                cameraPort = camera_port ?: 8554,
                cameraStream = camera_stream ?: "ch0_0.h264"
            )
        } else {
            Server(
                id = id,
                name = name,
                ip = ip,
                port = port,
                username = username,
                password = password,
                sshKeyPath = sshKeyPath,
                iconName = iconName,
                status = ServerStatus.UNKNOWN
            )
        }
    }

    fun saveMetricsAndCleanOld(serverId: Long, metrics: ServerMetrics, retentionDays: Int) {

        // 1. Llamamos a nuestra función multiplataforma nativa
        val now = getCurrentTimeMillis()

        // Calculamos la fecha límite (1 día = 86_400_000 ms)
        val cutoffTimestamp = now - (retentionDays * 86_400_000L)

        database.transaction {
            // 2. Insertamos la métrica
            database.serverDatabaseQueries.insertMetric(
                serverId = serverId,
                cpuUsage = metrics.cpuPercentage,
                ramUsage = metrics.ramPercentage,
                diskUsage = metrics.diskUsage.firstOrNull() ?: 0.0,
                temperature = metrics.temperatures["CPU"] ?: 0.0,
                timestamp = now
            )

            // 3. Limpiamos historial viejo
            database.serverDatabaseQueries.deleteOldMetrics(cutoffTimestamp)
        }
    }
}