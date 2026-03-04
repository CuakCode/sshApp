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

    
    private var currentColor = Color(0xFF00FF00) 
    private var isBold = false

    init {
        
        lines.add(StringBuilder())
    }

    fun appendText(newInput: String) {
        var i = 0
        while (i < newInput.length) {
            val char = newInput[i]
            when (char) {
                
                '\u001B' -> {
                    
                    val endIndex = findAnsiEndIndex(newInput, i)
                    if (endIndex != -1) {
                        val sequence = newInput.substring(i, endIndex + 1)
                        processAnsiCode(sequence)
                        i = endIndex
                    }
                }
                
                '\r' -> {
                    cursorCol = 0
                }
                
                '\u0008', '\u007F' -> {
                    if (cursorCol > 0) {
                        cursorCol--
                        ensureCursorSpace()
                        
                        if (cursorCol < lines[cursorRow].length) {
                            
                            lines[cursorRow].deleteCharAtKmp(cursorCol)
                        }
                    }
                }
                
                '\n' -> {
                    cursorRow++
                    cursorCol = 0
                    if (cursorRow >= lines.size) {
                        lines.add(StringBuilder())
                    }
                }
                
                else -> {
                    
                    if (char >= ' ' || char == '\t') {
                        ensureCursorSpace()
                        if (cursorCol < lines[cursorRow].length) {
                            
                            lines[cursorRow][cursorCol] = char
                        } else {
                            
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
        
        if (code.endsWith("m")) {
            
            val params = code.drop(2).dropLast(1).split(";")
            for (param in params) {
                when (param.toIntOrNull()) {
                    0 -> { currentColor = Color(0xFF00FF00); isBold = false } 
                    1 -> isBold = true
                    30 -> currentColor = Color.Black
                    31 -> currentColor = Color.Red
                    32 -> currentColor = Color.Green
                    33 -> currentColor = Color.Yellow
                    34 -> currentColor = Color.Blue
                    35 -> currentColor = Color.Magenta
                    36 -> currentColor = Color.Cyan
                    37 -> currentColor = Color.White
                    39 -> currentColor = Color(0xFF00FF00) 
                }
            }
        } else if (code.endsWith("K")) {
            
            ensureCursorSpace()
            if (cursorCol < lines[cursorRow].length) {
                
                lines[cursorRow].setLengthKmp(cursorCol)
            }
        }
        
    }

    /**
     * Devuelve el contenido visual como AnnotatedString para Compose.
     */
    fun toAnnotatedString(): AnnotatedString {
        return buildAnnotatedString {
            
            val startLine = (lines.size - 1000).coerceAtLeast(0)

            withStyle(SpanStyle(color = Color(0xFF00FF00))) {
                for (i in startLine until lines.size) {
                    append(lines[i].toString())
                    if (i < lines.size - 1) append("\n")
                }
            }
        }
    }

    

    
    private fun StringBuilder.deleteCharAtKmp(index: Int) {
        if (index in 0 until length) {
            val str = toString()
            clear()
            append(str.substring(0, index))
            append(str.substring(index + 1))
        }
    }

    
    private fun StringBuilder.setLengthKmp(newLength: Int) {
        if (newLength >= 0 && newLength < length) {
            val str = toString()
            clear()
            append(str.substring(0, newLength))
        }
    }
}