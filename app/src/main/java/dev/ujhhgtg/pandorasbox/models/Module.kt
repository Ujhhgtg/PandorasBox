package dev.ujhhgtg.pandorasbox.models

import androidx.compose.runtime.Composable

data class Module(
    val label: Int,
    val description: Int,
    val icon: @Composable () -> Unit,
    val id: String
)
