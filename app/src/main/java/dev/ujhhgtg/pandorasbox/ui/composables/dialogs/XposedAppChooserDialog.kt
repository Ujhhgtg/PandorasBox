package dev.ujhhgtg.pandorasbox.ui.composables.dialogs

import android.content.pm.PackageManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.highcapable.yukihookapi.hook.factory.prefs
import dev.ujhhgtg.pandorasbox.R
import dev.ujhhgtg.pandorasbox.models.AppInfo
import dev.ujhhgtg.pandorasbox.ui.composables.widgets.LoadingIndicator
import dev.ujhhgtg.pandorasbox.ui.composables.widgets.Text
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun XposedAppChooserDialog(
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    val ctx = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var apps by remember { mutableStateOf(listOf<AppInfo>()) }
    var searchQuery by remember { mutableStateOf("") }
    var pkgToClearConfig by remember { mutableStateOf<String?>(null) }
    val scope by remember { mutableStateOf(CoroutineScope(Dispatchers.Main)) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val pm = ctx.packageManager
            val _apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            apps = _apps.mapNotNull { appInfo ->
                try {
                    AppInfo(
                        label = pm.getApplicationLabel(appInfo).toString(),
                        packageName = appInfo.packageName,
                        icon = pm.getApplicationIcon(appInfo),
                        activity = null
                    )
                } catch (_: PackageManager.NameNotFoundException) {
                    null
                }
            }.sortedBy { it.label.lowercase() }
            isLoading = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(R.string.select_app) },
        text = {
            if (isLoading) {
                LoadingIndicator()
            } else {
                Column {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text(R.string.search) },
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

                    LazyColumn(
                        modifier =
                            Modifier
                                .heightIn(max = 500.dp)
                                .clip(MaterialTheme.shapes.large)
                    ) {
                        item {
                            ListItem(
                                headlineContent = { Text(R.string.default_) },
                                leadingContent = { Icon(Icons.Default.Settings, null) },
                                trailingContent = {
                                    if (ctx.prefs("xposed")
                                            .all().keys.any { it.contains("default") }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "has config",
                                            tint = Color.Green,
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .clickable {
                                                    pkgToClearConfig = "default"
                                                }
                                        )
                                    } else {
                                        Icon(
                                            painter = painterResource(R.drawable.block_24px),
                                            contentDescription = "has no config",
                                            tint = Color.Red
                                        )
                                    }
                                },
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
                                    if (ctx.prefs("xposed")
                                            .all().keys.any { it.contains(app.packageName) }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "has config",
                                            tint = Color.Green,
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .clickable {
                                                    pkgToClearConfig = app.packageName
                                                }
                                        )
                                    } else {
                                        Icon(
                                            painter = painterResource(R.drawable.block_24px),
                                            contentDescription = "has no config",
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
            TextButton(onClick = onDismiss) { Text(R.string.cancel) }
        }
    )

    if (pkgToClearConfig != null) {
        AlertDialog(
            onDismissRequest = { pkgToClearConfig = null },
            title = { Text("Clear Config for App") },
            text = { Text("Are you sure you want to clear the config for this app?") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        ctx.prefs("xposed").edit {
                            assert(pkgToClearConfig != null)

                            remove("${pkgToClearConfig}_pbg_e")
                            remove("${pkgToClearConfig}_pbg_bl")
                            remove("${pkgToClearConfig}_sc_t")
                            remove("${pkgToClearConfig}_sc_fs")
                            remove("${pkgToClearConfig}_sc_ds")
                            remove("${pkgToClearConfig}_ss_e")
                        }
                        pkgToClearConfig = null
                    }
                }) { Text(R.string.ok) }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text(R.string.cancel) }
            }
        )
    }
}
