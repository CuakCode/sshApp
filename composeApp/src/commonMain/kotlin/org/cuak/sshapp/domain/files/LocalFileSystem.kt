package org.cuak.sshapp.domain.files

import org.cuak.sshapp.models.SftpFile

interface LocalFileSystem {
    fun getInitialPath(): String
    fun getParentPath(path: String): String
    fun listFiles(path: String): List<SftpFile>
}