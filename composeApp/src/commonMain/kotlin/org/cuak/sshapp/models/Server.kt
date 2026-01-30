package org.cuak.sshapp.models

enum class DeviceType {
    SERVER, CAMERA
}

data class Server(
    val id: Long = 0,
    val name: String,
    val ip: String,
    val port: Int = 22,
    val username: String,
    val password: String? = null,
    val sshKeyPath: String? = null,
    val status: ServerStatus = ServerStatus.UNKNOWN,
    val iconName: String = "dns",
    val type: DeviceType = DeviceType.SERVER // Nuevo campo
) {
    // Helper para obtener la URL RTSP si es cámara
    fun getRtspUrl(): String {
        return "rtsp://${ip}:554/ch0_0.h264" // URL estándar de Yi-Hack v2
    }
}

enum class ServerStatus { ONLINE, OFFLINE, UNKNOWN }