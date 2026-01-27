package org.cuak.sshapp.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.launch
import org.cuak.sshapp.domain.ssh.SshClient
import org.cuak.sshapp.models.Server
import org.cuak.sshapp.models.ServerMetrics
import org.cuak.sshapp.repository.ServerRepository
import org.cuak.sshapp.domain.ssh.SshTerminalSession

// Estados de la UI para la pestaña de métricas
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

    // --- Estado General ---
    var server by mutableStateOf<Server?>(null)
        private set

    // --- Estado de Métricas ---
    var uiState by mutableStateOf<DetailUiState>(DetailUiState.Idle)
        private set

    // --- Estado de Terminal ---
    private var terminalSession: SshTerminalSession? = null

    // Salida de texto de la terminal (lo que se muestra en pantalla)
    var terminalOutput by mutableStateOf("")
        private set

    // Entrada de texto (lo que el usuario está escribiendo)
    var terminalInput by mutableStateOf("")

    // --- Lógica de Inicialización ---

    fun loadServer(serverId: Long) {
        screenModelScope.launch {
            server = repository.getServerById(serverId)
            // Una vez cargado el servidor, intentamos conectar automáticamente para métricas
            server?.let { fetchMetrics() }
        }
    }

    // --- Lógica de Métricas y Gestión ---

    fun fetchMetrics() {
        val currentServer = server ?: return

        screenModelScope.launch {
            uiState = DetailUiState.Loading

            // Llamada al cliente SSH (JvmSshClient en Android/Desktop)
            val result = sshClient.fetchMetrics(currentServer)

            uiState = result.fold(
                onSuccess = { metrics -> DetailUiState.Success(metrics) },
                onFailure = { error -> DetailUiState.Error(error.message ?: "Error desconocido") }
            )
        }
    }

    fun shutdownServer() {
        val currentServer = server ?: return

        screenModelScope.launch {
            val result = sshClient.shutdown(currentServer)

            result.fold(
                onSuccess = {
                    println("Comando de apagado enviado con éxito")
                    // Opcional: Podrías actualizar el estado de UI o navegar atrás
                },
                onFailure = { error ->
                    println("Error al apagar: ${error.message}")
                }
            )
        }
    }

    // --- Lógica de Terminal Interactiva ---

    fun startTerminal() {
        val currentServer = server ?: return

        // Si ya hay una sesión activa, no hacemos nada o la reiniciamos (aquí optamos por no duplicar)
        if (terminalSession != null) return

        screenModelScope.launch {
            terminalOutput = "Iniciando conexión segura con ${currentServer.ip}...\n"

            sshClient.openTerminal(currentServer).fold(
                onSuccess = { session ->
                    terminalSession = session
                    terminalOutput += "Conexión establecida.\n"

                    // Escuchamos el flujo de salida del servidor
                    session.output.collect { newText ->
                        // Concatenamos y limitamos el buffer a los últimos 5000 caracteres
                        // para evitar problemas de memoria en la UI
                        terminalOutput = (terminalOutput + newText).takeLast(5000)
                    }
                },
                onFailure = { error ->
                    terminalOutput += "Error al conectar: ${error.message}\n"
                }
            )
        }
    }

    fun sendTerminalCommand(command: String) {
        screenModelScope.launch {
            // Enviamos el comando seguido de un salto de línea para ejecutarlo
            terminalSession?.write(command + "\n")
            // Limpiamos el campo de entrada
            terminalInput = ""
        }
    }

    // Envía teclas especiales que no requieren salto de línea automático (como Ctrl+C o flechas)
    fun sendSpecialKey(keyCode: String) {
        screenModelScope.launch {
            terminalSession?.write(keyCode)
        }
    }

    // --- Limpieza ---

    override fun onDispose() {
        // Cerramos la sesión SSH si está abierta al salir de la pantalla
        terminalSession?.close()
        terminalSession = null
        super.onDispose()
    }
}