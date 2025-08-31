package dev.ujhhgtg.pandorasbox.models

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

data class MenuItem(
    val icon: @Composable (Modifier) -> Unit,
    val title: String,
    val onClick: () -> Unit
)
