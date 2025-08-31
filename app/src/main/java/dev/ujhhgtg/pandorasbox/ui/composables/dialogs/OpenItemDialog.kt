@file:Suppress("UnusedImport")

package dev.ujhhgtg.pandorasbox.ui.composables.dialogs

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import dev.ujhhgtg.pandorasbox.R
import dev.ujhhgtg.pandorasbox.enums.IntentAction
import dev.ujhhgtg.pandorasbox.utils.tooltip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.core.content.FileProvider
import dev.ujhhgtg.pandorasbox.models.OpenableItem
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toBitmap
import dev.ujhhgtg.pandorasbox.models.AppInfo
import dev.ujhhgtg.pandorasbox.ui.composables.LoadingIndicator

fun buildOpenIntent(
    item: OpenableItem,
    mode: IntentAction,
    packageName: String? = null,
    ctx: Context
): Intent {
    val intent = when (item) {
        is OpenableItem.FileItem -> {
            val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.provider", item.file)
            when (mode) {
                IntentAction.VIEW -> Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, ctx.contentResolver.getType(uri))
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                IntentAction.SEND -> Intent(Intent.ACTION_SEND).apply {
                    type = ctx.contentResolver.getType(uri) ?: "*/*"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                IntentAction.SEND_MULTIPLE -> Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                    type = ctx.contentResolver.getType(uri) ?: "*/*"
                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, arrayListOf(uri))
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }
        }

        is OpenableItem.UrlItem -> {
            val uri = item.url.toUri()
            when (mode) {
                IntentAction.VIEW -> Intent(Intent.ACTION_VIEW, uri)
                IntentAction.SEND -> Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, item.url)
                }
                IntentAction.SEND_MULTIPLE -> Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                    type = "text/plain"
                    putStringArrayListExtra(Intent.EXTRA_TEXT, arrayListOf(item.url))
                }
            }
        }
    }
    packageName?.let { intent.setPackage(it) }
    return intent
}

fun launchItemIntent(ctx: Context, item: OpenableItem, mode: IntentAction) {
    ctx.startActivity(Intent.createChooser(buildOpenIntent(item, mode, ctx = ctx), "Open with"))
}

fun launchItemIntentWithPackage(ctx: Context, item: OpenableItem, mode: IntentAction, packageName: String) {
    ctx.startActivity(buildOpenIntent(item, mode, packageName, ctx))
}

@Composable
fun OpenItemDialog(
    item: OpenableItem,
    onDismiss: () -> Unit
) {
    val ctx = LocalContext.current
    var openMode by remember { mutableStateOf(IntentAction.VIEW) }
    var apps by remember { mutableStateOf(listOf<AppInfo>()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(openMode) {
        isLoading = true
        withContext(Dispatchers.IO) {
            val pm = ctx.packageManager
            val launcherIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val launcherResolveInfos = pm.queryIntentActivities(launcherIntent, 0).associate { it.activityInfo.packageName to it.loadIcon(pm) }
            apps = pm.queryIntentActivities(buildOpenIntent(item, openMode, ctx = ctx), 0).map {
                val label = it.loadLabel(pm).toString()
                val packageName = it.activityInfo.packageName
                val icon = launcherResolveInfos.getOrDefault(packageName, null) ?: AppCompatResources.getDrawable(ctx, R.drawable.block_24px)!!
                val activity = it.activityInfo.targetActivity
                AppInfo(label, packageName, activity, icon)
            }.sortedBy { it.label.lowercase() }
            isLoading = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            DialogTitle(
                title = when(item) {
                    is OpenableItem.FileItem -> stringResource(R.string.open_file)
                    is OpenableItem.UrlItem -> stringResource(R.string.open_url)
                },
                onOpenClick = { launchItemIntent(ctx, item, openMode); onDismiss() }
            )
        },
        text = {
            Column {
                ModeSelector(openMode) { openMode = it }
                Spacer(Modifier.height(8.dp))
                SearchField(searchQuery) { searchQuery = it }
                Spacer(Modifier.height(8.dp))
                if (isLoading) {
                    LoadingIndicator()
                } else {
                    AppList(apps, searchQuery) { packageName ->
                        launchItemIntentWithPackage(ctx, item, openMode, packageName)
                        onDismiss()
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun DialogTitle(title: String, onOpenClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title)
        IconButton(onClick = onOpenClick) {
            Icon(
                painter = painterResource(R.drawable.share_windows_24px),
                contentDescription = stringResource(R.string.open_with_system_share),
                modifier = Modifier.tooltip(stringResource(R.string.open_with_system_share))
            )
        }
    }
}

@Composable
private fun ModeSelector(selectedMode: IntentAction, onModeSelected: (IntentAction) -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        IntentAction.entries.forEach { mode ->
            Text(
                text = mode.name,
                color = if (selectedMode == mode) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .clip(MaterialTheme.shapes.small)
                    .clickable { onModeSelected(mode) }
                    .padding(8.dp)
            )
        }
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        label = { dev.ujhhgtg.pandorasbox.ui.composables.Text(R.string.search) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search)
    )
}

@Composable
fun AppList(
    apps: List<AppInfo>,
    searchQuery: String,
    onAppClick: (String) -> Unit
) {
    val filteredApps = apps.filter {
        it.label.contains(searchQuery, ignoreCase = true)
                || it.packageName.contains(searchQuery, ignoreCase = true)
                || (it.activity?.contains(searchQuery, ignoreCase = true) ?: false)
    }

    LazyColumn(
        modifier = Modifier
            .heightIn(max = 400.dp)
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
                        if (app.activity != null) Text(app.activity, fontSize = 12.sp)
                    } },
                leadingContent = {
                    Icon(
                        bitmap = app.icon.toBitmap().asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.size(40.dp)
                    )
//                    ResolveInfoAppIcon(app)
                },
                modifier = Modifier.clickable { onAppClick(app.packageName) }
            )
        }
    }
}

fun Drawable.toBitmapSafe(sizePx: Int): Bitmap {
    val bitmap = createBitmap(sizePx, sizePx)
    val canvas = Canvas(bitmap)
    this.setBounds(0, 0, canvas.width, canvas.height)
    this.draw(canvas)
    return bitmap
}

@Composable
fun ResolveInfoAppIcon(app: AppInfo, size: Dp = 40.dp) {
    val sizePx = 40.dp.toPx()
    val iconBitmap = remember(app) { app.icon.toBitmapSafe(sizePx.toInt()) }
    Icon(
        bitmap = iconBitmap.asImageBitmap(),
        contentDescription = null,
        modifier = Modifier.size(size)
    )
}

@Composable
fun Dp.toPx(): Float = with(LocalDensity.current) { this@toPx.toPx() }
