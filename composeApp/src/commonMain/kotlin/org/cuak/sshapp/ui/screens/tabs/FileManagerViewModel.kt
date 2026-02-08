package org.cuak.sshapp.ui.screens.tabs

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

    init {
        refreshLocal()
        refreshRemote()
    }

    // --- Acciones Locales ---
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
        _localFiles.value = localFileSystem.listFiles(_localPath.value)
    }

    // --- Acciones Remotas ---
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
                    _remoteFiles.value = files
                }
                .onFailure {
                    _statusMessage.value = "Error remoto: ${it.message}"
                }
            _isLoading.value = false
        }
    }

    // --- Transferencias ---
    fun upload(file: SftpFile) {
        if (file.isDirectory) {
            _statusMessage.value = "No se pueden subir carpetas enteras aún."
            return
        }
        screenModelScope.launch {
            _isLoading.value = true
            _statusMessage.value = "Subiendo ${file.name}..."

            val destPath = if (_remotePath.value.endsWith("/"))
                "${_remotePath.value}${file.name}"
            else
                "${_remotePath.value}/${file.name}"

            sshClient.uploadFile(server, file.path, destPath)
                .onSuccess {
                    _statusMessage.value = "Subida completada."
                    refreshRemote()
                }
                .onFailure { _statusMessage.value = "Error subiendo: ${it.message}" }
            _isLoading.value = false
        }
    }

    fun download(file: SftpFile) {
        if (file.isDirectory) {
            _statusMessage.value = "No se pueden bajar carpetas enteras aún."
            return
        }
        screenModelScope.launch {
            _isLoading.value = true
            _statusMessage.value = "Descargando ${file.name}..."

            val separator = if (_localPath.value.endsWith("/") || _localPath.value.endsWith("\\")) "" else "/"
            val destPath = "${_localPath.value}$separator${file.name}"

            sshClient.downloadFile(server, file.path, destPath)
                .onSuccess {
                    _statusMessage.value = "Descarga completada."
                    refreshLocal()
                }
                .onFailure { _statusMessage.value = "Error descargando: ${it.message}" }
            _isLoading.value = false
        }
    }
}