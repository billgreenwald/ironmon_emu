package hh.game.mgba_android.tracker

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

private const val MAX_NAME_LENGTH = 10

@Composable
fun NamingOverlay(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Name Entry") },
        text = {
            Column {
                Text(
                    "Type the name, then tap Go. Make sure the naming screen is open and the cursor is on 'A'.",
                    modifier = Modifier.padding(bottom = 12.dp),
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { new ->
                        if (new.length <= MAX_NAME_LENGTH) text = new
                    },
                    singleLine = true,
                    label = { Text("Name (max $MAX_NAME_LENGTH chars)") },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        keyboardType = KeyboardType.Ascii,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (text.isNotEmpty()) onConfirm(text) },
                enabled = text.isNotEmpty(),
            ) {
                Text("Go")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
