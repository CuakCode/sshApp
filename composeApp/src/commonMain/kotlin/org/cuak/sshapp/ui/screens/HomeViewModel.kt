package org.cuak.sshapp.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.cuak.sshapp.models.Server
import org.cuak.sshapp.repository.ServerRepository

class HomeViewModel(private val repository: ServerRepository) : ViewModel() {

    val servers: StateFlow<List<Server>> = repository.getAllServers()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addServer(name: String, ip: String, user: String, iconName: String) {
        viewModelScope.launch {
            repository.addServer(
                Server(name = name, ip = ip, username = user, iconName = iconName)
            )
        }
    }

    fun showServerOptions(server: Server) {
        println("Pulsación larga en: ${server.name}")
    }
}
/*
class HomeViewModel : ViewModel() {
    // Lista de prueba con diferentes estados
    private val _servers = MutableStateFlow(
        listOf(
            Server(id = 1, name = "Producción Web", ip = "192.168.1.10", status = ServerStatus.ONLINE, username = "admin", iconName = "cloud"),
            Server(id = 2, name = "Base de Datos", ip = "192.168.1.11", status = ServerStatus.OFFLINE, username = "db_user"),
            Server(id = 3, name = "Backup Storage", ip = "10.0.0.5", status = ServerStatus.ONLINE, username = "backup", iconName = "storage"),
            Server(id = 4, name = "Servidor Desarrollo", ip = "172.16.0.100", status = ServerStatus.UNKNOWN, username = "dev")
        )
    )
    val servers: StateFlow<List<Server>> = _servers

    fun showServerOptions(server: Server) {
        println("Pulsación larga en: ${server.name}")
    }
}*/