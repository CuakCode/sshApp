package org.cuak.sshapp.ui.screens

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
import org.cuak.sshapp.utils.AnsiUtils

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

    // Output visual
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
            server?.let { fetchMetrics() }
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
                        // 1. Limpieza ANSI básica
                        val textWithoutAnsi = AnsiUtils.stripAnsiCodes(newRawText)

                        // 2. Procesado avanzado (Backspaces y Control Chars)
                        terminalOutput = processTerminalOutput(terminalOutput, textWithoutAnsi)
                    }
                },
                onFailure = { terminalOutput += "Error: ${it.message}\n" }
            )
        }
    }

    /**
     * Procesa el texto:
     * 1. Maneja Backspaces (\b o 0x7F) para borrar caracteres.
     * 2. Elimina caracteres de control no imprimibles (0x00..0x1F) excepto \n.
     */
    private fun processTerminalOutput(currentText: String, newText: String): String {
        val sb = StringBuilder(currentText)

        for (char in newText) {
            val code = char.code

            // Caso 1: Backspace (Borrar carácter anterior)
            if (char == '\b' || code == 127) {
                if (sb.isNotEmpty()) {
                    sb.deleteAt(sb.length - 1)
                }
                continue
            }

            // Caso 2: Saltos de línea (Permitidos)
            if (char == '\n') {
                sb.append(char)
                continue
            }

            // Caso 3: Caracteres de Control (Ignorar basura como Bell, Tabs crudos, etc.)
            // El rango 0..31 son controles. Solo permitimos imprimibles (>= 32).
            if (code < 32) {
                // Si es un TAB (\t = 9), podríamos convertirlo a espacios,
                // pero a veces el echo del servidor ya lo expande.
                // Si ves que faltan espacios, descomenta la siguiente línea:
                // if (code == 9) sb.append("    ")
                continue
            }

            // Caso 4: Texto normal
            sb.append(char)
        }

        // Limitar buffer
        return if (sb.length > 5000) sb.substring(sb.length - 5000) else sb.toString()
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