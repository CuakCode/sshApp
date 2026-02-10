package org.cuak.sshapp.ui.screens.viewModels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.cuak.sshapp.models.Server
import org.cuak.sshapp.models.ServerStatus
import org.cuak.sshapp.network.ConnectivityManager
import org.cuak.sshapp.repository.ServerRepository

// UI State para manejar la carga y la lista combinada
data class HomeUiState(
    val servers: List<Server> = emptyList(),
    val isLoading: Boolean = true
)

class HomeViewModel(
    private val repository: ServerRepository,
    private val connectivity: ConnectivityManager // Inyectado por constructor
) : ScreenModel {

    // 1. Estado para el servidor seleccionado (BottomSheet)
    var selectedServer by mutableStateOf<Server?>(null)
        private set

    // 2. Estado interno de los pings (Map: ID -> Online?)
    private val _pingStatus = MutableStateFlow<Map<Long, Boolean>>(emptyMap())

    // 3. Flow combinado: Base de datos + Estado de Red
    // Mapeamos el resultado del ping al enum ServerStatus para que la UI (ServerCard) lo entienda
    val uiState: StateFlow<HomeUiState> = combine(
        repository.getAllServers(),
        _pingStatus
    ) { servers, pingMap ->
        val mappedServers = servers.map { server ->
            // Si tenemos un estado de ping, lo usamos. Si no, mantenemos UNKNOWN o OFFLINE según prefieras.
            val isOnline = pingMap[server.id]
            val status = when (isOnline) {
                true -> ServerStatus.ONLINE
                false -> ServerStatus.OFFLINE
                null -> ServerStatus.UNKNOWN
            }
            server.copy(status = status)
        }
        HomeUiState(servers = mappedServers, isLoading = false)
    }.stateIn(
        scope = screenModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState(isLoading = true)
    )

    init {
        startMonitoringLoop()
    }

    // --- Lógica de Monitoreo Optimizada (Step 5) ---
    private fun startMonitoringLoop() {
        screenModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                // Obtenemos snapshot de servidores sin observar
                val currentServers = repository.getAllServers().first()

                if (currentServers.isNotEmpty()) {
                    // Ping en paralelo a todos
                    val results = currentServers.map { server ->
                        async {
                            val isReachable = try {
                                connectivity.isReachable(server.ip)
                            } catch (e: Exception) {
                                false
                            }
                            server.id to isReachable
                        }
                    }.awaitAll().toMap()

                    // Actualizamos el mapa de estados
                    _pingStatus.value = results
                }

                // Esperamos 10s
                delay(10000)
            }
        }
    }

    // --- Acciones de UI (BottomSheet) ---
    fun showServerOptions(server: Server) { selectedServer = server }
    fun dismissOptions() { selectedServer = null }

    // --- Operaciones CRUD ---

    fun addServer(server: Server) {
        screenModelScope.launch {
            // Aseguramos que el ID sea 0 para que la BD genere uno nuevo
            repository.addServer(server.copy(id = 0))
        }
    }

    // Actualiza usando el objeto completo
    fun updateServer(server: Server) {
        screenModelScope.launch {
            repository.updateServer(server)
            dismissOptions() // Cierra el menú inferior si estaba abierto
        }
    }

    fun deleteServer(id: Long) {
        screenModelScope.launch {
            repository.deleteServer(id)
            dismissOptions()
        }
    }
}