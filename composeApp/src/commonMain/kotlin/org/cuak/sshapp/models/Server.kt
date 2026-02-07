package org.cuak.sshapp.models

enum class DeviceType {
    SERVER, CAMERA
}

enum class ServerStatus { ONLINE, OFFLINE, UNKNOWN }

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
    val type: DeviceType = DeviceType.SERVER,
    val cameraProtocol: String? = "RTSP",
    val cameraPort: Int? = 8554,
    val cameraStream: String? = "ch0_0.h264"
)

val Server.rtspUrl: String
    get() {
        val protocol = cameraProtocol ?: "rtsp"
        val cPort = cameraPort ?: 8554
        val stream = cameraStream ?: "ch0_0.h264"
        return "$protocol://$ip:$cPort/$stream"
    }