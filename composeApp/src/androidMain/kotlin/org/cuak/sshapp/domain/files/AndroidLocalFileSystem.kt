package org.cuak.sshapp.domain.files

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import org.cuak.sshapp.models.SftpFile
import java.io.File

class AndroidLocalFileSystem(private val context: Context) : LocalFileSystem {

    override fun getInitialPath(): String {
        return Environment.getExternalStorageDirectory().absolutePath
    }

    override fun getParentPath(path: String): String {
        val file = File(path)
        val root = Environment.getExternalStorageDirectory().absolutePath
        if (path == root) return path
        return file.parent ?: path
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
                permissions = if (file.canWrite()) "rw" else "r",
                lastModified = file.lastModified()
            )
        }?.sortedWith(compareBy({ !it.isDirectory }, { it.name })) ?: emptyList()
    }


    // --- CORRECCIÓN 2: Intent Robustos con Permisos ---
    override fun openFile(path: String) {
        try {
            val file = File(path)

            // Log para depuración
            println("Abriendo archivo en: ${file.absolutePath}, existe: ${file.exists()}, tamaño: ${file.length()}")

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )

            val mimeType = getMimeType(file)

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                // Estos flags son CRÍTICOS para que la otra app pueda leer el archivo
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            // Usar createChooser fuerza al sistema a re-evaluar los permisos URI para la app destino
            val chooser = Intent.createChooser(intent, "Abrir con...")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            context.startActivity(chooser)

        } catch (e: Exception) {
            e.printStackTrace()
            println("Error al abrir archivo: ${e.message}")
        }

    }

    private val TEMP_FOLDER_NAME = "ssh_temp"

    // ... (getInitialPath, getParentPath, listFiles igual que antes) ...

    override fun getTempFilePath(fileName: String): String {
        // Usamos externalCacheDir/ssh_temp
        val baseDir = context.externalCacheDir ?: context.cacheDir
        val tempDir = File(baseDir, TEMP_FOLDER_NAME)

        // Si no existe la carpeta, la creamos
        if (!tempDir.exists()) {
            tempDir.mkdirs()
        }

        // Borramos si ya existía ese archivo concreto para evitar conflictos
        val file = File(tempDir, fileName)
        if (file.exists()) {
            file.delete()
        }

        return file.absolutePath
    }

    override fun clearTempFiles() {
        try {
            val baseDir = context.externalCacheDir ?: context.cacheDir
            val tempDir = File(baseDir, TEMP_FOLDER_NAME)

            if (tempDir.exists()) {
                // Borramos todos los archivos dentro de forma recursiva
                tempDir.deleteRecursively()
                println("Caché temporal eliminada: ${tempDir.absolutePath}")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getMimeType(file: File): String {
        val extension = MimeTypeMap.getFileExtensionFromUrl(file.name) ?: ""
        val type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase())
        println("MimeType detectado para ${file.name}: $type")
        return type ?: "*/*" // Fallback si no se detecta
    }
}