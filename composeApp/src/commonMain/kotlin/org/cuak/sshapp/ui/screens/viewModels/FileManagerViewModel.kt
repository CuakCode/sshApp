// composeApp/src/commonMain/kotlin/org/cuak/sshapp/ui/screens/filemanager/FileManagerViewModel.kt
package org.cuak.sshapp.ui.screens.viewModels

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
import org.cuak.sshapp.models.Device
import org.cuak.sshapp.models.SftpFile

enum class SortOption { NAME, SIZE, DATE }
enum class SortDirection { ASC, DESC }

class FileManagerViewModel(
    private val device: Device, // <- Cambiado a Device
    private val sshClient: SshClient,
    private val localFileSystem: LocalFileSystem
) : ScreenModel {

    // --- Estado Local ---
    private val _localPath = MutableStateFlow(localFileSystem.getInitialPath())
    val localPath: StateFlow<String> = _localPath.asStateFlow()

    private val _localFiles = MutableStateFlow<List<SftpFile>>(emptyList())
    val localFiles: StateFlow<List<SftpFile>> = _localFiles.asStateFlow()

    // --- Ordenación Local (Independiente) ---
    var localSortOption by mutableStateOf(SortOption.NAME)
    var localSortDirection by mutableStateOf(SortDirection.ASC)

    // --- Estado Remoto ---
    private val _remotePath = MutableStateFlow(".")
    val remotePath: StateFlow<String> = _remotePath.asStateFlow()

    private val _remoteFiles = MutableStateFlow<List<SftpFile>>(emptyList())
    val remoteFiles: StateFlow<List<SftpFile>> = _remoteFiles.asStateFlow()

    // --- Ordenación Remota (Independiente) ---
    var remoteSortOption by mutableStateOf(SortOption.NAME)
    var remoteSortDirection by mutableStateOf(SortDirection.ASC)

    // --- UI General ---
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _statusMessage = MutableStateFlow("")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private val _transferProgress = MutableStateFlow<Float?>(null)
    val transferProgress: StateFlow<Float?> = _transferProgress.asStateFlow()

    init {
        localFileSystem.clearTempFiles()
        refreshLocal()
        refreshRemote()
    }

    // --- LÓGICA DE ORDENACIÓN LOCAL ---
    fun toggleLocalSort(option: SortOption) {
        if (localSortOption == option) {
            localSortDirection = if (localSortDirection == SortDirection.ASC) SortDirection.DESC else SortDirection.ASC
        } else {
            localSortOption = option
            localSortDirection = SortDirection.ASC
        }
        _localFiles.value = sortFiles(_localFiles.value, localSortOption, localSortDirection)
    }

    // --- LÓGICA DE ORDENACIÓN REMOTA ---
    fun toggleRemoteSort(option: SortOption) {
        if (remoteSortOption == option) {
            remoteSortDirection = if (remoteSortDirection == SortDirection.ASC) SortDirection.DESC else SortDirection.ASC
        } else {
            remoteSortOption = option
            remoteSortDirection = SortDirection.ASC
        }
        _remoteFiles.value = sortFiles(_remoteFiles.value, remoteSortOption, remoteSortDirection)
    }

    // Función pura de ordenación
    private fun sortFiles(files: List<SftpFile>, option: SortOption, direction: SortDirection): List<SftpFile> {
        val sorted = when (option) {
            SortOption.NAME -> files.sortedBy { it.name.lowercase() }
            SortOption.SIZE -> files.sortedBy { it.size }
            SortOption.DATE -> files.sortedBy { it.lastModified }
        }
        val directed = if (direction == SortDirection.DESC) sorted.reversed() else sorted
        return directed.sortedBy { !it.isDirectory } // Carpetas siempre arriba
    }

    // --- NAVEGACIÓN LOCAL ---
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
        _localFiles.value = sortFiles(rawFiles, localSortOption, localSortDirection)
    }

    fun openLocalFile(file: SftpFile) {
        if (!file.isDirectory) {
            try {
                localFileSystem.openFile(file.path)
            } catch (e: Exception) {
                _statusMessage.value = "No se puede abrir: ${e.message}"
            }
        }
    }

    // --- NAVEGACIÓN REMOTA ---
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
            sshClient.listRemoteFiles(device, _remotePath.value) // Pasamos el 'device'
                .onSuccess { files ->
                    _remoteFiles.value = sortFiles(files, remoteSortOption, remoteSortDirection)
                }
                .onFailure {
                    _statusMessage.value = "Error remoto: ${it.message}"
                }
            _isLoading.value = false
        }
    }

    // --- TRANSFERENCIAS ---
    fun upload(file: SftpFile) {
        if (file.isDirectory) return
        screenModelScope.launch {
            _isLoading.value = true
            _statusMessage.value = "Subiendo ${file.name}..."
            _transferProgress.value = 0f

            val destPath = if (_remotePath.value.endsWith("/")) "${_remotePath.value}${file.name}" else "${_remotePath.value}/${file.name}"

            sshClient.uploadFile(device, file.path, destPath) { progress -> // Pasamos el 'device'
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

            sshClient.downloadFile(device, file.path, destPath) { progress -> // Pasamos el 'device'
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

    fun openRemoteFile(file: SftpFile) {
        if (file.isDirectory) {
            val separator = if (_remotePath.value.endsWith("/")) "" else "/"
            navigateRemote("${_remotePath.value}$separator${file.name}")
        } else {
            screenModelScope.launch {
                _isLoading.value = true
                _statusMessage.value = "Abriendo ${file.name}..."

                val tempPath = localFileSystem.getTempFilePath(file.name)

                sshClient.downloadFile(device, file.path, tempPath) { progress -> // Pasamos el 'device'
                    _transferProgress.value = progress
                }
                    .onSuccess {
                        _statusMessage.value = "Abriendo..."
                        _transferProgress.value = null

                        try {
                            localFileSystem.openFile(tempPath)
                        } catch (e: Exception) {
                            _statusMessage.value = "Error al abrir: ${e.message}"
                        }
                    }
                    .onFailure {
                        _statusMessage.value = "Error al descargar: ${it.message}"
                    }

                _isLoading.value = false
                _transferProgress.value = null
            }
        }
    }

    override fun onDispose() {
        super.onDispose()
        localFileSystem.clearTempFiles()
    }
}