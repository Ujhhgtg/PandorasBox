package dev.ujhhgtg.pandorasbox.ui.composables.screens

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import dev.ujhhgtg.pandorasbox.ui.activities.LocalScrollBehavior
import dev.ujhhgtg.pandorasbox.ui.composables.DefaultColumn


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val scrollBehavior = LocalScrollBehavior.current

    DefaultColumn(scrollBehavior) {

    }
}
