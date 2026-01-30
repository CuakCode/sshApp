package org.cuak.sshapp.ui.screens.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun TerminalTabContent(
    output: String,
    onSendInput: (String) -> Unit,
    onStart: () -> Unit
) {
    val scrollState = rememberScrollState()
    var hiddenInput by remember { mutableStateOf(" ") }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(output) { scrollState.animateScrollTo(scrollState.maxValue) }
    LaunchedEffect(Unit) {
        onStart()
        delay(300)
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF1E1E1E)).padding(8.dp)) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth().background(Color.Black, RoundedCornerShape(4.dp)).padding(8.dp)) {
            SelectionContainer {
                Text(
                    text = output,
                    color = Color(0xFF00FF00),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.fillMaxSize().verticalScroll(scrollState)
                )
            }
            TextField(
                value = hiddenInput,
                onValueChange = { newValue ->
                    if (newValue.length < 1) onSendInput("\u007F")
                    else if (newValue.length > 1) onSendInput(newValue.substring(1))
                    hiddenInput = " "
                },
                modifier = Modifier.alpha(0f).size(1.dp).focusRequester(focusRequester),
                keyboardOptions = KeyboardOptions(autoCorrect = false, keyboardType = KeyboardType.Ascii, imeAction = ImeAction.None),
                singleLine = true
            )
        }

        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            TerminalButton("Esc", onClick = { onSendInput("\u001B") }, color = MaterialTheme.colorScheme.secondary)
            TerminalButton("Ctrl+C", onClick = { onSendInput("\u0003") }, color = MaterialTheme.colorScheme.error)
            TerminalButton("⌨️", onClick = { focusRequester.requestFocus(); keyboardController?.show() })
            TerminalButton("⬆", onClick = { onSendInput("\u001B[A") })
            TerminalButton("⬇", onClick = { onSendInput("\u001B[B") })
            TerminalButton("TAB", onClick = { onSendInput("\u0009") })
            TerminalButton("Enter", onClick = { onSendInput("\n") })
        }
    }
}

@Composable
fun TerminalButton(text: String, onClick: () -> Unit, color: Color = MaterialTheme.colorScheme.primaryContainer) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = color),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.height(36.dp)
    ) {
        Text(text, fontSize = MaterialTheme.typography.labelSmall.fontSize, color = Color.White)
    }
}