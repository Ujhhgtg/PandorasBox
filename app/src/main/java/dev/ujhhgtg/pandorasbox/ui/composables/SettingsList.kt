package dev.ujhhgtg.pandorasbox.ui.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.navigation.NavController
import androidx.navigation.NavDeepLink
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.ujhhgtg.pandorasbox.R
import dev.ujhhgtg.pandorasbox.models.SettingItem
import dev.ujhhgtg.pandorasbox.ui.activities.LocalActivityContext
import dev.ujhhgtg.pandorasbox.ui.activities.LocalPrefsRepository
import dev.ujhhgtg.pandorasbox.ui.activities.LocalSnackbarHostState
import dev.ujhhgtg.pandorasbox.utils.SnackbarUtils.showShort
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun SettingsList(
    items: List<SettingItem>,
    navController: NavController
) {
    val scope = rememberCoroutineScope()
    val settings = LocalPrefsRepository.current
    val snackbarHostState = LocalSnackbarHostState.current
    val ctx = LocalActivityContext.current
    val dataStore = settings.dataStore
    var inputDialogState by remember { mutableStateOf<Triple<SettingItem.Input, String, (String) -> Unit>?>(null) }
    var validationError by remember { mutableStateOf<String?>(null) }

    LazyColumn {
        items(items) { item ->
            when (item) {
                is SettingItem.Toggle -> {
                    var value by remember { mutableStateOf(item.defaultValue) }

                    LaunchedEffect(item.key) {
                        val key = booleanPreferencesKey(item.key)
                        value = dataStore.data.first()[key] ?: item.defaultValue
                    }

                    ListItem(
                        headlineContent = { Text(item.title) },
                        supportingContent = {
                            if (item.description.isNotEmpty()) Text(item.description)
                        },
                        leadingContent = item.icon,
                        trailingContent = {
                            Switch(
                                checked = value,
                                onCheckedChange = { newValue ->
                                    value = newValue
                                    val key = booleanPreferencesKey(item.key)
                                    scope.launch {
                                        dataStore.edit { prefs -> prefs[key] = newValue }
                                    }
                                }
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(onClick = {
                                value = !value
                                val key = booleanPreferencesKey(item.key)
                                scope.launch {
                                    dataStore.edit { prefs -> prefs[key] = value }
                                }
                            }, onLongClick = {
                                value = item.defaultValue
                                val key = booleanPreferencesKey(item.key)
                                scope.launch {
                                    dataStore.edit { prefs -> prefs[key] = item.defaultValue }
                                    snackbarHostState.showShort(ctx.getString(R.string.reset_setting_item), scope)
                                }
                            })
                    )
                }

                is SettingItem.Selection -> {
                    var expanded by remember { mutableStateOf(false) }
                    var value by remember { mutableIntStateOf(item.selectedIndex) }

                    LaunchedEffect(item.key) {
                        val key = intPreferencesKey(item.key)
                        value = dataStore.data.first()[key] ?: item.defaultIndex
                    }

                    ListItem(
                        headlineContent = { Text(item.title) },
                        supportingContent = {
                            if (item.description.isNotEmpty()) Text(item.description)
                        },
                        leadingContent = item.icon,
                        trailingContent = {
                            Box {
                                Text(
                                    text = item.options[value],
                                    modifier = Modifier
                                        .clickable { expanded = true }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                                DropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    item.options.forEachIndexed { index, option ->
                                        DropdownMenuItem(onClick = {
                                            value = index
                                            expanded = false
                                            val key = intPreferencesKey(item.key)
                                            scope.launch {
                                                dataStore.edit { prefs -> prefs[key] = index }
                                            }
                                        }, text = {
                                            Text(option)
                                        })
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                            .combinedClickable(onClick = { expanded = true },
                                onLongClick = {
                                    val key = intPreferencesKey(item.key)
                                    scope.launch {
                                        value = item.defaultIndex
                                        dataStore.edit { prefs -> prefs[key] = item.defaultIndex }
                                        snackbarHostState.showShort(ctx.getString(R.string.reset_setting_item), scope)
                                    } })
                    )
                }

                is SettingItem.Input -> {
                    var value by remember { mutableStateOf(item.defaultValue) }
                    LaunchedEffect(item.key) {
                        val key = stringPreferencesKey(item.key)
                        value = dataStore.data.first()[key] ?: item.defaultValue
                    }

                    ListItem(
                        headlineContent = { Text(item.label) },
                        supportingContent = {
                            if (item.description.isNotEmpty()) Text(item.description)
                        },
                        leadingContent = item.icon,
                        trailingContent = { Text(value, modifier = Modifier.clickable { inputDialogState = Triple(item, value) { value = it }
                            validationError = null }) },
                        modifier = Modifier.combinedClickable(onClick = {
                            inputDialogState = Triple(item, value) { value = it }
                            validationError = null
                        }, onLongClick = {
                            val key = stringPreferencesKey(item.key)
                            scope.launch {
                                value = item.defaultValue
                                dataStore.edit { prefs -> prefs[key] = item.defaultValue }
                                snackbarHostState.showShort(ctx.getString(R.string.reset_setting_item), scope)
                            }
                        })
                    )
                }

                is SettingItem.SubPage -> {
                    ListItem(
                        headlineContent = { Text(item.title) },
                        supportingContent = {
                            if (item.description.isNotEmpty()) Text(item.description)
                        },
                        leadingContent = item.icon,
                        trailingContent = { Icon(Icons.AutoMirrored.Default.ArrowForward, contentDescription = null) },
                        modifier = Modifier.clickable { navController.navigate(item.route) }
                    )
                }

                is SettingItem.Action -> {
                    ListItem(
                        headlineContent = { Text(item.title) },
                        supportingContent = {
                            if (item.description.isNotEmpty()) Text(item.description)
                        },
                        leadingContent = item.icon,
                        modifier = Modifier.clickable { item.onClick() }
                    )

                    item.content?.let { it() }
                }

                is SettingItem.CustomPage -> {

                }
            }
        }
    }

    inputDialogState?.let { (inputItem, currentValue, onValueChange) ->
        AlertDialog(
            onDismissRequest = { inputDialogState = null },
            title = { Text(inputItem.label) },
            text = {
                Column {
                    TextField(
                        value = currentValue,
                        onValueChange = {
                            onValueChange(it)
                            validationError = null
                        },
                        singleLine = true
                    )
                    if (inputItem.description.isNotEmpty()) {
                        Text(inputItem.description)
                    }
                    validationError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val isValid = inputItem.validator?.invoke(currentValue) ?: true
                    if (isValid) {
                        val key = stringPreferencesKey(inputItem.key)
                        scope.launch {
                            dataStore.edit { prefs -> prefs[key] = currentValue }
                        }
                        inputDialogState = null
                    } else {
                        validationError = inputItem.validationFailMessage
                    }
                }) {
                    Text(R.string.ok)
                }
            },
            dismissButton = {
                TextButton(onClick = { inputDialogState = null }) {
                    Text(R.string.cancel)
                }
            }
        )
    }
}


fun NavGraphBuilder.settingsGraph(
    navController: NavController,
    rootItems: List<SettingItem>,
    route: String,
    deepLinks: List<NavDeepLink> = emptyList()
) {
    composable(route, deepLinks = deepLinks) {
        SettingsList(rootItems, navController)
    }

    rootItems.filterIsInstance<SettingItem.SubPage>().forEach { sub ->
        composable(sub.route) {
            SettingsList(sub.children, navController)
        }
    }
}
