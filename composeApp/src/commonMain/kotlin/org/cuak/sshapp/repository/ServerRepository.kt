package org.cuak.sshapp.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.cuak.sshapp.ServerDatabase
import org.cuak.sshapp.models.Server
import org.cuak.sshapp.models.ServerStatus

class ServerRepository(database: ServerDatabase) {
    private val queries = database.serverDatabaseQueries

    // Obtener todos los servidores como un Flow constante
    fun getAllServers(): Flow<List<Server>> {
        return queries.selectAllServers()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { entities ->
                entities.map { it.toDomain() }
            }
    }

    fun addServer(server: Server) {
        queries.insertServer(
            name = server.name,
            ip = server.ip,
            port = server.port,
            username = server.username,
            password = server.password,
            sshKeyPath = server.sshKeyPath,
            iconName = server.iconName
        )
    }

    // ServerRepository.kt
    // Modifica esta función en ServerRepository.kt para persistir cambios correctamente
    fun updateServer(server: Server) {
        queries.updateServer(
            name = server.name,
            ip = server.ip,
            port = server.port,
            username = server.username,
            password = server.password,
            sshKeyPath = server.sshKeyPath,
            iconName = server.iconName,
            id = server.id
        )
    }

    fun deleteServer(id: Long) {
        queries.deleteServer(id)
    }

    // Mapper de DB a Dominio
    private fun org.cuak.sshapp.ServerEntity.toDomain() = Server(
        id = id,
        name = name,
        ip = ip,
        port = port,
        username = username,
        iconName = iconName,
        status = ServerStatus.UNKNOWN // El estado se calcula dinámicamente
    )
}