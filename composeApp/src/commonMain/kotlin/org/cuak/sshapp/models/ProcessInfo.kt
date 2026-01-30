package org.cuak.sshapp.models

data class ProcessInfo(
    val pid: String,
    val user: String,
    val cpuUsage: Double,
    val memUsage: Double,
    val command: String
)

enum class ProcessSortOption {
    CPU, MEM, PID, NAME
}