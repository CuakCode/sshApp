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


data class HomeUiState(
    val devices: List<Device> = emptyList(),
    val isLoading: Boolean = true
)

class HomeViewModel(
    private val repository: ServerRepository,
    private val connectivity: ConnectivityManager,
    private val settingsRepository: SettingsRepository
) : ScreenModel {

    
    var selectedDevice by mutableStateOf<Device?>(null)
        private set

    
    private val _pingStatus = MutableStateFlow<Map<Long, Boolean>>(emptyMap())

    
    val uiState: StateFlow<HomeUiState> = combine(
        repository.getAllServers(), 
        _pingStatus
    ) { devices, pingMap ->
        val mappedDevices = devices.map { device ->
            val isOnline = pingMap[device.id]
            val status = when (isOnline) {
                true -> ServerStatus.ONLINE
                false -> ServerStatus.OFFLINE
                null -> ServerStatus.UNKNOWN
            }

            
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
                    
                    val currentTimeout = settingsRepository.settings.value.pingTimeoutMs

                    val results = currentDevices.map { device ->
                        async {
                            val isReachable = try {
                                
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

    
    fun showDeviceOptions(device: Device) { selectedDevice = device }
    fun dismissOptions() { selectedDevice = null }

    

    fun addDevice(device: Device) {
        screenModelScope.launch {
            
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