package org.cuak.sshapp.models

data class ServerMetrics(
    val cpuPercentage: Double,
    val ramPercentage: Double,
    val diskUsage: List<Double>,
    val temperatures: Map<String, Double>
)


