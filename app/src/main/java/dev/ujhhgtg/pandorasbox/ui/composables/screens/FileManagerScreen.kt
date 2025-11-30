package dev.ujhhgtg.pandorasbox.ui.composables.screens

import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import dev.ujhhgtg.pandorasbox.R
import dev.ujhhgtg.pandorasbox.models.OpenableItem
import dev.ujhhgtg.pandorasbox.ui.activities.LocalActivityContext
import dev.ujhhgtg.pandorasbox.ui.activities.LocalBottomBarSetter
import dev.ujhhgtg.pandorasbox.ui.activities.LocalNavController
import dev.ujhhgtg.pandorasbox.ui.activities.LocalScrollBehavior
import dev.ujhhgtg.pandorasbox.ui.activities.LocalTopBarSetter
import dev.ujhhgtg.pandorasbox.ui.activities.MainActivity
import dev.ujhhgtg.pandorasbox.ui.composables.ButtonSpacer
import dev.ujhhgtg.pandorasbox.ui.composables.Icon
import dev.ujhhgtg.pandorasbox.ui.composables.Text
import dev.ujhhgtg.pandorasbox.ui.composables.dialogs.InputDialog
import dev.ujhhgtg.pandorasbox.ui.composables.dialogs.OpenItemDialog
import dev.ujhhgtg.pandorasbox.ui.composables.dialogs.toPx
import dev.ujhhgtg.pandorasbox.utils.FileUtils
import dev.ujhhgtg.pandorasbox.utils.PermissionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileManagerScreen(rootPath: String) {
    val ctx = LocalActivityContext.current
    val scrollBehavior = LocalScrollBehavior.current

    LaunchedEffect(Unit) {
        if (!PermissionManager.checkExternalStorage()) {
            PermissionManager.checkAndRequestExternalStorage(ctx)
        }
    }

    var leftPath by rememberSaveable { mutableStateOf(File(rootPath)) }
    var rightPath by rememberSaveable { mutableStateOf(File(rootPath)) }
    var leftIsSelectionMode by remember { mutableStateOf(false) }
    var rightIsSelectionMode by remember { mutableStateOf(false) }
    val leftSelectedFiles = remember { mutableStateListOf<File>() }
    val rightSelectedFiles = remember { mutableStateListOf<File>() }
    var leftIsActive by remember { mutableStateOf(true) }
    val navController = LocalNavController.current
    val colors = MaterialTheme.colorScheme
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
        if (file.parentFile == target) {
            Toast.makeText(ctx, ctx.getString(R.string.source_target_are_same), Toast.LENGTH_SHORT).show()
            return
        }

        if (file.isDirectory) {
            file.copyRecursively(target.resolve(file.name), overwrite = true)
        } else {
            file.copyTo(target.resolve(file.name), overwrite = true)
        }
    }

    fun move(file: File, target: File) {
        if (file.parentFile == target) {
            Toast.makeText(ctx, ctx.getString(R.string.source_target_are_same), Toast.LENGTH_SHORT).show()
            return
        }

        Files.move(file.toPath(), target.toPath().resolve(file.name), StandardCopyOption.REPLACE_EXISTING)
    }

    fun curPath(): File {
        return if (leftIsActive)
            leftPath
        else
            rightPath
    }


    val setTopBar = LocalTopBarSetter.current
    LaunchedEffect(Unit) {
        setTopBar {
            var used by remember { mutableLongStateOf(0L) }
            var total by remember { mutableLongStateOf(0L) }
            var files by remember { mutableStateOf<Array<File>?>(null) }
            val haptic = LocalHapticFeedback.current

            LaunchedEffect(if (leftIsActive) leftPath else rightPath) {
                val (u, _, t) = FileUtils.getInternalStorageInfo()
                used = u
                total = t
                files = curPath().listFiles()
            }

            TopAppBar(
                colors = TopAppBarColors(
                    containerColor = colors.secondaryContainer,
                    scrolledContainerColor = colors.surfaceContainer,
                    navigationIconContentColor = colors.onSecondaryContainer,
                    titleContentColor = colors.onSecondaryContainer,
                    actionIconContentColor = colors.onSecondaryContainer,
                    subtitleContentColor = colors.onSecondaryContainer
                ),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack()
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }) {
                        Icon(Icons.AutoMirrored.Default.ArrowBack,
                            stringResource(R.string.back))
                    }
                },
                title = {
                    Column(
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.Center
                    ) {
                        var showEditPathDialog by remember { mutableStateOf(false) }
                        Text(text = curPath().absolutePath,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.clickable { showEditPathDialog = true })

                        if (showEditPathDialog) {
                            InputDialog(
                                title = { Text(R.string.edit_path) },
                                textFieldLabel = { Text(R.string.enter_a_path) },
                                value = curPath().absolutePath,
                                onValueChange = { if (leftIsActive) leftPath = File(it) else rightPath =
                                    File(it) },
                                onParseValue = { it },
                                onDismissRequest = { showEditPathDialog = false },
                                onValidate = { null }
                            )
                        }

                        Text(text =
                                "${stringResource(R.string.directory)}: ${files?.filter { it.isDirectory }?.size} " +
                                "${stringResource(R.string.file)}: ${files?.filter { !it.isDirectory }?.size} " +
                                "${stringResource(R.string.storage)}: ${FileUtils.formatBytes(ctx, used)}/${FileUtils.formatBytes(ctx, total)}",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                },
                actions = {
                    var showMenu by remember { mutableStateOf(false) }
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, R.string.menu)
                    }

                    DropdownMenu(expanded = showMenu,
                        onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            leadingIcon = { Icon(Icons.Default.Refresh, null) },
                            text = { Text(R.string.refresh) },
                            onClick = {
                                showMenu = false
                                scope.launch {
                                    files = curPath().listFiles()
                                }
                            }
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    }
    DisposableEffect(Unit) {
        onDispose { setTopBar(null) }
    }

    val setBottomBar = LocalBottomBarSetter.current
    LaunchedEffect(Unit) {
        setBottomBar {
            BottomAppBar(modifier = Modifier.height(48.dp)) {
                Row(Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically) {
                    var showCreateDialog by remember { mutableStateOf(false) }
                    IconButton(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Default.Add, R.string.create)
                    }
                    var fileName by remember { mutableStateOf("") }
                    var errorMessage by remember { mutableStateOf<String?>(null) }
                    if (showCreateDialog) {
                        AlertDialog(onDismissRequest = { showCreateDialog = false },
                            title = { Text(R.string.create) },
                            text = {
                                Column {
                                    OutlinedTextField(
                                        value = fileName,
                                        onValueChange = { fileName = it },
                                        label = { Text(R.string.enter_a_name) },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Number
                                        )
                                    )
                                    errorMessage?.let {
                                        Text(text = it, color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            },
                            dismissButton = { TextButton(onClick = {
                                if (fileName.isEmpty()) {
                                    errorMessage = ctx.getString(R.string.filename_must_not_be_empty)
                                    return@TextButton
                                }

                                if (curPath().resolve(fileName).exists()) {
                                    errorMessage = ctx.getString(R.string.file_already_exists)
                                    return@TextButton
                                }

                                curPath().resolve(fileName).createNewFile()
                                showCreateDialog = false
                                fileName = ""
                            }) { Text(R.string.file) } },
                            confirmButton = { TextButton(onClick = {
                                if (fileName.isEmpty()) {
                                    errorMessage = ctx.getString(R.string.filename_must_not_be_empty)
                                    return@TextButton
                                }

                                if (curPath().resolve(fileName).exists()) {
                                    errorMessage = ctx.getString(R.string.file_already_exists)
                                    return@TextButton
                                }

                                Files.createDirectory(curPath().resolve(fileName).toPath())
                                showCreateDialog = false
                                fileName = ""
                            }) { Text(R.string.directory) } })
                    }
                }
            }
        }
    }
    DisposableEffect(Unit) {
        onDispose { setBottomBar(null) }
    }

    BackHandler {
        if (leftIsActive) {
            if (leftIsSelectionMode) {
                leftIsSelectionMode = false; leftSelectedFiles.clear()
            } else {
                if (leftPath.parentFile != null) {
                    leftPath = leftPath.parentFile!!
                } else {
                    navController.popBackStack()
                }
            }
        } else {
            if (rightIsSelectionMode) {
                rightIsSelectionMode = false; rightSelectedFiles.clear()
            } else {
                if (rightPath.parentFile != null) {
                    rightPath = rightPath.parentFile!!
                } else {
                    navController.popBackStack()
                }
            }
        }
    }

    BoxWithConstraints(Modifier.fillMaxHeight()) {
        val totalWidth = constraints.maxWidth.toFloat()
        val totalHeight = constraints.maxHeight.toFloat()
        val shadowWidth = 6
        val shadowWidthDp = shadowWidth.dp
        val dividerX = if (leftIsActive) totalWidth / 2f else totalWidth / 2f - shadowWidthDp.toPx()

        Row(modifier = Modifier.fillMaxSize()) {
            FileManagerPane(
                path = leftPath,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                left = true,
                selectionMode = leftIsSelectionMode,
                selectedFiles = leftSelectedFiles,
                onEnterSelectionMode = { leftIsSelectionMode = true },
                onLeaveSelectionMode = { leftIsSelectionMode = false; leftSelectedFiles.clear() },
                onFocus = { leftIsActive = true },
                scope = scope,
                onNavigate = { leftPath = it },
                onRename = { f, n -> rename(f, n) },
                onDelete = { f -> delete(f) },
                onCopy = { f -> copy(f, rightPath) },
                onMove = { f -> move(f, rightPath) }
            )
            FileManagerPane(
                path = rightPath,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                left = false,
                selectionMode = rightIsSelectionMode,
                selectedFiles = rightSelectedFiles,
                onEnterSelectionMode = { rightIsSelectionMode = true },
                onLeaveSelectionMode = { rightIsSelectionMode = false; rightSelectedFiles.clear() },
                onFocus = { leftIsActive = false },
                scope = scope,
                onNavigate = { rightPath = it },
                onRename = { f, n -> rename(f, n) },
                onDelete = { f -> delete(f) },
                onCopy = { f -> copy(f, leftPath) },
                onMove = { f -> move(f, leftPath) }
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .height(shadowWidthDp)
                .offset { if (leftIsActive) IntOffset(dividerX.toInt(), 0) else IntOffset(0, 0) }
                .background(Brush.verticalGradient(colors = listOf(Color.Black.copy(alpha = 0.2f), Color.Transparent)))
        )
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(shadowWidthDp)
                .offset { IntOffset(dividerX.toInt(), 0) }
                .background(Brush.horizontalGradient(colors =
                    if (leftIsActive) listOf(Color.Black.copy(alpha = 0.2f), Color.Transparent)
                    else listOf(Color.Transparent, Color.Black.copy(alpha = 0.2f))))
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .height(shadowWidthDp)
                .offset { if (leftIsActive) IntOffset(0, (totalHeight - shadowWidth).toInt()) else IntOffset(dividerX.toInt(), (totalHeight - shadowWidth).toInt()) }
                .background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.2f))))
        )
    }

//    Row(Modifier.fillMaxWidth().height(48.dp),
//        horizontalArrangement = Arrangement.SpaceBetween,
//        verticalAlignment = Alignment.CenterVertically) {
//        var showCreateDialog by remember { mutableStateOf(false) }
//        IconButton(onClick = { showCreateDialog = true }) {
//            Icon(Icons.Default.Add, R.string.create)
//        }
//        var fileName by remember { mutableStateOf("") }
//        var errorMessage by remember { mutableStateOf<String?>(null) }
//        if (showCreateDialog) {
//            AlertDialog(onDismissRequest = { showCreateDialog = false },
//                title = { Text(R.string.create) },
//                text = {
//                    Column {
//                        OutlinedTextField(
//                            value = fileName,
//                            onValueChange = { fileName = it },
//                            label = { Text(R.string.enter_a_name) },
//                            singleLine = true,
//                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
//                        )
//                        errorMessage?.let {
//                            Text(text = it, color = MaterialTheme.colorScheme.error)
//                        }
//                    }
//                },
//                dismissButton = { TextButton(onClick = {
//                    if (fileName.isEmpty()) {
//                        errorMessage = ctx.getString(R.string.filename_must_not_be_empty)
//                        return@TextButton
//                    }
//                    if (curPath().resolve(fileName).exists()) {
//                        errorMessage = ctx.getString(R.string.file_already_exists)
//                        return@TextButton
//                    }
//                    curPath().resolve(fileName).createNewFile()
//                    showCreateDialog = false
//                    fileName = ""
//                }) { Text(R.string.file) } },
//                confirmButton = { TextButton(onClick = {
//                    if (fileName.isEmpty()) {
//                        errorMessage = ctx.getString(R.string.filename_must_not_be_empty)
//                        return@TextButton
//                    }
//                    if (curPath().resolve(fileName).exists()) {
//                        errorMessage = ctx.getString(R.string.file_already_exists)
//                        return@TextButton
//                    }
//                    Files.createDirectory(curPath().resolve(fileName).toPath())
//                    showCreateDialog = false
//                    fileName = ""
//                }) { Text(R.string.directory) } })
//        }
//    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileManagerPane(
    path: File,
    modifier: Modifier,
    left: Boolean,
    selectionMode: Boolean,
    selectedFiles: SnapshotStateList<File>,
    onEnterSelectionMode: () -> Unit,
    onLeaveSelectionMode: () -> Unit,
    onFocus: () -> Unit,
    scope: CoroutineScope,
    onNavigate: (File) -> Unit,
    onRename: (File, String) -> Unit,
    onDelete: (File) -> Unit,
    onCopy: (File) -> Unit,
    onMove: (File) -> Unit
) {
    fun getFiles(path: File): List<File> {
        return path.listFiles()?.sortedWith(compareBy({ !it.isDirectory }, { it.name })) ?: emptyList()
    }

    val pullToRefreshState = rememberPullToRefreshState()
    val colors = MaterialTheme.colorScheme
    val haptic = LocalHapticFeedback.current

    var isRefreshing by remember { mutableStateOf(false) }
    var files by remember { mutableStateOf(getFiles(path)) }

    val maxSwipePx = with(LocalDensity.current) { 40.dp.toPx() }

    fun toggleSelection(file: File) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        if (selectedFiles.contains(file)) {
            Log.d(MainActivity.TAG, "unselected ${file.name}")
            selectedFiles.remove(file)
        } else {
            Log.d(MainActivity.TAG, "selected ${file.name}")
            selectedFiles.add(file)
        }
        if (selectedFiles.isEmpty()) {
            Log.d(MainActivity.TAG, "left selection mode")
            onLeaveSelectionMode()
        }
    }

    PullToRefreshBox(
        modifier = modifier.background(colors.background),
        isRefreshing = isRefreshing,
        onRefresh = {
            isRefreshing = true
            scope.launch {
                files = getFiles(path)
                isRefreshing = false
            }
        },
        state = pullToRefreshState,
        indicator = {
            Indicator(
                modifier = Modifier.align(Alignment.TopCenter),
                isRefreshing = isRefreshing,
                containerColor = colors.secondaryContainer,
                color = colors.onSecondaryContainer,
                state = pullToRefreshState
            )
        },
    ) {
        Column(
            modifier = Modifier
                .background(colors.surface)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            if (event.changes.any { it.pressed }) {
                                onFocus()
                            }
                        }
                    }
                }
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                if (path.parentFile != null) {
                    item {
                        Text(
                            text = "..",
                            color = colors.onSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                    onLeaveSelectionMode()
                                    onNavigate(path.parentFile!!)
                                }
                                .padding(8.dp)
                        )
                    }
                }

                files = getFiles(path)
                items(files) { file ->
                    var menuExpanded by remember { mutableStateOf(false) }
                    var showOpenFileDialog by remember { mutableStateOf(false) }
                    var showRenameFileDialog by remember { mutableStateOf(false) }
                    val isSelected = file in selectedFiles
                    val swipeOffsetX = remember { Animatable(0f) }

                    Box {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .pointerInput(Unit) {
                                    detectHorizontalDragGestures(
                                        onHorizontalDrag = { change, dragAmount ->
                                            scope.launch {
                                                swipeOffsetX.snapTo(
                                                    (swipeOffsetX.value + dragAmount).coerceIn(
                                                        0f,
                                                        maxSwipePx
                                                    )
                                                )
                                                change.consume()
                                            }
                                        },
                                        onDragEnd = {
                                            scope.launch {
                                                if (swipeOffsetX.value > 40f) {
                                                    onEnterSelectionMode()
                                                    toggleSelection(file)
                                                }
                                                swipeOffsetX.animateTo(0f)
                                            }
                                        },
                                        onDragCancel = {
                                            scope.launch { swipeOffsetX.animateTo(0f) }
                                        }
                                    )
                                }
                                .offset { IntOffset(swipeOffsetX.value.toInt(), 0) }
                                .background(if (isSelected) colors.secondaryContainer else Color.Transparent)
                                .combinedClickable(
                                    onClick = {
                                        if (selectionMode) {
                                            toggleSelection(file)
                                            return@combinedClickable
                                        }

                                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                        if (file.isDirectory) {
                                            onNavigate(file)
                                        } else if (file.isFile) {
                                            showOpenFileDialog = true
                                        }
                                    },
                                    onLongClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        menuExpanded = true
                                    }
                                )
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (file.isDirectory) {
                                Icon(
                                    modifier = Modifier.size(20.dp),
                                    painter = R.drawable.folder_open_24px,
                                    contentDescription = stringResource(R.string.directory)
                                )
                            }
                            else if (file.isFile) {
                                Icon(
                                    modifier = Modifier.size(20.dp),
                                    painter = R.drawable.docs_24px,
                                    contentDescription = stringResource(R.string.file)
                                )
                            }
                            ButtonSpacer()
                            Text(
                                text = file.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.onSurface
                            )
                        }

                        FileDropdownMenu(
                            file,
                            menuExpanded,
                            left,
                            selectionMode,
                            selectedFiles,
                            { menuExpanded = false },
                            { onNavigate(it); onLeaveSelectionMode() },
                            { showOpenFileDialog = true },
                            { showRenameFileDialog = true },
                            { onDelete(it); files = getFiles(path) },
                            { onCopy(it); files = getFiles(path) },
                            { onMove(it); files = getFiles(path) }
                        )

                        if (showOpenFileDialog) {
                            OpenItemDialog(OpenableItem.FileItem(file)) { showOpenFileDialog = false }
                        } else if (showRenameFileDialog) {
                            RenameFileDialog(
                                file = file,
                                onDismiss = { showRenameFileDialog = false },
                                onRename = { f, n -> onRename(f, n); files = getFiles(path) }
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
    isLeft: Boolean,
    selectionMode: Boolean,
    selectedFiles: List<File>,
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
        if (!selectionMode || selectedFiles.size <= 1) {
            DropdownMenuItem(
                text = { Text(R.string.open) },
                leadingIcon = { Icon(R.drawable.file_open_24px, null) },
                onClick = {
                    if (file.isDirectory) onOpenDirectory(file)
                    else if (file.isFile) showOpenFileDialog()
                    onDismiss()
                }
            )

            DropdownMenuItem(
                leadingIcon = { Icon(Icons.Outlined.Edit, null) },
                text = { Text(R.string.rename) },
                onClick = {
                    onRename(file)
                    onDismiss()
                }
            )
        }


        DropdownMenuItem(
            leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
            text = { Text(R.string.delete) },
            onClick = {
                if (selectionMode) {
                    for (file in selectedFiles) {
                        onDelete(file)
                    }
                } else {
                    onDelete(file)
                }

                onDismiss()
            }
        )

        DropdownMenuItem(
            leadingIcon = { Icon(R.drawable.file_copy_24px, null) },
            text = { Text(if (isLeft) R.string.copy_to_right else R.string.copy_to_left) },
            onClick = {
                if (selectionMode) {
                    for (file in selectedFiles) {
                        onCopy(file)
                    }
                } else {
                    onCopy(file)
                }

                onDismiss()
            }
        )

        DropdownMenuItem(
            leadingIcon = { Icon(R.drawable.drive_file_move_24px, null) },
            text = { Text(if (isLeft) R.string.move_to_right else R.string.move_to_left) },
            onClick = {
                if (selectionMode) {
                    for (file in selectedFiles) {
                        onMove(file)
                    }
                } else {
                    onMove(file)
                }

                onDismiss()
            }
        )
    }
}

@Composable
private fun RenameFileDialog(
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
