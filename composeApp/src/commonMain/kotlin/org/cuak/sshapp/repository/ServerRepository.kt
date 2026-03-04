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
import org.cuak.sshapp.domain.security.EncryptionService 

class ServerRepository(
    private val database: ServerDatabase,
    private val encryptionService: EncryptionService 
) {
    private val queries = database.serverDatabaseQueries

    
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
            
            val securePassword = encryptionService.encrypt(device.password)

            
            queries.insertServer(
                name = device.name,
                ip = device.ip,
                port = device.port,
                username = device.username,
                password = securePassword, 
                sshKeyPath = device.sshKeyPath,
                iconName = device.iconName
            )

            
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
            
            val securePassword = encryptionService.encrypt(device.password)

            
            queries.updateServer(
                name = device.name,
                ip = device.ip,
                port = device.port,
                username = device.username,
                password = securePassword, 
                sshKeyPath = device.sshKeyPath,
                iconName = device.iconName,
                id = device.id
            )

            
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
        
        queries.deleteServer(id)
    }

    suspend fun getServerById(id: Long): Device? {
        return queries.selectDeviceById(id).executeAsOneOrNull()?.toDomain()
    }

    
    
    

    private fun org.cuak.sshapp.SelectAllDevices.toDomain(): Device {
        
        val plainTextPassword = encryptionService.decrypt(password)

        return if (camera_protocol != null) {
            Camera(
                id = id,
                name = name,
                ip = ip,
                port = port,
                username = username,
                password = plainTextPassword, 
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
                password = plainTextPassword, 
                sshKeyPath = sshKeyPath,
                iconName = iconName,
                status = ServerStatus.UNKNOWN
            )
        }
    }

    private fun org.cuak.sshapp.SelectDeviceById.toDomain(): Device {
        
        val plainTextPassword = encryptionService.decrypt(password)

        return if (camera_protocol != null) {
            Camera(
                id = id,
                name = name,
                ip = ip,
                port = port,
                username = username,
                password = plainTextPassword, 
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
                password = plainTextPassword, 
                sshKeyPath = sshKeyPath,
                iconName = iconName,
                status = ServerStatus.UNKNOWN
            )
        }
    }

    fun saveMetricsAndCleanOld(serverId: Long, metrics: ServerMetrics, retentionDays: Int) {

        
        val now = getCurrentTimeMillis()

        
        val cutoffTimestamp = now - (retentionDays * 86_400_000L)

        database.transaction {
            
            database.serverDatabaseQueries.insertMetric(
                serverId = serverId,
                cpuUsage = metrics.cpuPercentage,
                ramUsage = metrics.ramPercentage,
                diskUsage = metrics.diskUsage.firstOrNull() ?: 0.0,
                temperature = metrics.temperatures["CPU"] ?: 0.0,
                timestamp = now
            )

            
            database.serverDatabaseQueries.deleteOldMetrics(cutoffTimestamp)
        }
    }
}