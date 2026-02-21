package org.cuak.sshapp.ui.screens.viewModels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.launch
import org.cuak.sshapp.domain.ssh.SshClient
import org.cuak.sshapp.domain.ssh.SshTerminalSession
import org.cuak.sshapp.models.ProcessInfo
import org.cuak.sshapp.models.ProcessSortOption
import org.cuak.sshapp.models.Device
import org.cuak.sshapp.models.ServerMetrics
import org.cuak.sshapp.repository.ServerRepository
import org.cuak.sshapp.repository.SettingsRepository

sealed class DetailUiState {
    object Idle : DetailUiState()
    object Loading : DetailUiState()
    data class Success(val metrics: ServerMetrics) : DetailUiState()
    data class Error(val message: String) : DetailUiState()
}

class ServerDetailViewModel(
    private val repository: ServerRepository,
    private val sshClient: SshClient,
    private val settingsRepository: SettingsRepository
) : ScreenModel {

    // Cambiado de 'server' a 'device'
    var device by mutableStateOf<Device?>(null)
        private set

    var uiState by mutableStateOf<DetailUiState>(DetailUiState.Idle)
        private set

    private var terminalSession: SshTerminalSession? = null

    var terminalOutput by mutableStateOf("")
        private set

    var processes by mutableStateOf<List<ProcessInfo>>(emptyList())
        private set

    var processSortOption by mutableStateOf(ProcessSortOption.CPU)
        private set

    var isProcessesLoading by mutableStateOf(false)
        private set

    // --- Métricas ---
    fun loadServer(serverId: Long) {
        screenModelScope.launch {
            device = repository.getServerById(serverId)
        }
    }

    fun fetchMetrics() {
        val currentDevice = device ?: return
        screenModelScope.launch {
            uiState = DetailUiState.Loading

            val result = sshClient.fetchMetrics(currentDevice)

            uiState = result.fold(
                onSuccess = { metrics ->
                    // --- AQUÍ ESTÁ LA MAGIA ---
                    // 2. Leemos los días de retención configurados por el usuario
                    val retentionDays = settingsRepository.settings.value.metricsRetentionDays

                    // 3. Guardamos la métrica y limpiamos el historial antiguo en segundo plano
                    repository.saveMetricsAndCleanOld(
                        serverId = currentDevice.id,
                        metrics = metrics,
                        retentionDays = retentionDays
                    )

                    // 4. Actualizamos el estado de la UI para mostrar los gráficos
                    DetailUiState.Success(metrics)
                },
                onFailure = { DetailUiState.Error(it.message ?: "Error") }
            )
        }
    }

    // --- Procesos ---
    fun fetchProcesses() {
        val currentDevice = device ?: return
        screenModelScope.launch {
            isProcessesLoading = true
            // NOTA: Asegúrate de que SshClient.fetchProcesses acepta 'Device'
            sshClient.fetchProcesses(currentDevice).onSuccess { rawList ->
                processes = sortList(rawList, processSortOption)
            }
            isProcessesLoading = false
        }
    }

    fun sortProcesses(option: ProcessSortOption) {
        processSortOption = option
        processes = sortList(processes, option)
    }

    private fun sortList(list: List<ProcessInfo>, option: ProcessSortOption): List<ProcessInfo> {
        return when (option) {
            ProcessSortOption.CPU -> list.sortedByDescending { it.cpuUsage }
            ProcessSortOption.MEM -> list.sortedByDescending { it.memUsage }
            ProcessSortOption.PID -> list.sortedBy { it.pid.toIntOrNull() ?: 0 }
            ProcessSortOption.NAME -> list.sortedBy { it.command }
        }
    }

    fun shutdownServer() {
        val currentDevice = device ?: return
        screenModelScope.launch { sshClient.shutdown(currentDevice) }
    }

    // --- Terminal ---
    fun startTerminal() {
        val currentDevice = device ?: return
        if (terminalSession != null) return

        screenModelScope.launch {
            terminalOutput = "Conectado a ${currentDevice.ip}...\n"

            // NOTA: Asegúrate de que SshClient.openTerminal acepta 'Device'
            sshClient.openTerminal(currentDevice).fold(
                onSuccess = { session ->
                    terminalSession = session

                    session.output.collect { newRawText ->
                        terminalOutput += newRawText
                        if (terminalOutput.length > 15000) {
                            terminalOutput = terminalOutput.takeLast(15000)
                        }
                    }
                },
                onFailure = { terminalOutput += "Error: ${it.message}\n" }
            )
        }
    }

    fun sendInput(input: String) {
        screenModelScope.launch {
            terminalSession?.write(input)
        }
    }

    override fun onDispose() {
        terminalSession?.close()
        terminalSession = null
        super.onDispose()
    }
}