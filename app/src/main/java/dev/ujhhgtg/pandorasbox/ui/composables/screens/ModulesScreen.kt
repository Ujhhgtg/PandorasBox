package dev.ujhhgtg.pandorasbox.ui.composables.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import dev.ujhhgtg.pandorasbox.R
import dev.ujhhgtg.pandorasbox.models.Module
import dev.ujhhgtg.pandorasbox.ui.activities.LocalActivityContext
import dev.ujhhgtg.pandorasbox.ui.composables.Text
import dev.ujhhgtg.pandorasbox.utils.tooltip

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
    val haptic = LocalHapticFeedback.current
    val ctx = LocalActivityContext.current

    ListItem(
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .clickable {
                expanded = false
                navController.navigate(module.id)
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            },
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        leadingContent = module.unselectedIcon,
        headlineContent = { Text(text = stringResource(module.label), style = MaterialTheme.typography.titleMedium) },
        supportingContent = { Text(text = stringResource(module.description), style = MaterialTheme.typography.bodyMedium) },
        trailingContent = {
            IconButton(onClick = { expanded = true }) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = stringResource(R.string.more),
                    modifier = Modifier.tooltip(stringResource(R.string.more))
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                if (module.id != "settings") {
                    DropdownMenuItem(
                        leadingIcon = { Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = null
                        ) },
                        text = { Text(R.string.settings) },
                        onClick = {
                            expanded = false
                            navController.navigate("${module.id}_settings")
                        }
                    )
                }
                DropdownMenuItem(
                    leadingIcon = { Icon(
                        painter = painterResource(R.drawable.share_windows_24px),
                        contentDescription = stringResource(R.string.add_shortcut_to_launcher)
                    ) },
                    text = { Text(R.string.add_shortcut_to_launcher) },
                    onClick = {
                        expanded = false
                        onAddShortcut(module.id, module.label, module.description, "pb://${module.id}", R.mipmap.ic_launcher)
                    }
                )
            }
        }
    )
}