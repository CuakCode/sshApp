package org.cuak.sshapp.utils

object AnsiUtils {
    // Patrón genérico ANSI (CSI codes)
    private val ANSI_CSI = Regex("\u001B\\[[\\d;?]*[a-zA-Z]")
    // Patrón específico para el modo "Bracketed Paste" que causa el ?2004h
    private val BRACKETED_PASTE_START = Regex("\u001B\\[\\?2004h")
    private val BRACKETED_PASTE_END = Regex("\u001B\\[\\?2004l")
    // Títulos de ventana oscuros (OSC)
    private val ANSI_OSC = Regex("\u001B\\][^\u0007]*\u0007")

    fun stripAnsiCodes(text: String): String {
        var clean = text
        clean = BRACKETED_PASTE_START.replace(clean, "")
        clean = BRACKETED_PASTE_END.replace(clean, "")
        clean = ANSI_OSC.replace(clean, "")
        clean = ANSI_CSI.replace(clean, "")
        return clean
    }
}