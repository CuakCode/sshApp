package org.cuak.sshapp.domain.files

import org.cuak.sshapp.models.SftpFile

interface LocalFileSystem {
    fun getInitialPath(): String
    fun getParentPath(path: String): String
    fun listFiles(path: String): List<SftpFile>
    fun openFile(path: String)
    fun getTempFilePath(fileName: String): String
    fun clearTempFiles()
}