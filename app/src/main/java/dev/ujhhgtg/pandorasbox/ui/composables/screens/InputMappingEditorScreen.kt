package dev.ujhhgtg.pandorasbox.ui.composables.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import dev.ujhhgtg.pandorasbox.R
import dev.ujhhgtg.pandorasbox.models.NormalizedCircle
import dev.ujhhgtg.pandorasbox.ui.composables.Text
import kotlin.math.roundToInt

@Composable
fun InputMapperEditorScreen(
    onClose: () -> Unit
) {
    var editorSize by remember { mutableStateOf(IntSize.Zero) }
    var circleList by rememberSaveable {
        mutableStateOf(emptyList<NormalizedCircle>())
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var newKeyName by remember { mutableStateOf(TextFieldValue("")) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val circleSizeDp = 64.dp
    val circleSizePx = with(LocalDensity.current) { circleSizeDp.toPx().roundToInt() }

    BackHandler(onBack = onClose)

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Text("+")
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.Black)
                .onGloballyPositioned { coordinates ->
                    editorSize = coordinates.size
                }
        ) {
            val isPortrait = editorSize.height >= editorSize.width

            circleList.forEachIndexed { index, circle ->
                val offsetX = if (isPortrait)
                    (circle.xPercent * editorSize.width).roundToInt()
                else
                    (circle.yPercent * editorSize.width).roundToInt()

                val offsetY = if (isPortrait)
                    (circle.yPercent * editorSize.height).roundToInt()
                else
                    (circle.xPercent * editorSize.height).roundToInt()

                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                offsetX - (circleSizePx / 2),
                                offsetY - (circleSizePx / 2)
                            )
                        }
                        .size(64.dp)
                        .pointerInput(circle.key) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()

                                val deltaX: Float
                                val deltaY: Float

                                if (isPortrait) {
                                    deltaX = dragAmount.x / editorSize.width
                                    deltaY = dragAmount.y / editorSize.height
                                } else {
                                    deltaX = dragAmount.y / editorSize.height
                                    deltaY = dragAmount.x / editorSize.width
                                }

                                circleList = circleList.toMutableList().apply {
                                    this[index] = this[index].copy(
                                        xPercent = (this[index].xPercent + deltaX).coerceIn(0f, 1f),
                                        yPercent = (this[index].yPercent + deltaY).coerceIn(0f, 1f)
                                    )
                                }
                            }
                            detectTapGestures(
                                onLongPress = {
                                    circleList =
                                        circleList.toMutableList().apply { removeAt(index) }
                                }
                            )
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(Color.Cyan)
                    }
                    Text(
                        text = circle.key,
                        modifier = Modifier.align(Alignment.Center),
                        color = Color.Black
                    )
                }
            }
        }

        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text("Add Key Mapping") },
                text = {
                    Column {
                        Text("Enter key name:")
                        TextField(
                            value = newKeyName,
                            onValueChange = {
                                newKeyName = it
                                errorMessage = null
                            },
                            singleLine = true,
                            isError = errorMessage != null
                        )
                        if (errorMessage != null) {
                            Text(errorMessage!!, color = Color.Red)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val key = newKeyName.text.trim()
                        if (key.isEmpty()) {
                            errorMessage = "Key name cannot be empty"
                            return@TextButton
                        }
                        if (circleList.any { it.key == key }) {
                            errorMessage = "Key already exists"
                            return@TextButton
                        }

                        // Add to center
                        circleList = circleList + NormalizedCircle(
                            key = key,
                            xPercent = 0.5f,
                            yPercent = 0.5f
                        )
                        showAddDialog = false
                        newKeyName = TextFieldValue("")
                    }) {
                        Text("Add")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDialog = false }) {
                        Text(R.string.cancel)
                    }
                }
            )
        }
    }
}