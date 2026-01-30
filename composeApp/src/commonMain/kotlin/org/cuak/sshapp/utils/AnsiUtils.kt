package org.cuak.sshapp.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle

/**
 * Procesa texto ANSI y mantiene un buffer de líneas simulando una terminal básica.
 * Soporta colores, \r (Carriage Return), \b (Backspace) y limpieza de línea.
 */
class TerminalBuffer {
    private val lines = mutableListOf<StringBuilder>()
    private var cursorRow = 0
    private var cursorCol = 0

    // Estilos actuales
    private var currentColor = Color(0xFF00FF00) // Verde terminal por defecto
    private var isBold = false

    init {
        // Empezamos con una línea vacía
        lines.add(StringBuilder())
    }

    fun appendText(newInput: String) {
        var i = 0
        while (i < newInput.length) {
            val char = newInput[i]
            when (char) {
                // ANSI Escape Sequence (Inicio)
                '\u001B' -> {
                    // Detectar secuencia completa (ej: [31m)
                    val endIndex = findAnsiEndIndex(newInput, i)
                    if (endIndex != -1) {
                        val sequence = newInput.substring(i, endIndex + 1)
                        processAnsiCode(sequence)
                        i = endIndex
                    }
                }
                // Carriage Return: Volver al inicio de la línea actual
                '\r' -> {
                    cursorCol = 0
                }
                // Backspace: Retroceder cursor y borrar carácter
                '\u0008', '\u007F' -> {
                    if (cursorCol > 0) {
                        cursorCol--
                        ensureCursorSpace()
                        // Simulamos borrado visual reemplazando con espacio o truncando si es el final
                        if (cursorCol < lines[cursorRow].length) {
                            // CORRECCIÓN: Usamos nuestra función helper compatible con KMP
                            lines[cursorRow].deleteCharAtKmp(cursorCol)
                        }
                    }
                }
                // New Line
                '\n' -> {
                    cursorRow++
                    cursorCol = 0
                    if (cursorRow >= lines.size) {
                        lines.add(StringBuilder())
                    }
                }
                // Caracteres normales
                else -> {
                    // Filtramos caracteres de control raros
                    if (char >= ' ' || char == '\t') {
                        ensureCursorSpace()
                        if (cursorCol < lines[cursorRow].length) {
                            // Sobrescribir (comportamiento terminal)
                            lines[cursorRow][cursorCol] = char
                        } else {
                            // Añadir
                            lines[cursorRow].append(char)
                        }
                        cursorCol++
                    }
                }
            }
            i++
        }
    }

    private fun ensureCursorSpace() {
        // Asegura que existan líneas/espacios hasta la posición del cursor
        while (cursorRow >= lines.size) lines.add(StringBuilder())
        val currentLine = lines[cursorRow]
        while (currentLine.length < cursorCol) currentLine.append(' ')
    }

    private fun findAnsiEndIndex(text: String, startIndex: Int): Int {
        if (startIndex + 1 >= text.length || text[startIndex + 1] != '[') return -1
        for (j in startIndex + 2 until text.length) {
            val c = text[j]
            if (c in 'A'..'Z' || c in 'a'..'z') return j
        }
        return -1
    }

    private fun processAnsiCode(code: String) {
        // Ejemplo código: \u001B[31m
        if (code.endsWith("m")) {
            // Códigos de Color / Estilo
            val params = code.drop(2).dropLast(1).split(";")
            for (param in params) {
                when (param.toIntOrNull()) {
                    0 -> { currentColor = Color(0xFF00FF00); isBold = false } // Reset
                    1 -> isBold = true
                    30 -> currentColor = Color.Black
                    31 -> currentColor = Color.Red
                    32 -> currentColor = Color.Green
                    33 -> currentColor = Color.Yellow
                    34 -> currentColor = Color.Blue
                    35 -> currentColor = Color.Magenta
                    36 -> currentColor = Color.Cyan
                    37 -> currentColor = Color.White
                    39 -> currentColor = Color(0xFF00FF00) // Default FG
                }
            }
        } else if (code.endsWith("K")) {
            // \u001B[K -> Clear line from cursor to end
            ensureCursorSpace()
            if (cursorCol < lines[cursorRow].length) {
                // CORRECCIÓN: Usamos helper para setLength (truncar)
                lines[cursorRow].setLengthKmp(cursorCol)
            }
        }
        // Se pueden añadir códigos de movimiento de cursor (A, B, C, D) si fuera necesario
    }

    /**
     * Devuelve el contenido visual como AnnotatedString para Compose.
     */
    fun toAnnotatedString(): AnnotatedString {
        return buildAnnotatedString {
            // Por rendimiento, renderizamos solo las últimas 1000 líneas
            val startLine = (lines.size - 1000).coerceAtLeast(0)

            withStyle(SpanStyle(color = Color(0xFF00FF00))) {
                for (i in startLine until lines.size) {
                    append(lines[i].toString())
                    if (i < lines.size - 1) append("\n")
                }
            }
        }
    }

    // --- HELPERS COMPATIBLES CON KMP (CommonMain) ---

    // Simula deleteCharAt(index) reconstruyendo el string
    private fun StringBuilder.deleteCharAtKmp(index: Int) {
        if (index in 0 until length) {
            val str = toString()
            clear()
            append(str.substring(0, index))
            append(str.substring(index + 1))
        }
    }

    // Simula setLength(newLength) para truncar
    private fun StringBuilder.setLengthKmp(newLength: Int) {
        if (newLength >= 0 && newLength < length) {
            val str = toString()
            clear()
            append(str.substring(0, newLength))
        }
    }
}