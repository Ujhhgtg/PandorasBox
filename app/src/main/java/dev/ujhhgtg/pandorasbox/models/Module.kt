package dev.ujhhgtg.pandorasbox.models

import androidx.compose.runtime.Composable

data class Module(
    val label: Int,
    val description: Int,
    val unselectedIcon: @Composable () -> Unit,
    val selectedIcon: @Composable () -> Unit,
    val id: String
)
