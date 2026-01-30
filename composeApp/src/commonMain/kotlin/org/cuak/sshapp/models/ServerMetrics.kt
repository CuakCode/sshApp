package org.cuak.sshapp.models

data class ServerMetrics(
    val cpuPercentage: Double,
    val ramPercentage: Double,
    val diskUsage: List<Double>,
    val temperatures: Map<String, Double>,
    val openPorts: List<PortInfo> = emptyList()
)

data class PortInfo(
    val port: Int,
    val protocol: String, // tcp, udp
    val processName: String // ej: nginx, dropbear
)
