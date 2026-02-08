// composeApp/src/sharedJvmMain/kotlin/org/cuak/sshapp/domain/files/JvmLocalFileSystem.kt
package org.cuak.sshapp.domain.files

import org.cuak.sshapp.models.SftpFile
import java.io.File
import java.awt.Desktop

class JvmLocalFileSystem : LocalFileSystem {

    override fun getInitialPath(): String = System.getProperty("user.home") ?: "/"

    override fun getParentPath(path: String): String = File(path).parent ?: path

    override fun listFiles(path: String): List<SftpFile> {
        val directory = File(path)
        if (!directory.exists() || !directory.isDirectory) return emptyList()

        return directory.listFiles()?.map { file ->
            SftpFile(
                name = file.name,
                path = file.absolutePath,
                isDirectory = file.isDirectory,
                size = file.length(),
                permissions = if (file.canWrite()) "rw" else "r",
                lastModified = file.lastModified() // Importante para ordenar por fecha
            )
        }?.sortedWith(compareBy({ !it.isDirectory }, { it.name })) ?: emptyList()
    }
    
    override fun openFile(path: String) {
        try {
            val file = File(path)
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(file)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}