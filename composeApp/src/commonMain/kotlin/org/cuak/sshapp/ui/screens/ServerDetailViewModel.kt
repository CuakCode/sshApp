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

// Estados de la UI
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

    fun loadServer(serverId: Long) {
        screenModelScope.launch {
            server = repository.getServerById(serverId)
            // Una vez cargado el servidor, intentamos conectar automáticamente
            server?.let { fetchMetrics() }
        }
    }

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
}