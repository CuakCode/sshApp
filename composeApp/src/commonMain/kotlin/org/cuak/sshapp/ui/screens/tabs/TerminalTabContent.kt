package org.cuak.sshapp.ui.screens.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
// Asegúrate de tener este import
import androidx.compose.foundation.layout.imePadding

import org.cuak.sshapp.utils.TerminalBuffer

@Composable
fun TerminalTabContent(
    output: String,
    onSendInput: (String) -> Unit,
    onStart: () -> Unit
) {
    val scrollState = rememberScrollState()
    val clipboardManager = LocalClipboardManager.current
    var inputBuffer by remember { mutableStateOf("x") }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    val processedText = remember(output) {
        val buffer = TerminalBuffer()
        buffer.appendText(output)
        buffer.toAnnotatedString()
    }

    // Auto-scroll: Se activa cuando cambia el texto O cuando cambia el tamaño de la ventana (teclado)
    LaunchedEffect(processedText, scrollState.maxValue) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    LaunchedEffect(Unit) {
        onStart()
        delay(300)
        focusRequester.requestFocus()
        // Opcional: Mostrar teclado automáticamente al entrar
        // keyboardController?.show()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E1E))
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        focusRequester.requestFocus()
                        keyboardController?.show()
                    }
                )
            }
        // Eliminamos el padding general de 8.dp aquí para manejarlo mejor dentro
    ) {
        // --- 1. MOTOR DE ENTRADA (INVISIBLE) ---
        BasicTextField(
            value = inputBuffer,
            onValueChange = { newValue ->
                if (newValue.length < 1) onSendInput("\u007F")
                else if (newValue.length > 1) onSendInput(newValue.last().toString())
                inputBuffer = "x"
            },
            modifier = Modifier
                .size(1.dp)
                .graphicsLayer { alpha = 0f }
                .focusRequester(focusRequester)
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown) {
                        when {
                            event.key == Key.V && event.isCtrlPressed && event.isShiftPressed -> {
                                clipboardManager.getText()?.text?.let { onSendInput(it) }
                                true
                            }
                            event.key == Key.R && event.isCtrlPressed -> {
                                onSendInput("\u0012")
                                true
                            }
                            event.key == Key.C && event.isCtrlPressed && !event.isShiftPressed -> {
                                onSendInput("\u0003")
                                true
                            }
                            event.key == Key.Enter -> { onSendInput("\n"); true }
                            event.key == Key.Backspace -> { onSendInput("\u007F"); true }
                            event.key == Key.Tab -> { onSendInput("\t"); true }
                            event.key == Key.DirectionUp -> { onSendInput("\u001B[A"); true }
                            event.key == Key.DirectionDown -> { onSendInput("\u001B[B"); true }
                            event.key == Key.DirectionRight -> { onSendInput("\u001B[C"); true }
                            event.key == Key.DirectionLeft -> { onSendInput("\u001B[D"); true }
                            event.key == Key.Escape -> { onSendInput("\u001B"); true }
                            else -> false
                        }
                    } else false
                },
            textStyle = TextStyle(color = Color.Transparent),
            cursorBrush = SolidColor(Color.Transparent),
            keyboardOptions = KeyboardOptions(autoCorrect = false, keyboardType = KeyboardType.Ascii, imeAction = ImeAction.None)
        )

        // --- 2. INTERFAZ VISUAL ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                // --- CAMBIO CLAVE ---
                // systemBarsPadding asegura que no se meta bajo la barra de estado
                // imePadding asegura que el contenido suba con el teclado
                .systemBarsPadding()
                .imePadding()
                .padding(8.dp) // Padding visual interno
        ) {
            Box(
                modifier = Modifier
                    .weight(1f) // Esto hace que la terminal ocupe todo el espacio disponible
                    .fillMaxWidth()
                    .background(Color.Black, RoundedCornerShape(4.dp))
                    .padding(4.dp)
            ) {
                SelectionContainer {
                    Text(
                        text = processedText,
                        color = Color(0xFF00FF00),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        lineHeight = 16.sp,
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                    )
                }
            }

            // Barra de Botones
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TerminalButton("Esc", onClick = { onSendInput("\u001B") }, color = MaterialTheme.colorScheme.secondary)
                TerminalButton("Ctrl+C", onClick = { onSendInput("\u0003") }, color = MaterialTheme.colorScheme.error)
                TerminalButton("Tab", onClick = { onSendInput("\t") })
                TerminalButton("⬆", onClick = { onSendInput("\u001B[A") })
                TerminalButton("⬇", onClick = { onSendInput("\u001B[B") })
                TerminalButton("Enter", onClick = { onSendInput("\n") })
            }
        }
    }
}

@Composable
fun TerminalButton(
    text: String,
    onClick: () -> Unit,
    color: Color = MaterialTheme.colorScheme.primaryContainer
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = color),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.height(36.dp).defaultMinSize(minWidth = 48.dp)
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            color = Color.White,
            fontFamily = FontFamily.Monospace
        )
    }
}