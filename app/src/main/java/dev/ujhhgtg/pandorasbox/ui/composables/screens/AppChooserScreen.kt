package dev.ujhhgtg.pandorasbox.ui.composables.screens

import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import dev.ujhhgtg.pandorasbox.R
import dev.ujhhgtg.pandorasbox.models.AppInfo
import dev.ujhhgtg.pandorasbox.ui.composables.widgets.LoadingIndicator
import dev.ujhhgtg.pandorasbox.ui.composables.widgets.Text
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG: String = "PB.PackageManager"

@Composable
fun AppChooserScreen(
    onDismiss: () -> Unit,
    onSelect: (AppInfo) -> Unit,
    withActivities: Boolean
) {
    val ctx = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var apps by remember { mutableStateOf(listOf<AppInfo>()) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            if (!withActivities) {
                val pm = ctx.packageManager
                val intent = Intent(Intent.ACTION_MAIN, null).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                }
                val resolveInfos = pm.queryIntentActivities(intent, 0)
                apps = resolveInfos.map {
                    val label = it.loadLabel(pm).toString()
                    val icon = it.loadIcon(pm)
                    val packageName = it.activityInfo.packageName
                    val activity = it.activityInfo.name
                    AppInfo(label, packageName, activity, icon)
                }.sortedBy { it.label.lowercase() }
            } else {
                val _apps = mutableListOf<AppInfo>()
                val pm = ctx.packageManager
                for (app in pm.getInstalledPackages(PackageManager.GET_ACTIVITIES)) {
                    if (app.activities == null) {
                        Log.w(TAG, "cannot get activities of ${app.packageName}, skipping")
                        continue
                    }

                    for (act in app.activities) {
                        _apps.add(
                            AppInfo(
                                act.loadLabel(pm).toString(),
                                app.packageName,
                                act.name,
                                act.loadIcon(pm)
                            )
                        )
                    }
                }
                apps = _apps
            }
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
                        it.label.contains(searchQuery, ignoreCase = true)
                                || it.packageName.contains(searchQuery, ignoreCase = true)
                                || (it.activity?.contains(searchQuery, ignoreCase = true) ?: false)
                    }

                    LazyColumn(
                        modifier =
                            Modifier
                                .heightIn(max = 500.dp)
                                .clip(MaterialTheme.shapes.large)
                    ) {
                        items(filteredApps) { app ->
                            ListItem(
                                headlineContent = { Text(app.label) },
                                supportingContent = {
                                    Column(
                                        Modifier.fillMaxWidth(),
                                        horizontalAlignment = Alignment.Start,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(app.packageName, fontSize = 12.sp)
                                        if (withActivities) Text(
                                            app.activity ?: "null",
                                            fontSize = 12.sp
                                        )
                                    }
                                },
                                leadingContent = {
                                    Image(
                                        bitmap = app.icon.toBitmap().asImageBitmap(),
                                        contentDescription = null,
                                        modifier = Modifier.size(40.dp)
                                    )
                                },
                                modifier = Modifier.clickable {
                                    onSelect(app)
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
}
