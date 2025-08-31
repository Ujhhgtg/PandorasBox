package dev.ujhhgtg.pandorasbox.ui.composables

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min

@Composable
fun ColorChooserDialog(
    initialColor: Color = Color.Red,
    onDismiss: () -> Unit,
    onColorSelected: (Color) -> Unit
) {
    var color by remember { mutableStateOf(initialColor) }
    var alpha by remember { mutableFloatStateOf(initialColor.alpha) }
    var hue by remember { mutableFloatStateOf(initialColor.toHsv().first) }
    var saturation by remember { mutableFloatStateOf(initialColor.toHsv().second) }
    var value by remember { mutableFloatStateOf(initialColor.toHsv().third) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onColorSelected(color.copy(alpha = alpha)) }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        title = { Text("Choose Color") },
        text = {
            Column {
                // Color preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .background(color.copy(alpha = alpha), shape = CircleShape)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Saturation/Value Rectangle
                SVPicker(
                    hue = hue,
                    saturation = saturation,
                    value = value
                ) { s, v ->
                    saturation = s
                    value = v
                    color = Color.hsv(hue, s, v)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Hue Slider
                HueSlider(hue) { newHue ->
                    hue = newHue
                    color = Color.hsv(hue, saturation, value)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Alpha Slider
                AlphaSlider(alpha = alpha, color = color) { newAlpha ->
                    alpha = newAlpha
                }
            }
        }
    )
}

// --- Saturation / Value Picker ---
@Composable
fun SVPicker(
    hue: Float,
    saturation: Float,
    value: Float,
    onChange: (saturation: Float, value: Float) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(Color.Gray)
            .pointerInput(Unit) {
                detectTapGestures { pos ->
                    val s = (pos.x / size.width).coerceIn(0f, 1f)
                    val v = 1f - (pos.y / size.height).coerceIn(0f, 1f)
                    onChange(s, v)
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, _ ->
                    val s = (change.position.x / size.width).coerceIn(0f, 1f)
                    val v = 1f - (change.position.y / size.height).coerceIn(0f, 1f)
                    onChange(s, v)
                }
            }
            .pointerInput(Unit) {
                detectVerticalDragGestures { change, _ ->
                    val s = (change.position.x / size.width).coerceIn(0f, 1f)
                    val v = 1f - (change.position.y / size.height).coerceIn(0f, 1f)
                    onChange(s, v)
                }
            }
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val width = size.width
            val height = size.height

            val hueColor = Color.hsv(hue, 1f, 1f)

            // Horizontal gradient (saturation)
            val horizontal = Brush.horizontalGradient(
                colors = listOf(Color.White, hueColor)
            )

            // Vertical gradient (value)
            val vertical = Brush.verticalGradient(
                colors = listOf(Color.Transparent, Color.Black)
            )

            drawRect(horizontal)
            drawRect(vertical)

            // Draw selector dot
            drawCircle(
                color = Color.White,
                radius = 10.dp.toPx(),
                center = Offset(saturation * width, (1 - value) * height),
                style = Stroke(width = 2.dp.toPx())
            )
        }
    }
}

// --- Hue Slider ---
@Composable
fun HueSlider(hue: Float, onChange: (Float) -> Unit) {
    // Initialize selectorX based on hue
    var selectorX by remember { mutableFloatStateOf(hue / 360f * 1f) } // fraction of width

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(30.dp)
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, _ ->
                    val x = change.position.x.coerceIn(0f, size.width.toFloat())
                    val newHue = (x / size.width) * 360f
                    onChange(newHue)
                    selectorX = x / size.width
                }
            }
            .pointerInput(Unit) {
                detectTapGestures { pos ->
                    val x = pos.x.coerceIn(0f, size.width.toFloat())
                    val newHue = (x / size.width) * 360f
                    onChange(newHue)
                    selectorX = x / size.width
                }
            }
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val width = size.width
            val hueColors = (0..360 step 60).map { Color.hsv(it.toFloat(), 1f, 1f) }
            drawRect(Brush.horizontalGradient(hueColors))

            drawCircle(
                color = Color.White,
                radius = 10.dp.toPx(),
                center = Offset(selectorX * width, size.height / 2),
                style = Stroke(width = 2.dp.toPx())
            )
        }
    }
}


// --- Alpha Slider ---
@Composable
fun AlphaSlider(alpha: Float, color: Color, onChange: (Float) -> Unit) {
    // Initialize selectorX based on alpha
    var selectorX by remember { mutableFloatStateOf(alpha) } // fraction of width

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(30.dp)
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, _ ->
                    val x = change.position.x.coerceIn(0f, size.width.toFloat())
                    val newAlpha = (x / size.width)
                    onChange(newAlpha)
                    selectorX = x / size.width
                }
            }
            .pointerInput(Unit) {
                detectTapGestures { pos ->
                    val x = pos.x.coerceIn(0f, size.width.toFloat())
                    val newAlpha = (x / size.width)
                    onChange(newAlpha)
                    selectorX = x / size.width
                }
            }
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val width = size.width
            drawRect(
                Brush.horizontalGradient(
                    listOf(color.copy(alpha = 0f), color.copy(alpha = 1f))
                )
            )

            drawCircle(
                color = Color.White,
                radius = 10.dp.toPx(),
                center = Offset(selectorX * width, size.height / 2),
                style = Stroke(width = 2.dp.toPx())
            )
        }
    }
}

// --- Helper ---
fun Color.toHsv(): Triple<Float, Float, Float> {
    val r = red
    val g = green
    val b = blue
    val max = max(r, max(g, b))
    val min = min(r, min(g, b))
    val delta = max - min

    val h = when {
        delta == 0f -> 0f
        max == r -> ((g - b) / delta) % 6
        max == g -> ((b - r) / delta) + 2
        else -> ((r - g) / delta) + 4
    } * 60f

    val s = if (max == 0f) 0f else delta / max
    val v = max

    return Triple(if (h < 0) h + 360f else h, s, v)
}
