package org.cuak.sshapp.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.cuak.sshapp.models.Server
import org.cuak.sshapp.models.ServerStatus
import org.cuak.sshapp.network.ConnectivityManager
import org.cuak.sshapp.repository.ServerRepository

class HomeViewModel(private val repository: ServerRepository) : ScreenModel {

    // 1. Estado para el servidor seleccionado (BottomSheet)
    var selectedServer by mutableStateOf<Server?>(null)
        private set

    // 2. Estado volátil de los pings (No persiste en DB)
    private val _serverStatuses = MutableStateFlow<Map<Long, ServerStatus>>(emptyMap())

    // 3. Flow combinado: La fuente de verdad para la UI
    val servers: StateFlow<List<Server>> = repository.getAllServers()
        .combine(_serverStatuses) { dbServers, statuses ->
            dbServers.map { server ->
                server.copy(status = statuses[server.id] ?: ServerStatus.UNKNOWN)
            }
        }
        .stateIn(
            scope = screenModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        startMonitoring()
    }

    private fun startMonitoring() {
        screenModelScope.launch {
            val connectivity = ConnectivityManager()

            // Escucha cambios en la DB. Si la lista cambia, reinicia los pings.
            repository.getAllServers().collectLatest { dbServers ->
                dbServers.forEach { server ->
                    launch(Dispatchers.Default) {
                        while (currentCoroutineContext().isActive) {
                            // Ejecuta el ping basado en el Host (IP/Dominio)
                            val isOnline = connectivity.isReachable(server.ip)
                            val newStatus = if (isOnline) ServerStatus.ONLINE else ServerStatus.OFFLINE

                            _serverStatuses.update { it + (server.id to newStatus) }

                            // Espera 10 segundos para el siguiente pulso
                            delay(10000)
                        }
                    }
                }
            }
        }
    }

    // --- Acciones de UI ---
    fun showServerOptions(server: Server) { selectedServer = server }
    fun dismissOptions() { selectedServer = null }

    // --- Operaciones CRUD ---
    fun addServer(name: String, ip: String, port: Int, user: String, pass: String?, keyPath: String?, iconName: String) {
        screenModelScope.launch {
            repository.addServer(Server(name = name, ip = ip, port = port, username = user, password = pass, sshKeyPath = keyPath, iconName = iconName))
        }
    }

    fun updateServer(id: Long, name: String, ip: String, port: Int, user: String, pass: String?, key: String?, icon: String) {
        screenModelScope.launch {
            repository.updateServer(Server(id = id, name = name, ip = ip, port = port, username = user, password = pass, sshKeyPath = key, iconName = icon))
            dismissOptions()
        }
    }

    fun deleteServer(id: Long) {
        screenModelScope.launch {
            repository.deleteServer(id)
            dismissOptions()
        }
    }
}