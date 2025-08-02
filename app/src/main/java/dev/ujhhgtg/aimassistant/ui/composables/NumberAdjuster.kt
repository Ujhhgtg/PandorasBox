package dev.ujhhgtg.aimassistant.ui.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import dev.ujhhgtg.aimassistant.R
import dev.ujhhgtg.aimassistant.utils.NumberOperations.Companion.add
import dev.ujhhgtg.aimassistant.utils.NumberOperations.Companion.coerceIn
import dev.ujhhgtg.aimassistant.utils.NumberOperations.Companion.compareTo
import dev.ujhhgtg.aimassistant.utils.NumberOperations.Companion.divide
import dev.ujhhgtg.aimassistant.utils.NumberOperations.Companion.subtract
import dev.ujhhgtg.aimassistant.utils.NumberOperations.Companion.toGeneric

@Composable
fun <T> NumberAdjuster(
    label: String,
    value: T,
    defaultValue: T,
    minValue: T,
    maxValue: T,
    valueStep: T,
    onChange: (T) -> Unit
)
where T : Number
{
    var showDialog by remember { mutableStateOf(false) }
    var textFieldValue by remember { mutableStateOf(TextFieldValue(value.toString())) }
    val errorMessage = remember { mutableStateOf<String?>(null) }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text(
                text = label.format(value),
                modifier = Modifier
                    .clickable {
                        textFieldValue = TextFieldValue(value.toString())
                        errorMessage.value = null
                        showDialog = true
                    }
                    .weight(1f),
                style = MaterialTheme.typography.bodyLarge
            )

            IconButton(
                onClick = {
                    val newValue = (value subtract valueStep).coerceIn(minValue, maxValue)
                    onChange(newValue as T)
            }) {
                Icon(
                    painter = painterResource(R.drawable.remove_24px),
                    contentDescription = "decrease"
                )
            }

            IconButton(
                onClick = {
                    val newValue = (value add valueStep).coerceIn(minValue, maxValue)
                    onChange(newValue as T)
            }) {
                Icon(
                    painter = painterResource(R.drawable.add_24px),
                    contentDescription = "increase",
                )
            }

            IconButton(
                onClick = { onChange(defaultValue) }
            ) {
                Icon(
                    painter = painterResource(R.drawable.undo_24px),
                    contentDescription = "reset"
                )
            }
        }

        if (value is Int) {
            Slider(
                value = value.toFloat(),
                onValueChange = {
                    onChange(it.toGeneric(Int::class.java) as T)
                },
                valueRange = minValue.toFloat()..maxValue.toFloat(),
                steps = ((maxValue subtract minValue) divide valueStep).toInt() - 1
            )
        } else {
            Slider(
                value = value.toFloat(),
                onValueChange = {
                    onChange(it as T)
                },
                valueRange = minValue.toFloat()..maxValue.toFloat()
            )
        }


        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("Enter value") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = textFieldValue,
                            onValueChange = { textFieldValue = it },
                            label = { Text("Value ($minValue to $maxValue)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number
                            ),
                            isError = errorMessage.value != null
                        )
                        errorMessage.value?.let {
                            Text(text = it, color = MaterialTheme.colorScheme.error)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val input = textFieldValue.text.toFloatOrNull()
                        if (input == null || input < minValue || input > maxValue) {
                            errorMessage.value = "Please enter a number between $minValue and $maxValue"
                        } else {
                            onChange(input as T)
                            showDialog = false
                        }
                    }) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
