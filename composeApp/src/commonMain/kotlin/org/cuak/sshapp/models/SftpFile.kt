package org.cuak.sshapp.models

data class SftpFile(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long,
    val permissions: String = ""
) {
    fun sizeFormatted(): String {
        if (isDirectory) return ""
        val kb = size / 1024.0
        val mb = kb / 1024.0
        return when {
            mb > 1 -> "${mb.toString().take(4)} MB"
            kb > 1 -> "${kb.toString().take(4)} KB"
            else -> "$size B"
        }
    }
}