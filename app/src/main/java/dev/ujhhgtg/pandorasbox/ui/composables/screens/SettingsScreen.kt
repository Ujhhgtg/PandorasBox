package dev.ujhhgtg.pandorasbox.ui.composables.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import dev.ujhhgtg.pandorasbox.ui.composables.DefaultColumn
import dev.ujhhgtg.pandorasbox.utils.SettingsRepository


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    padding: PaddingValues,
    settings: SettingsRepository,
    scrollBehavior: TopAppBarScrollBehavior
) {
    DefaultColumn(scrollBehavior) {

    }
}
