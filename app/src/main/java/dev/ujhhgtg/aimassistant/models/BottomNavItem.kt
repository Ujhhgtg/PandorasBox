package dev.ujhhgtg.aimassistant.models

import androidx.compose.runtime.Composable

data class BottomNavItem(
    val label: Int,
    val unselectedIcon: @Composable () -> Unit,
    val selectedIcon: @Composable () -> Unit,
    val route: String
)
