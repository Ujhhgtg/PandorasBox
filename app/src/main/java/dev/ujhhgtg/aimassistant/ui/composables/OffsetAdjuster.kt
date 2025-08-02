package dev.ujhhgtg.aimassistant.ui.composables

import androidx.compose.runtime.Composable

@Composable
fun OffsetAdjuster(
    label: String,
    value: Float,
    onChange: (Float) -> Unit
) {
    NumberAdjuster(
        "$label: %.3f",
        value,
        0f,
        -1f,
        1f,
        0.01f,
        onChange
    )
}