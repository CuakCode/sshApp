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
import org.cuak.sshapp.models.Server
import org.cuak.sshapp.models.ServerMetrics
import org.cuak.sshapp.repository.ServerRepository

sealed class DetailUiState {
    object Idle : DetailUiState()
    object Loading : DetailUiState()
    data class Success(val metrics: ServerMetrics) : DetailUiState()
    data class Error(val message: String) : DetailUiState()
}

class ServerDetailViewModel(
    private val repository: ServerRepository,
    private val sshClient: SshClient
) : ScreenModel {

    var server by mutableStateOf<Server?>(null)
        private set

    var uiState by mutableStateOf<DetailUiState>(DetailUiState.Idle)
        private set

    private var terminalSession: SshTerminalSession? = null

    // Output visual (Acumula el texto crudo con códigos ANSI)
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
            server = repository.getServerById(serverId)
            // Opcional: Cargar métricas automáticamente al iniciar si NO es cámara
            // Si es cámara, la UI priorizará el video, así que quizás no queremos cargar métricas inmediatamente
            // para no saturar la red. Dejamos que la pestaña Monitor las pida.
        }
    }

    fun fetchMetrics() {
        val currentServer = server ?: return
        screenModelScope.launch {
            uiState = DetailUiState.Loading
            val result = sshClient.fetchMetrics(currentServer)
            uiState = result.fold(
                onSuccess = { DetailUiState.Success(it) },
                onFailure = { DetailUiState.Error(it.message ?: "Error") }
            )
        }
    }

    // --- Procesos ---
    fun fetchProcesses() {
        val currentServer = server ?: return
        screenModelScope.launch {
            isProcessesLoading = true
            sshClient.fetchProcesses(currentServer).onSuccess { rawList ->
                // Aplicamos ordenación inicial
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
        val currentServer = server ?: return
        screenModelScope.launch { sshClient.shutdown(currentServer) }
    }

    // --- Terminal ---
    fun startTerminal() {
        val currentServer = server ?: return
        if (terminalSession != null) return

        screenModelScope.launch {
            terminalOutput = "Conectado a ${currentServer.ip}...\n"

            sshClient.openTerminal(currentServer).fold(
                onSuccess = { session ->
                    terminalSession = session

                    session.output.collect { newRawText ->
                        // Acumulamos texto crudo
                        terminalOutput += newRawText

                        // Limpieza preventiva buffer
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