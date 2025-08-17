package dev.ujhhgtg.pandorasbox.ui.composables.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import dev.ujhhgtg.pandorasbox.R
import dev.ujhhgtg.pandorasbox.models.Module

@Composable
fun ModulesScreen(navController: NavController, modules: List<Module>, onAddShortcut: (String, Int, Int, String, Int) -> Unit) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(modules) { module ->
            ModuleCard(navController, module, onAddShortcut)
        }
    }
}

@Composable
fun ModuleCard(navController: NavController, module: Module, onAddShortcut: (String, Int, Int, String, Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    ListItem(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable {
                expanded = false
                navController.navigate(module.id) {
                    popUpTo("modules") {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            },
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        leadingContent = module.unselectedIcon,
        headlineContent = { Text(text = stringResource(module.label), style = MaterialTheme.typography.titleMedium) },
        supportingContent = { Text(text = stringResource(module.description), style = MaterialTheme.typography.bodyMedium) },
        trailingContent = {
            IconButton(onClick = { expanded = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.more))
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(
                    leadingIcon = { Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = stringResource(R.string.settings)
                    ) },
                    text = { Text(stringResource(R.string.settings)) },
                    onClick = {
                        expanded = false
                        // TODO: module settings
                    }
                )
                DropdownMenuItem(
                    leadingIcon = { Icon(
                        painter = painterResource(R.drawable.share_windows_24px),
                        contentDescription = stringResource(R.string.add_shortcut_to_launcher)
                    ) },
                    text = { Text(stringResource(R.string.add_shortcut_to_launcher)) },
                    onClick = {
                        expanded = false
                        onAddShortcut(module.id, module.label, module.description, "pb://${module.id}", R.mipmap.ic_launcher)
                    }
                )
            }
        }
    )
}