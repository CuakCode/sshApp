package org.cuak.sshapp.ui.screens

// 1. Elimina el import de androidx.lifecycle.ViewModel
// 2. Añade los imports de Voyager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.cuak.sshapp.models.Server
import org.cuak.sshapp.repository.ServerRepository


class HomeViewModel(private val repository: ServerRepository) : ScreenModel {

    // Estado para el servidor seleccionado (para editar/eliminar)
    // Usamos 'by' para que selectedServer actúe directamente como Server? y no como MutableState
    var selectedServer by mutableStateOf<Server?>(null)
        private set

    val servers: StateFlow<List<Server>> = repository.getAllServers()
        .stateIn(
            scope = screenModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun showServerOptions(server: Server) {
        selectedServer = server
    }

    fun dismissOptions() {
        selectedServer = null
    }

    fun addServer(name: String, ip: String, port: Int, user: String, pass: String?, keyPath: String?, iconName: String) {
        screenModelScope.launch {
            repository.addServer(
                Server(name = name, ip = ip, port = port, username = user, password = pass, sshKeyPath = keyPath, iconName = iconName)
            )
        }
    }

    fun updateServer(id: Long, name: String, ip: String, port: Int, user: String, pass: String?, key: String?, icon: String) {
        screenModelScope.launch {
            repository.updateServer(
                Server(id = id, name = name, ip = ip, port = port, username = user, password = pass, sshKeyPath = key, iconName = icon)
            )
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