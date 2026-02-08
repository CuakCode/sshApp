package org.cuak.sshapp.domain.files

import org.cuak.sshapp.models.SftpFile
import java.io.File

class JvmLocalFileSystem : LocalFileSystem {

    override fun getInitialPath(): String {
        return System.getProperty("user.home") ?: "/"
    }

    override fun getParentPath(path: String): String {
        return File(path).parent ?: path
    }

    override fun listFiles(path: String): List<SftpFile> {
        val directory = File(path)
        if (!directory.exists() || !directory.isDirectory) return emptyList()

        return directory.listFiles()?.map { file ->
            SftpFile(
                name = file.name,
                path = file.absolutePath,
                isDirectory = file.isDirectory,
                size = file.length(),
                permissions = if (file.canWrite()) "rw" else "r"
            )
        }?.sortedWith(compareBy({ !it.isDirectory }, { it.name }))
            ?: emptyList()
    }
}