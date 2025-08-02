package dev.ujhhgtg.aimassistant.ui.composables

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import dev.ujhhgtg.aimassistant.utils.SettingsRepository
import dev.ujhhgtg.aimassistant.models.AppInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun PackageChooserDialog(
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    val context = LocalContext.current
    val settingsRepository by remember { mutableStateOf(SettingsRepository(context)) }
    var isLoading by remember { mutableStateOf(true) }
    var apps by remember { mutableStateOf(listOf<AppInfo>()) }
    var searchQuery by remember { mutableStateOf("") }
    var appToClearConfig by remember { mutableStateOf("") }
    var showClearConfigDialog by remember { mutableStateOf(false) }
    val coroutineScope by remember { mutableStateOf(CoroutineScope(Dispatchers.Main)) }
    val interactionSource = remember { MutableInteractionSource() }


    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val intent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = pm.queryIntentActivities(intent, 0)
            val loadedApps = resolveInfos.map {
                val label = it.loadLabel(pm).toString()
                val icon = it.loadIcon(pm)
                val packageName = it.activityInfo.packageName
                AppInfo(label, packageName, icon)
            }.sortedBy { it.label.lowercase() }
            apps = loadedApps
            isLoading = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select App") },
        text = {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                Column {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Search") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search)
                    )

                    val filteredApps = apps.filter {
                        it.label.contains(searchQuery, ignoreCase = true) ||
                                it.packageName.contains(searchQuery, ignoreCase = true)
                    }

                    LazyColumn(modifier =
                        Modifier
                            .heightIn(max = 500.dp)
                            .clip(RoundedCornerShape(16.dp))
                    ) {
                        item {
                            ListItem(
                                headlineContent = { Text("Default") },
                                leadingContent = { Icon(imageVector = Icons.Default.Settings, contentDescription = "default") },
                                modifier = Modifier.clickable {
                                    onSelect("default")
                                }
                            )
                        }

                        items(filteredApps) { app ->
                            ListItem(
                                headlineContent = { Text(app.label) },
                                supportingContent = {
                                    Text(app.packageName, fontSize = 12.sp)
                                },
                                leadingContent = {
                                    Image(
                                        bitmap = app.icon.toBitmap().asImageBitmap(),
                                        contentDescription = null,
                                        modifier = Modifier.size(40.dp)
                                    )
                                },
                                trailingContent = {
                                    if (settingsRepository.hasConfigOfPackage(app.packageName)) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "has config",
                                            tint = Color.Green,
                                            modifier = Modifier.clickable(
                                                interactionSource = interactionSource,
                                                indication = null
                                            ) {
                                                appToClearConfig = app.packageName
                                                showClearConfigDialog = true
                                            }
                                        )
                                    }
                                    else {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "no config",
                                            tint = Color.Red
                                        )
                                    }
                                },
                                modifier = Modifier.clickable {
                                    onSelect(app.packageName)
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )

    if (showClearConfigDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfigDialog = false },
            title = { Text("Clear Config for App" ) },
            text = { Text("Are you sure you want to clear the config for this app?") },
            confirmButton = {
                TextButton(onClick = { showClearConfigDialog = false } ) { Text("Cancel") }
                TextButton(onClick = {
                    coroutineScope.launch {
                        settingsRepository.removeSingleConfig(floatPreferencesKey("${appToClearConfig}_horizontal_offset"))
                        settingsRepository.removeSingleConfig(floatPreferencesKey("${appToClearConfig}_vertical_offset"))
                        settingsRepository.removeSingleConfig(intPreferencesKey("${appToClearConfig}_dot_size"))
                        settingsRepository.removeSingleConfig(intPreferencesKey("${appToClearConfig}_line_width"))
                    }
                    showClearConfigDialog = false
                } ) { Text("OK") }
            }
        )
    }
}
