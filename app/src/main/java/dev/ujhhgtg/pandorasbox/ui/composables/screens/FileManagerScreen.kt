package dev.ujhhgtg.pandorasbox.ui.composables.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import dev.ujhhgtg.pandorasbox.R
import dev.ujhhgtg.pandorasbox.enums.FileOpenMode
import dev.ujhhgtg.pandorasbox.ui.composables.ButtonSpacer
import dev.ujhhgtg.pandorasbox.utils.PermissionManager
import dev.ujhhgtg.pandorasbox.utils.tooltip
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

@Composable
fun FileManagerScreen(ctx: Activity, rootPath: String) {
    LaunchedEffect(Unit) {
        if (!PermissionManager.checkExternalStorage()) {
            PermissionManager.checkAndRequestExternalStorage(ctx)
        }
    }

    var leftPath by rememberSaveable { mutableStateOf(File(rootPath)) }
    var rightPath by rememberSaveable { mutableStateOf(File(rootPath)) }

    val scope = rememberCoroutineScope()

    fun rename(file: File, newName: String) {
        val newFile = File(file.parentFile, newName)
        file.renameTo(newFile)
    }

    fun delete(file: File) {
        if (file.isDirectory) {
            file.deleteRecursively()
        } else {
            file.delete()
        }
    }

    fun copy(file: File, target: File) {
        if (file.isDirectory) {
            file.copyRecursively(target.resolve(file.name), overwrite = true)
        } else {
            file.copyTo(target.resolve(file.name), overwrite = true)
        }
    }

    fun move(file: File, target: File) {
        Files.move(file.toPath(), target.toPath().resolve(file.name), StandardCopyOption.REPLACE_EXISTING)
    }

    Row(modifier = Modifier.fillMaxSize()) {
        FileManagerColumn(
            path = leftPath,
            modifier = Modifier.weight(1f).fillMaxHeight(),
            isLeft = true,
            scope = scope,
            onOpenDirectory = { leftPath = it },
            onRename = { f, n -> rename(f, n) },
            onDelete = { f -> delete(f) },
            onCopy = { f -> copy(f, rightPath) },
            onMove = { f -> move(f, rightPath) }
        )
        FileManagerColumn(
            path = rightPath,
            modifier = Modifier.weight(1f).fillMaxHeight(),
            isLeft = false,
            scope = scope,
            onOpenDirectory = { rightPath = it },
            onRename = { f, n -> rename(f, n) },
            onDelete = { f -> delete(f) },
            onCopy = { f -> copy(f, leftPath) },
            onMove = { f -> move(f, leftPath) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileManagerColumn(
    path: File,
    modifier: Modifier,
    isLeft: Boolean,
    scope: CoroutineScope,
    onOpenDirectory: (File) -> Unit,
    onRename: (File, String) -> Unit,
    onDelete: (File) -> Unit,
    onCopy: (File) -> Unit,
    onMove: (File) -> Unit
) {
    fun getFiles(): List<File> {
        return path.listFiles()?.sortedWith(compareBy({ !it.isDirectory }, { it.name })) ?: emptyList()
    }

    var isRefreshing by remember { mutableStateOf(false) }
    var files by remember { mutableStateOf(getFiles()) }
    val state = rememberPullToRefreshState()

    PullToRefreshBox(
        modifier = modifier.background(MaterialTheme.colorScheme.background),
        isRefreshing = isRefreshing,
        onRefresh = {
            isRefreshing = true
            scope.launch {
                files = getFiles()
                isRefreshing = false
            }
        },
        state = state,
        indicator = {
            Indicator(
                modifier = Modifier.align(Alignment.TopCenter),
                isRefreshing = isRefreshing,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                state = state
            )
        },
    ) {
        Column {
            Text(
                text = path.absolutePath,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(8.dp)
            )

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                if (path.parentFile != null) {
                    item {
                        Text(
                            text = "..",
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenDirectory(path.parentFile!!) }
                                .padding(8.dp)
                        )
                    }
                }

                files = getFiles()
                items(files) { file ->
                    var menuExpanded by remember { mutableStateOf(false) }
                    var showOpenFileDialog by remember { mutableStateOf(false) }
                    var showRenameFileDialog by remember { mutableStateOf(false) }

                    Box {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = {
                                        if (file.isDirectory) {
                                            onOpenDirectory(file)
                                        }
                                        else if (file.isFile) {
                                            showOpenFileDialog = true
                                        }
                                    },
                                    onLongClick = {
                                        menuExpanded = true
                                    }
                                )
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (file.isDirectory) {
                                Icon(
                                    modifier = Modifier.size(20.dp),
                                    painter = painterResource(R.drawable.folder_open_24px),
                                    contentDescription = stringResource(R.string.directory)
                                )
                            }
                            else if (file.isFile) {
                                Icon(
                                    modifier = Modifier.size(20.dp),
                                    painter = painterResource(R.drawable.docs_24px),
                                    contentDescription = stringResource(R.string.file)
                                )
                            }
                            ButtonSpacer()
                            Text(
                                text = file.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        FileDropdownMenu(
                            file,
                            menuExpanded,
                            isLeft,
                            { menuExpanded = false },
                            { onOpenDirectory(it) },
                            { showOpenFileDialog = true },
                            { showRenameFileDialog = true },
                            { onDelete(it); files = getFiles() },
                            { onCopy(it); files = getFiles() },
                            { onMove(it); files = getFiles() }
                        )

                        if (showOpenFileDialog) {
                            OpenFileDialog(
                                file = file,
                                onDismiss = { showOpenFileDialog = false }
                            )
                        } else if (showRenameFileDialog) {
                            RenameFileDialog(
                                file = file,
                                onDismiss = { showRenameFileDialog = false },
                                onRename = { f, n -> onRename(f, n); files = getFiles() }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FileDropdownMenu(
    file: File,
    menuExpanded: Boolean,
    isFirst: Boolean,
    onDismiss: () -> Unit,
    onOpenDirectory: (File) -> Unit,
    showOpenFileDialog: () -> Unit,
    onRename: (File) -> Unit,
    onDelete: (File) -> Unit,
    onCopy: (File) -> Unit,
    onMove: (File) -> Unit
) {
    DropdownMenu(
        expanded = menuExpanded,
        onDismissRequest = onDismiss
    ) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.open)) },
            leadingIcon = { Icon(painter = painterResource(R.drawable.file_open_24px), contentDescription = "Open") },
            onClick = {
                if (file.isDirectory) onOpenDirectory(file)
                else if (file.isFile) showOpenFileDialog()
                onDismiss()
            }
        )

        DropdownMenuItem(
            leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
            text = { Text(stringResource(R.string.rename)) },
            onClick = {
                onRename(file)
                onDismiss()
            }
        )

        DropdownMenuItem(
            leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
            text = { Text(stringResource(R.string.delete)) },
            onClick = {
                onDelete(file)
                onDismiss()
            }
        )

        DropdownMenuItem(
            leadingIcon = { Icon(painter = painterResource(R.drawable.file_copy_24px), contentDescription = null) },
            text = { Text(stringResource(if (isFirst) R.string.copy_to_right else R.string.copy_to_left)) },
            onClick = {
                onCopy(file)
                onDismiss()
            }
        )

        DropdownMenuItem(
            leadingIcon = { Icon(painter = painterResource(R.drawable.drive_file_move_24px), contentDescription = null) },
            text = { Text(stringResource(if (isFirst) R.string.move_to_right else R.string.move_to_left)) },
            onClick = {
                onMove(file)
                onDismiss()
            }
        )
    }
}


@Composable
fun OpenFileDialog(
    file: File,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var openMode by remember { mutableStateOf(FileOpenMode.VIEW) }
    var apps by remember { mutableStateOf(listOf<ResolveInfo>()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }

    val pm = context.packageManager

    LaunchedEffect(file, openMode) {
        isLoading = true
        withContext(Dispatchers.IO) {
            apps = getAppsForFile(context, file, openMode)
            isLoading = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(stringResource(R.string.open_file))
                IconButton(onClick = {
                    launchFileIntent(context, file, openMode)
                    onDismiss()
                }) {
                    Icon(painter = painterResource(R.drawable.share_windows_24px),
                        contentDescription = stringResource(R.string.open_file_with_system_share),
                        modifier = Modifier.tooltip(stringResource(R.string.open_file_with_system_share)))
                }
            }
        },
        text = {
            Column {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    FileOpenMode.entries.forEach { mode ->
                        Text(
                            text = mode.name,
                            color = if (openMode == mode) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier
                                .clickable { openMode = mode }
                                .padding(8.dp)
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text(stringResource(R.string.search)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search)
                )

                Spacer(Modifier.height(8.dp))

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    val filteredApps = apps.filter {
                        it.loadLabel(pm).toString().contains(searchQuery, ignoreCase = true)
                                || it.activityInfo.packageName.contains(searchQuery, ignoreCase = true)
                    }

                    LazyColumn(
                        modifier = Modifier
                            .heightIn(max = 400.dp)
                            .clip(RoundedCornerShape(16.dp))
                    ) {
                        items(filteredApps) { app ->
                            ListItem(
                                headlineContent = { Text(app.loadLabel(pm).toString()) },
                                supportingContent = { Text(app.activityInfo.packageName, fontSize = 12.sp) },
                                leadingContent = {
                                    Icon(
                                        bitmap = app.loadIcon(pm).toBitmap().asImageBitmap(),
                                        contentDescription = null,
                                        modifier = Modifier.size(40.dp)
                                    )
                                },
                                modifier = Modifier.clickable {
                                    launchFileIntentWithPackage(context, file, openMode, app.activityInfo.packageName)
                                    onDismiss()
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
}

fun getAppsForFile(ctx: Context, file: File, mode: FileOpenMode): List<ResolveInfo> {
    val uri: Uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.provider", file)
    val intent = when (mode) {
        FileOpenMode.VIEW -> Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, ctx.contentResolver.getType(uri))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        FileOpenMode.SEND -> Intent(Intent.ACTION_SEND).apply {
            type = ctx.contentResolver.getType(uri) ?: "*/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        FileOpenMode.SEND_MULTIPLE -> Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = ctx.contentResolver.getType(uri) ?: "*/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, arrayListOf(uri))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    return ctx.packageManager.queryIntentActivities(intent, 0)
}

fun launchFileIntent(ctx: Context, file: File, mode: FileOpenMode) {
    val uri = FileProvider.getUriForFile(
        ctx,
        "${ctx.packageName}.provider",
        file
    )
    val intent = when (mode) {
        FileOpenMode.VIEW -> Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, ctx.contentResolver.getType(uri))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        FileOpenMode.SEND -> Intent(Intent.ACTION_SEND).apply {
            type = ctx.contentResolver.getType(uri) ?: "*/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        FileOpenMode.SEND_MULTIPLE -> Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = ctx.contentResolver.getType(uri) ?: "*/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, arrayListOf(uri))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
    ctx.startActivity(
        Intent.createChooser(intent, "Open with")
    )
}

fun launchFileIntentWithPackage(ctx: Context, file: File, mode: FileOpenMode, packageName: String) {
    val uri: Uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.provider", file)
    val intent = when (mode) {
        FileOpenMode.VIEW -> Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, ctx.contentResolver.getType(uri))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        FileOpenMode.SEND -> Intent(Intent.ACTION_SEND).apply {
            type = ctx.contentResolver.getType(uri) ?: "*/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        FileOpenMode.SEND_MULTIPLE -> Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = ctx.contentResolver.getType(uri) ?: "*/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, arrayListOf(uri))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
    intent.setPackage(packageName)
    ctx.startActivity(intent)
}

@Composable
fun RenameFileDialog(
    file: File,
    onDismiss: () -> Unit,
    onRename: (File, String) -> Unit
) {
    var newName by remember { mutableStateOf(file.name) }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename File") },
        text = {
            Column {
                OutlinedTextField(
                    value = newName,
                    onValueChange = {
                        newName = it
                        isError = it.isBlank() || it.contains("/")
                    },
                    label = { Text("New Name") },
                    isError = isError,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (isError) {
                    Text(
                        text = "Invalid name",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (!isError && newName != file.name) {
                        onRename(file, newName)
                        onDismiss()
                    }
                }
            ) {
                Text("Rename")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

