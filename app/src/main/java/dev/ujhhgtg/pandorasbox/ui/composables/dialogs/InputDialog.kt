package dev.ujhhgtg.pandorasbox.ui.composables.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import dev.ujhhgtg.pandorasbox.R
import dev.ujhhgtg.pandorasbox.ui.composables.Text

@Composable
fun <T> InputDialog(
    title: @Composable () -> Unit,
    textFieldLabel: @Composable () -> Unit,
    value: T,
    onValueChange: (T) -> Unit,
    onParseValue: (String) -> T,
    onDismissRequest: () -> Unit,
    onValidate: (T) -> String? = { null }
) {
    var textFieldValue by remember { mutableStateOf(TextFieldValue(value.toString())) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = title,
        text = {
            Column {
                OutlinedTextField(
                    value = textFieldValue,
                    onValueChange = { textFieldValue = it },
                    label = textFieldLabel,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                )
                errorMessage?.let {
                    Text(text = it, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val value = onParseValue(textFieldValue.text)
                val result = onValidate(value)
                if (result != null) {
                    errorMessage = result
                    return@TextButton
                }
                onValueChange(value)
                onDismissRequest()
            }) {
                Text(R.string.ok)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(R.string.cancel)
            }
        }
    )
}