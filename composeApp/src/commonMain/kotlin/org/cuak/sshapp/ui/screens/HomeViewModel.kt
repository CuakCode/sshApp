package org.cuak.sshapp.ui.screens

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.cuak.sshapp.models.Server
import org.cuak.sshapp.models.ServerStatus
/*
class HomeViewModel : ViewModel() {
    private val _servers = MutableStateFlow<List<Server>>(emptyList())
    val servers: StateFlow<List<Server>> = _servers

    init {
        loadServers()
        startStatusPolling()
    }

    private fun loadServers() {
        // Aquí cargarías desde ServerDatabase.sq
    }

    private fun startStatusPolling() {
        viewModelScope.launch {
            // Proceso encargado de realizar ping periódico a las IPs
            // Si responde, status = ServerStatus.ONLINE (Verde en UI)
            // Si no responde, status = ServerStatus.OFFLINE (Rojo en UI)
        }
    }

    fun showServerOptions(server: Server) {
        // Lógica para eliminar o modificar el servidor tras pulsación larga
    }
}*/

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
}