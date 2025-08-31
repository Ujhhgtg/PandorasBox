package dev.ujhhgtg.pandorasbox.ui.composables.screens

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Environment
import android.os.Message
import android.util.Log
import android.webkit.CookieManager
import android.webkit.GeolocationPermissions
import android.webkit.JsPromptResult
import android.webkit.JsResult
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.appcompat.app.AlertDialog
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import dev.ujhhgtg.pandorasbox.R
import dev.ujhhgtg.pandorasbox.models.BrowserTab
import dev.ujhhgtg.pandorasbox.models.DEFAULT_HOME_URL
import dev.ujhhgtg.pandorasbox.models.DEFAULT_SEARCH_ENGINE
import dev.ujhhgtg.pandorasbox.models.MenuItem
import dev.ujhhgtg.pandorasbox.models.OpenableItem
import dev.ujhhgtg.pandorasbox.services.DownloadService
import dev.ujhhgtg.pandorasbox.ui.activities.LocalActivityContext
import dev.ujhhgtg.pandorasbox.ui.activities.LocalHistoryRepository
import dev.ujhhgtg.pandorasbox.ui.activities.LocalNavController
import dev.ujhhgtg.pandorasbox.ui.activities.LocalSnackbarHostState
import dev.ujhhgtg.pandorasbox.ui.activities.LocalTopBarSetter
import dev.ujhhgtg.pandorasbox.ui.composables.ButtonSpacer
import dev.ujhhgtg.pandorasbox.ui.composables.IconButton
import dev.ujhhgtg.pandorasbox.ui.composables.Text
import dev.ujhhgtg.pandorasbox.ui.composables.dialogs.OpenItemDialog
import dev.ujhhgtg.pandorasbox.utils.ClipboardBridge
import dev.ujhhgtg.pandorasbox.utils.PackageUtils
import dev.ujhhgtg.pandorasbox.utils.ServiceLocator
import dev.ujhhgtg.pandorasbox.utils.SnackbarUtils.showShort
import dev.ujhhgtg.pandorasbox.utils.UriUtils.isUri
import dev.ujhhgtg.pandorasbox.utils.UriUtils.withoutScheme
import dev.ujhhgtg.pandorasbox.utils.settings.HistoryRepository
import dev.ujhhgtg.pandorasbox.utils.tooltip
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private const val TAG: String = "PB.BrowserScreen"

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen() {
    val haptic = LocalHapticFeedback.current
    val navController = LocalNavController.current
    val snackbarHostState = LocalSnackbarHostState.current
    val setTopBar = LocalTopBarSetter.current
    val clipboard = LocalClipboard.current
    val history = LocalHistoryRepository.current
    val scope = rememberCoroutineScope()

    val ctx = LocalActivityContext.current
    val colors = MaterialTheme.colorScheme
    var tabs by remember { mutableStateOf(listOf(createTab(ctx, snackbarHostState, clipboard, scope, history, "file:///android_asset/home.html"))) }
    var activeTabId by remember { mutableStateOf(tabs.first().id) }
    val activeTab = tabs.find { it.id == activeTabId }

    LaunchedEffect(Unit) {
        setTopBar {
            TopAppBar(
                navigationIcon = {
                    Row {
                        IconButton(onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (activeTab?.webView?.canGoBack() == true) {
                                activeTab.webView.goBack()
                            }
                            else {
                                navController.popBackStack()
                            }
                        }) {
                            Icon(imageVector = Icons.AutoMirrored.Default.ArrowBack,
                                contentDescription = stringResource(R.string.back),
                                modifier = Modifier.tooltip(stringResource(R.string.back)))
                        }
                        IconButton(onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (activeTab?.webView?.canGoForward() == true) {
                                activeTab.webView.goForward()
                            }
                        }) {
                            Icon(imageVector = Icons.AutoMirrored.Default.ArrowForward,
                                contentDescription = stringResource(R.string.forward),
                                modifier = Modifier.tooltip(stringResource(R.string.forward)))
                        }
                    }
                },
                title = { Text(R.string.browser) }
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose { setTopBar(null) }
    }

    BackHandler {
        if (activeTab?.webView?.canGoBack() == true) {
            activeTab.webView.goBack()
        }
        else {
            navController.popBackStack()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        tabs.find { it.id == activeTabId }?.let { tab ->
            key(tab.id) {
//                var drag by remember { mutableFloatStateOf(0f) }
//                var show by remember { mutableStateOf(false) }

//                val density = LocalDensity.current
//                val revealThreshold = with(density) { 12.dp.toPx() }
//                val navigateThreshold = with(density) { 96.dp.toPx() }
//                val maxShift = with(density) { 56.dp.toPx() }

                Box(Modifier.weight(1f)) {
                    AndroidView(
                        factory = { tab.webView },
                        modifier = Modifier
                            .fillMaxSize()
                        // FIXME: drag gestures conflicting with website dragging

//                            .pointerInput(Unit) {
//                                detectHorizontalDragGestures(
//                                    onDragStart = {
//                                        drag = 0f
//                                        show = false
//                                    },
//                                    onHorizontalDrag = { _, delta ->
//                                        drag += delta
//                                        val canBack = tab.webView.canGoBack()
//                                        val canFwd  = tab.webView.canGoForward()
//
//                                        show = when {
//                                            drag > 0f  -> canBack && drag > revealThreshold
//                                            drag < 0f  -> canFwd && -drag > revealThreshold
//                                            else       -> false
//                                        }
//                                    },
//                                    onDragEnd = {
//                                        val canBack = tab.webView.canGoBack()
//                                        val canFwd  = tab.webView.canGoForward()
//
//                                        if (drag > navigateThreshold && canBack) {
//                                            tab.webView.goBack()
//                                        } else if (-drag > navigateThreshold && canFwd) {
//                                            tab.webView.goForward()
//                                        }
//                                        drag = 0f
//                                        show = false
//                                    },
//                                    onDragCancel = {
//                                        drag = 0f
//                                        show = false
//                                    }
//                                )
//                            }
                    )

//                    if (show) {
//                        val isBack = drag > 0f
//                        val shift = min(abs(drag), maxShift).toInt()
//                        val alphaFactor = min(abs(drag) / navigateThreshold, 1f)
//
//                        Box(
//                            modifier = Modifier
//                                .fillMaxSize()
//                                .zIndex(10f),
//                        ) {
//                            Box(
//                                modifier = Modifier
//                                    .align(if (isBack) Alignment.CenterStart else Alignment.CenterEnd)
//                                    .offset { IntOffset(if (isBack) shift else -shift, 0) }
//                                    .size(44.dp)
//                                    .background(
//                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.7f * alphaFactor),
//                                        CircleShape
//                                    )
//                                    .alpha(alphaFactor),
//                                contentAlignment = Alignment.Center
//                            ) {
//                                Icon(
//                                    imageVector = if (isBack) Icons.AutoMirrored.Default.ArrowBack else Icons.AutoMirrored.Default.ArrowForward,
//                                    contentDescription = stringResource(if (isBack) R.string.back else R.string.forward),
//                                    tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = alphaFactor)
//                                )
//                            }
//                        }
//                    }
                }
            }
        }

        Column {
            activeTab?.let { tab ->
                val animatedProgress by animateFloatAsState(
                    targetValue = tab.loadingProgress.value,
                    animationSpec = androidx.compose.animation.core.tween(
                        durationMillis = 250
                    )
                )

                if (0f < tab.loadingProgress.value && tab.loadingProgress.value < 1f) {
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                    )
                }
            }

            Spacer(Modifier.padding(top = 4.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState())) {
                    tabs.forEach { tab ->
                        val isActive = tab.id == activeTabId
                        Row(
                            modifier = Modifier
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { activeTabId = tab.id }
                                .padding(all = 4.dp)
                        ) {
                            Text(
                                text = tab.title.value.take(10),
                                color = if (isActive) colors.primary else colors.outline
                            )
                            ButtonSpacer()
                            IconButton(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .tooltip(stringResource(R.string.close)),
                                onClick = {
                                tabs = tabs.filterNot { it.id == tab.id }

                                scope.launch {
                                    val result = snackbarHostState.showSnackbar(
                                        ctx.getString(R.string.closed_xxx, tab.webView.title),
                                        actionLabel = ctx.getString(R.string.undo),
                                        withDismissAction = false,
                                        duration = SnackbarDuration.Short)

                                    if (result == SnackbarResult.ActionPerformed) {
                                        val newTab = createTab(ctx, snackbarHostState, clipboard, scope, history, url = tab.webView.url ?: "")
                                        tabs = tabs + newTab
                                        activeTabId = newTab.id
                                    }
                                }

                                activeTabId = if (tabs.isNotEmpty()) {
                                    tabs.first().id
                                } else {
                                    val homeTab = createTab(ctx, snackbarHostState, clipboard, scope, history)
                                    tabs = listOf(homeTab)
                                    homeTab.id
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.close),
                                    tint = if (isActive) colors.primary else colors.outline
                                )
                            }
                        }
                    }
                }

                IconButton(onClick = {
                    val newTab = createTab(ctx, snackbarHostState, clipboard, scope, history)
                    tabs = tabs + newTab
                    activeTabId = newTab.id
                }) {
                    Icon(modifier = Modifier.tooltip(stringResource(R.string.new_tab)),
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.new_tab),
                        tint = colors.primary)
                }
            }

            Spacer(Modifier.padding(top = 4.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(all = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val addressBarSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                var showAddressBarSheet by remember { mutableStateOf(false) }
                var addressBarSheetInput by remember { mutableStateOf(TextFieldValue("")) }
                val addressBarFocusRequester = remember { FocusRequester() }

                Spacer(Modifier.size(4.dp))
                Box(
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.medium)
                        .fillMaxHeight()
                        .weight(1f)
                        .clickable { showAddressBarSheet = true }
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(text = activeTab?.url?.value.withoutScheme(),
                        maxLines = 1,
                        overflow = TextOverflow.Clip)
                }

                if (showAddressBarSheet) {
                    LaunchedEffect(Unit) {
                        val address = activeTab?.url?.value ?: ""
                        addressBarSheetInput = TextFieldValue(address,
                            selection = TextRange(0, address.length))
                        addressBarFocusRequester.requestFocus()
                    }

                    ModalBottomSheet(
                        onDismissRequest = { showAddressBarSheet = false },
                        sheetState = addressBarSheetState,
                        dragHandle = null
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {

                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 150.dp)
                                    .padding(top = 8.dp, start = 8.dp, end = 8.dp)
                            ) {
                                items(listOf("Suggestion 1", "Suggestion 2", "Suggestion 3")) { suggestion ->
                                    Text(
                                        text = suggestion,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clickable {
                                                activeTab?.webView?.openUrl(suggestion)
                                                showAddressBarSheet = false
                                            }) } }

                            Row(modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 2.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { addressBarSheetInput.text }) {
                                    Icon(Icons.Default.Search,
                                        contentDescription = stringResource(R.string.search),
                                        modifier = Modifier.tooltip(stringResource(R.string.search)))
                                }

                                Box(modifier = Modifier.weight(1f)) {
                                    BasicTextField(
                                        value = addressBarSheetInput,
                                        onValueChange = { addressBarSheetInput = it },
                                        singleLine = true,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .focusRequester(addressBarFocusRequester),
                                        keyboardOptions = KeyboardOptions.Default.copy(
                                            imeAction = ImeAction.Go
                                        ),
                                        keyboardActions = KeyboardActions(
                                            onGo = {
                                                activeTab?.webView?.openUrl(addressBarSheetInput.text)
                                                showAddressBarSheet = false
                                            }
                                        ),
                                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                                            color = MaterialTheme.colorScheme.onSurface
                                        ),
                                    )

                                    if (addressBarSheetInput.text.isEmpty()) {
                                        Text(
                                            stringResource(R.string.empty_address_bar_msg),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.align(Alignment.CenterStart),
                                            maxLines = 1
                                        )
                                    }
                                }

                                IconButton(onClick = { addressBarSheetInput = TextFieldValue("") }) {
                                    Icon(Icons.Default.Clear,
                                        contentDescription = stringResource(R.string.clear),
                                        modifier = Modifier.tooltip(stringResource(R.string.clear)))
                                }

                                IconButton(onClick = {
                                    // TODO
                                }) {
                                    Icon(painterResource(R.drawable.open_in_full_24px),
                                        contentDescription = stringResource(R.string.maximize),
                                        modifier = Modifier.tooltip(stringResource(R.string.maximize)))
                                }

                                IconButton(Modifier.combinedClickable(
                                    onClick = {
                                        if (addressBarSheetInput.text.isEmpty()) {
                                            return@combinedClickable
                                        }

                                        activeTab?.webView?.openUrl(addressBarSheetInput.text)
                                        showAddressBarSheet = false
                                    },
                                    onLongClick = {
                                        if (addressBarSheetInput.text.isEmpty()) {
                                            return@combinedClickable
                                        }

                                        activeTab?.webView?.openUrl(addressBarSheetInput.text, true)
                                        showAddressBarSheet = false
                                    },
                                    role = Role.Button,
                                    interactionSource = null
                                )) {
                                    Icon(painterResource(R.drawable.arrow_forward_24px),
                                        contentDescription = stringResource(R.string.go))
                                }
                            }
                        }
                    }
                }

                val menuSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                var showMenuSheet by remember { mutableStateOf(false) }

                IconButton(onClick = {
                    tabs.find { it.id == activeTabId }?.webView?.reload()
                }) {
                    Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.reload))
                }

                IconButton(onClick = {
                    tabs.find { it.id == activeTabId }?.webView?.openUrl(DEFAULT_HOME_URL)
                }) {
                    Icon(Icons.Default.Home, contentDescription = "Home")
                }

                IconButton(onClick = { /* TODO */ }) {
                    Icon(Icons.Default.Menu, contentDescription = "Placeholder")
                }

                IconButton(onClick = { showMenuSheet = true }) {
                    Icon(Icons.Default.Menu, contentDescription = stringResource(R.string.menu),
                        Modifier.tooltip(R.string.menu))
                }

                var showOpenUrlDialog by remember { mutableStateOf(false) }

                if (showMenuSheet) {
                    val items = listOf(
                        MenuItem({ Icon(Icons.Default.Settings, null, it) },
                            stringResource(R.string.settings), { navController.navigate("browser_settings") }),
                        MenuItem({ Icon(painterResource(R.drawable.share_windows_24px), null, it) },
                            stringResource(R.string.open_in_external_app), {
//                                openUrlInExternalApps(ctx, activeTab?.url?.value, snackbarHostState, scope)
                                showOpenUrlDialog = true
                            })
                    )
                    val itemsPerPage = 10
                    val pageCount = (items.size + itemsPerPage - 1) / itemsPerPage
                    val pagerState = rememberPagerState(pageCount = { pageCount })
                    ModalBottomSheet(
                        onDismissRequest = { showMenuSheet = false },
                        sheetState = menuSheetState,
                        dragHandle = null
                    ) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxWidth()
                        ) { pageIndex ->

                            val startIndex = pageIndex * itemsPerPage
                            val endIndex = minOf(startIndex + itemsPerPage, items.size)
                            val pageItems = items.subList(startIndex, endIndex)

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                val firstRow = pageItems.take(5)
                                val secondRow = pageItems.drop(5)

                                Row(
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    firstRow.forEach { item ->
                                        MenuButton(item) { showMenuSheet = false }
                                    }
                                }

                                Spacer(Modifier.height(16.dp))

                                Row(
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    secondRow.forEach { item ->
                                        MenuButton(item) { showMenuSheet = false }
                                    }
                                }
                            }
                        }
                    }
                }

                if (showOpenUrlDialog) {
                    OpenItemDialog(OpenableItem.UrlItem(activeTab?.url?.value.toString())) { showOpenUrlDialog = false }
                }
            }
        }
    }
}

@Composable
fun MenuButton(item: MenuItem, onDismiss: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .padding(8.dp)
            .clickable { item.onClick(); onDismiss() }
    ) {
        item.icon(Modifier.size(32.dp))
        Spacer(Modifier.height(4.dp))
        Text(
            text = item.title,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
    }
}


private fun handleSpecialUrls(urlStr: String?, ctx: Context, scope: CoroutineScope, view: WebView?, snackbarHostState: SnackbarHostState): Boolean? {
    if (urlStr == null) {
        return null
    }

    // redirect http to https
    if (urlStr.startsWith("http://")) {
        val httpsUrl = urlStr.replaceFirst("http://", "https://")
        view?.stopLoading()
        view?.openUrl(httpsUrl)
//        snackbarHostState.showShort(
//            ctx.getString(R.string.redirected_http_to_https), scope)
        return true
    }
    val url = urlStr.toUri()
    val packageManager = ctx.packageManager

    // handle non-web schemes
    try {
        // intents
        if (urlStr.startsWith("intent://")) {
            val intent = Intent.parseUri(urlStr, Intent.URI_INTENT_SCHEME)

            scope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = ctx.getString(R.string.open_in_external_app_xxx_question,
                        PackageUtils.getAppName(ctx, intent.`package` ?: intent.component?.packageName) ?:
                        ctx.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.name),
                    actionLabel = ctx.getString(R.string.open),
                    withDismissAction = true,
                    duration = SnackbarDuration.Long
                )

                if (result == SnackbarResult.ActionPerformed) {
                    val fallbackUrl = intent.getStringExtra("browser_fallback_url")
                    if (intent.`package` != null && intent.resolveActivity(packageManager) != null) {
                        ctx.startActivity(intent)
                    } else if (fallbackUrl != null) {
                        snackbarHostState.showShort(
                            ctx.getString(R.string.using_fallback_url), scope
                        )
                        view?.openUrl(fallbackUrl)
                    }
                }
            }
            return true
        }

        // custom schemes
        val scheme = url.scheme
        if (scheme != null && scheme != "http" && scheme != "https") {
            val intent = Intent(Intent.ACTION_VIEW, url)
            val activity = intent.resolveActivity(packageManager)
            if (activity != null) {
                scope.launch {
                    val result = snackbarHostState.showSnackbar(
                        message = ctx.getString(R.string.open_in_external_app_xxx_question,
                            PackageUtils.getAppName(ctx, activity.packageName) ?: activity.packageName),
                        actionLabel = ctx.getString(R.string.open),
                        withDismissAction = true,
                        duration = SnackbarDuration.Long
                    )

                    if (result == SnackbarResult.ActionPerformed) {
                        ctx.startActivity(intent)
                    }
                }
            } else {
                snackbarHostState.showShort(
                    ctx.getString(R.string.no_apps_to_handle_scheme_xxx, scheme), scope)
                Log.e(TAG, ctx.getString(R.string.no_apps_to_handle_scheme_xxx, scheme))
            }
            return true
        }

    } catch (e: Exception) {
        Log.e(TAG, "exception while handling url: $urlStr", e)
        return false
    }

    return false
}

//private fun openUrlInExternalApps(ctx: Context, url: String?, snackbarHostState: SnackbarHostState, scope: CoroutineScope) {
//    if (url == null) return
//
//    try {
//        val uri = url.toUri()
//        val intent = Intent(Intent.ACTION_VIEW, uri)
//        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
//
//        val packageManager = ctx.packageManager
//        val resolveInfo = packageManager.queryIntentActivities(intent, 0)
//
//        if (resolveInfo.isNotEmpty()) {
//            val chooser = Intent.createChooser(intent, ctx.getString(R.string.open_in_external_app))
//            chooser.flags = Intent.FLAG_ACTIVITY_NEW_TASK
//            ctx.startActivity(chooser)
//        } else {
//            snackbarHostState.showShort(
//                ctx.getString(R.string.no_apps_to_handle_url_xxx, url), scope)
//        }
//    } catch (e: Exception) {
//        Log.e(TAG, "failed to open link in external apps", e)
//        snackbarHostState.showShort(
//            ctx.getString(R.string.no_apps_to_handle_url_xxx, url), scope)
//    }
//}

@SuppressLint("SetJavaScriptEnabled")
private fun createTab(
    ctx: ComponentActivity,
    snackbarHostState: SnackbarHostState,
    clipboard: Clipboard,
    scope: CoroutineScope,
    history: HistoryRepository,
    url: String = DEFAULT_HOME_URL,
): BrowserTab {
    val titleState = mutableStateOf("New tab")
    val urlState = mutableStateOf(url)
    val progressState = mutableFloatStateOf(0f)

    val webView = WebView(ctx).apply {
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.setSupportZoom(true)
        settings.loadWithOverviewMode = true
        settings.useWideViewPort = true
        settings.builtInZoomControls = true
        settings.displayZoomControls = false
        settings.safeBrowsingEnabled = false
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW

        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(this, false)

        addJavascriptInterface(
            ClipboardBridge(context, scope, snackbarHostState),
            "ClipboardBridge"
        )

        webViewClient = object : WebViewClient() {
            override fun onPageFinished(
                view: WebView?,
                url: String?
            ) {
                super.onPageFinished(view, url)

                view?.title?.let { titleState.value = it }
                view?.url?.let { urlState.value = it }
                Log.d(TAG, "written ${view?.url} to urlState")
                progressState.floatValue = 0f

                val js = """
                (function() {
                    const originalWriteText = navigator.clipboard.writeText;
                    navigator.clipboard.writeText = function(text) {
                        alert("copy function invoked 1");
                        ClipboardBridge.copyText(text.toString());
                        return Promise.resolve();
                    };
                    
                    document.addEventListener('copy', function(e) {
                        var selected = window.getSelection().toString();
                        if (selected) {
                            ClipboardBridge.copyText(selected);
                        }
                    });
                })();
                """.trimIndent()
                view?.evaluateJavascript(js, null)

                if (url != null) {
                    scope.launch {
                        history.addEntry(url, view?.title ?: url)
                    }
                }
            }

            // this function is invoked on user navigation & loadUrl()
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                handleSpecialUrls(url, ctx, scope, view, snackbarHostState)
            }

            // this function is invoked on user navigation
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val result = handleSpecialUrls(request?.url.toString(), ctx, scope, view, snackbarHostState)
                return result == true
            }

            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                if (request == null) {
                    Log.w(TAG, "request is null")
                    return null
                }

                val headers = request.requestHeaders
                headers["DNT"] = "1"
                headers["Sec-GPC"] = "1"
                headers

                return super.shouldInterceptRequest(view, request)
            }
        }
        webChromeClient = object : WebChromeClient() {
            // TODO: file uploads
//            var filePathCallback: ValueCallback<Array<Uri>>? = null
//            var fileChooserLauncher = ctx.registerForActivityResult(
//                ActivityResultContracts.StartActivityForResult()
//            ) { result ->
//                val data = result.data
//                val uris: Array<Uri>? = when {
//                    result.resultCode == Activity.RESULT_OK && data != null -> {
//                        data.clipData?.let { clip ->
//                            Array(clip.itemCount) { i -> clip.getItemAt(i).uri }
//                        } ?: data.data?.let { arrayOf(it) }
//                    }
//                    else -> null
//                }
//
//                filePathCallback?.apply {
//                    onReceiveValue(uris)
//                    filePathCallback = null
//                }
//            }

            override fun onProgressChanged(view: WebView, newProgress: Int) {
                progressState.floatValue = newProgress / 100f
            }

            override fun onJsAlert(view: WebView, url: String, message: String, result: JsResult): Boolean {
                AlertDialog.Builder(context)
                    .setTitle("Website alerts: $url")
                    .setMessage(message)
                    .setPositiveButton(android.R.string.ok) { _, _ -> result.confirm() }
                    .setCancelable(false)
                    .show()
                return true
            }

            override fun onJsConfirm(view: WebView, url: String, message: String, result: JsResult): Boolean {
                AlertDialog.Builder(context)
                    .setTitle("Website asks for confirmation: $url")
                    .setMessage(message)
                    .setPositiveButton(android.R.string.ok) { _, _ -> result.confirm() }
                    .setNegativeButton(android.R.string.cancel) { _, _ -> result.cancel() }
                    .setCancelable(false)
                    .show()
                return true
            }

            override fun onJsPrompt(
                view: WebView,
                url: String,
                message: String,
                defaultValue: String,
                result: JsPromptResult
            ): Boolean {
                val input = EditText(context)
                input.setText(defaultValue)
                AlertDialog.Builder(context)
                    .setTitle("Website prompts for input: $url")
                    .setMessage(message)
                    .setView(input)
                    .setPositiveButton(android.R.string.ok) { _, _ -> result.confirm(input.text.toString()) }
                    .setNegativeButton(android.R.string.cancel) { _, _ -> result.cancel() }
                    .setCancelable(false)
                    .show()
                return true
            }

//            override fun onShowFileChooser(
//                webView: WebView?,
//                filePathCallback: ValueCallback<Array<Uri>>?,
//                fileChooserParams: FileChooserParams?
//            ): Boolean {
//                this.filePathCallback = filePathCallback
//                val intent = fileChooserParams?.createIntent()
//                intent?.let { fileChooserLauncher.launch(it) }
//                return true
//            }

            override fun onCreateWindow(
                view: WebView,
                isDialog: Boolean,
                isUserGesture: Boolean,
                resultMsg: Message
            ): Boolean {
                val newWebView = WebView(context)
                newWebView.settings.javaScriptEnabled = true
                newWebView.settings.domStorageEnabled = true
                val transport = resultMsg.obj as? WebView.WebViewTransport
                transport?.webView = newWebView
                resultMsg.sendToTarget()
                return true
            }

            override fun onGeolocationPermissionsShowPrompt(origin: String, callback: GeolocationPermissions.Callback) {
                AlertDialog.Builder(context)
                    .setTitle(context.getString(R.string.website_asks_for_location_perm_xxx, url))
                    .setPositiveButton("Allow") { _, _ -> callback.invoke(origin, true, false) }
                    .setNegativeButton("Deny") { _, _ -> callback.invoke(origin, false, false) }
                    .show()
            }
        }
        setDownloadListener { url, userAgent, contentDisposition, mimeType, contentLength ->
            val fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
            val input = EditText(ctx)
            input.setText(fileName)
            AlertDialog.Builder(ctx)
                .setTitle(R.string.download_file_question)
                .setView(input)
                .setPositiveButton(R.string.yes) { _, _ ->
                    val outputDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    val dlManager = ServiceLocator.get(DownloadService::class.java)!!
                    dlManager.startDownload(
                        url = url,
                        outputDir = outputDir,
                        fileName = fileName,
                        mimeType = mimeType,
                        headers = mapOf("User-Agent" to userAgent),
                        onComplete = { file ->
                            scope.launch {
                                if (file != null) {
                                    snackbarHostState.showShort(
                                        ctx.getString(R.string.download_complete_xxx, fileName),
                                        scope
                                    )
                                } else {
                                    snackbarHostState.showShort(
                                        ctx.getString(R.string.download_failed_xxx, fileName),
                                        scope
                                    )
                                }
                            }
                        }
                    )

//                    val request = DownloadManager.Request(url.toUri())
//                    request.setMimeType(mimeType)
//                    request.addRequestHeader("User-Agent", userAgent)
//                    request.setDescription(ctx.getString(R.string.downloading_file))
//                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
//                    request.setTitle(input.text)
//                    request.setDestinationInExternalPublicDir(
//                        Environment.DIRECTORY_DOWNLOADS, input.text.toString())
//
//                    val dm = ctx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
//                    dm.enqueue(request)

                    snackbarHostState.showShort(ctx.getString(R.string.download_started), scope)
                }
                .setNegativeButton(R.string.no) { _, _ ->

                }
                .setNeutralButton(R.string.copy_link) { _, _ ->
                    scope.launch {
                        clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("Download link", url)))
                    }
                    snackbarHostState.showShort(ctx.getString(R.string.copied_to_clipboard), scope)
                }
                .show()


        }
        openUrl(url)
    }
    return BrowserTab(
        webView = webView,
        title = titleState,
        url = urlState,
        loadingProgress = progressState
    )
}

fun WebView.openUrl(url: String, forceOpen: Boolean = false) {
    if (forceOpen) {
        this.loadUrl(url)
        return
    }

    var result = url

    if (result.startsWith("http://"))
        result = url.replaceFirst("http://", "https://")

    if (!result.isUri())
        result = DEFAULT_SEARCH_ENGINE.format(result)

    if (result.toUri().scheme == null) {
        result = "https://${result}"
    }

    this.loadUrl(result)
}
