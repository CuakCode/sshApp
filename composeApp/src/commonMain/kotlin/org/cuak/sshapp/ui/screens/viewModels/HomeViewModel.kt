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
import org.cuak.sshapp.models.Device
import org.cuak.sshapp.models.Server
import org.cuak.sshapp.models.Camera
import org.cuak.sshapp.models.ServerStatus
import org.cuak.sshapp.network.ConnectivityManager
import org.cuak.sshapp.repository.ServerRepository
import org.cuak.sshapp.repository.SettingsRepository

// UI State adaptado a 'Device'
data class HomeUiState(
    val devices: List<Device> = emptyList(),
    val isLoading: Boolean = true
)

class HomeViewModel(
    private val repository: ServerRepository,
    private val connectivity: ConnectivityManager,
    private val settingsRepository: SettingsRepository
) : ScreenModel {

    // 1. Estado para el dispositivo seleccionado (BottomSheet)
    var selectedDevice by mutableStateOf<Device?>(null)
        private set

    // 2. Estado interno de los pings (Map: ID -> Online?)
    private val _pingStatus = MutableStateFlow<Map<Long, Boolean>>(emptyMap())

    // 3. Flow combinado: Base de datos + Estado de Red
    val uiState: StateFlow<HomeUiState> = combine(
        repository.getAllServers(), // Sigue llamándose getAllServers en tu Repo, pero devuelve List<Device>
        _pingStatus
    ) { devices, pingMap ->
        val mappedDevices = devices.map { device ->
            val isOnline = pingMap[device.id]
            val status = when (isOnline) {
                true -> ServerStatus.ONLINE
                false -> ServerStatus.OFFLINE
                null -> ServerStatus.UNKNOWN
            }

            // MAGIA KOTLIN: Como Device es interfaz, hacemos cast seguro para usar su copy()
            when (device) {
                is Server -> device.copy(status = status)
                is Camera -> device.copy(status = status)
            }
        }
        HomeUiState(devices = mappedDevices, isLoading = false)
    }.stateIn(
        scope = screenModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState(isLoading = true)
    )

    init {
        startMonitoringLoop()
    }

    private fun startMonitoringLoop() {
        screenModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                val currentDevices = repository.getAllServers().first()

                if (currentDevices.isNotEmpty()) {
                    // 2. Leemos el timeout configurado en los ajustes (fuera del async para eficiencia)
                    val currentTimeout = settingsRepository.settings.value.pingTimeoutMs

                    val results = currentDevices.map { device ->
                        async {
                            val isReachable = try {
                                // 3. Le pasamos el timeout personalizado a tu función
                                connectivity.isReachable(device.ip, currentTimeout)
                            } catch (e: Exception) {
                                false
                            }
                            device.id to isReachable
                        }
                    }.awaitAll().toMap()

                    _pingStatus.value = results
                }
                delay(10000)
            }
        }
    }

    // --- Acciones de UI (BottomSheet) ---
    fun showDeviceOptions(device: Device) { selectedDevice = device }
    fun dismissOptions() { selectedDevice = null }

    // --- Operaciones CRUD ---

    fun addDevice(device: Device) {
        screenModelScope.launch {
            // Ponemos ID 0 dependiendo del tipo concreto
            val newDevice = when (device) {
                is Server -> device.copy(id = 0)
                is Camera -> device.copy(id = 0)
            }
            repository.addServer(newDevice)
        }
    }

    fun updateDevice(device: Device) {
        screenModelScope.launch {
            repository.updateServer(device)
            dismissOptions()
        }
    }

    fun deleteDevice(id: Long) {
        screenModelScope.launch {
            repository.deleteServer(id)
            dismissOptions()
        }
    }
}