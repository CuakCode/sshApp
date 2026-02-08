// composeApp/src/commonMain/kotlin/org/cuak/sshapp/ui/screens/filemanager/FileManagerViewModel.kt
package org.cuak.sshapp.ui.screens.tabs

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.cuak.sshapp.domain.files.LocalFileSystem
import org.cuak.sshapp.domain.ssh.SshClient
import org.cuak.sshapp.models.Server
import org.cuak.sshapp.models.SftpFile

// Opciones de ordenación
enum class SortOption { NAME, SIZE, DATE }
enum class SortDirection { ASC, DESC }

class FileManagerViewModel(
    private val server: Server,
    private val sshClient: SshClient,
    private val localFileSystem: LocalFileSystem
) : ScreenModel {

    // --- Estado Local ---
    private val _localPath = MutableStateFlow(localFileSystem.getInitialPath())
    val localPath: StateFlow<String> = _localPath.asStateFlow()

    private val _localFiles = MutableStateFlow<List<SftpFile>>(emptyList())
    val localFiles: StateFlow<List<SftpFile>> = _localFiles.asStateFlow()

    // --- Estado Remoto ---
    private val _remotePath = MutableStateFlow(".")
    val remotePath: StateFlow<String> = _remotePath.asStateFlow()

    private val _remoteFiles = MutableStateFlow<List<SftpFile>>(emptyList())
    val remoteFiles: StateFlow<List<SftpFile>> = _remoteFiles.asStateFlow()

    // --- UI General ---
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _statusMessage = MutableStateFlow("")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    // --- Progreso de Transferencia (0.0 - 1.0) ---
    private val _transferProgress = MutableStateFlow<Float?>(null)
    val transferProgress: StateFlow<Float?> = _transferProgress.asStateFlow()

    // --- Ordenación ---
    var sortOption by mutableStateOf(SortOption.NAME)
    var sortDirection by mutableStateOf(SortDirection.ASC)

    init {
        refreshLocal()
        refreshRemote()
    }

    // Cambiar ordenación al pulsar botones
    fun toggleSort(option: SortOption) {
        if (sortOption == option) {
            sortDirection = if (sortDirection == SortDirection.ASC) SortDirection.DESC else SortDirection.ASC
        } else {
            sortOption = option
            sortDirection = SortDirection.ASC
        }
        // Reordenar listas actuales inmediatamente
        _localFiles.value = sortFiles(_localFiles.value)
        _remoteFiles.value = sortFiles(_remoteFiles.value)
    }

    private fun sortFiles(files: List<SftpFile>): List<SftpFile> {
        val sorted = when (sortOption) {
            SortOption.NAME -> files.sortedBy { it.name.lowercase() }
            SortOption.SIZE -> files.sortedBy { it.size }
            SortOption.DATE -> files.sortedBy { it.lastModified }
        }
        val directed = if (sortDirection == SortDirection.DESC) sorted.reversed() else sorted
        // Carpetas siempre primero
        return directed.sortedBy { !it.isDirectory }
    }

    fun navigateLocal(path: String) {
        _localPath.value = path
        refreshLocal()
    }

    fun navigateLocalUp() {
        val current = _localPath.value
        val parent = localFileSystem.getParentPath(current)
        navigateLocal(parent)
    }

    private fun refreshLocal() {
        val rawFiles = localFileSystem.listFiles(_localPath.value)
        _localFiles.value = sortFiles(rawFiles)
    }

    // --- Abrir Archivo Local ---
    fun openLocalFile(file: SftpFile) {
        if (!file.isDirectory) {
            try {
                localFileSystem.openFile(file.path)
            } catch (e: Exception) {
                _statusMessage.value = "No se puede abrir: ${e.message}"
            }
        }
    }

    fun navigateRemote(path: String) {
        _remotePath.value = path
        refreshRemote()
    }

    fun navigateRemoteUp() {
        val current = _remotePath.value
        val parent = if (current.lastIndexOf('/') > 0) {
            current.substringBeforeLast('/')
        } else if (current == "." || current.isEmpty()) {
            ".."
        } else {
            "/"
        }
        navigateRemote(parent)
    }

    private fun refreshRemote() {
        screenModelScope.launch {
            _isLoading.value = true
            sshClient.listRemoteFiles(server, _remotePath.value)
                .onSuccess { files ->
                    _remoteFiles.value = sortFiles(files)
                }
                .onFailure {
                    _statusMessage.value = "Error remoto: ${it.message}"
                }
            _isLoading.value = false
        }
    }

    fun upload(file: SftpFile) {
        if (file.isDirectory) return
        screenModelScope.launch {
            _isLoading.value = true
            _statusMessage.value = "Subiendo ${file.name}..."
            _transferProgress.value = 0f

            val destPath = if (_remotePath.value.endsWith("/")) "${_remotePath.value}${file.name}" else "${_remotePath.value}/${file.name}"

            sshClient.uploadFile(server, file.path, destPath) { progress ->
                _transferProgress.value = progress
            }
                .onSuccess {
                    _statusMessage.value = "Subida completada."
                    refreshRemote()
                }
                .onFailure { _statusMessage.value = "Error: ${it.message}" }

            _transferProgress.value = null
            _isLoading.value = false
        }
    }

    fun download(file: SftpFile) {
        if (file.isDirectory) return
        screenModelScope.launch {
            _isLoading.value = true
            _statusMessage.value = "Descargando ${file.name}..."
            _transferProgress.value = 0f

            val separator = if (_localPath.value.endsWith("/") || _localPath.value.endsWith("\\")) "" else "/"
            val destPath = "${_localPath.value}$separator${file.name}"

            sshClient.downloadFile(server, file.path, destPath) { progress ->
                _transferProgress.value = progress
            }
                .onSuccess {
                    _statusMessage.value = "Descarga completada."
                    refreshLocal()
                }
                .onFailure { _statusMessage.value = "Error: ${it.message}" }

            _transferProgress.value = null
            _isLoading.value = false
        }
    }
}