package dev.ujhhgtg.pandorasbox.ui.composables.screens

import android.app.Activity
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import dev.ujhhgtg.pandorasbox.R
import dev.ujhhgtg.pandorasbox.services.OverlayService
import dev.ujhhgtg.pandorasbox.ui.composables.ButtonSpacer
import dev.ujhhgtg.pandorasbox.ui.composables.DefaultColumn
import dev.ujhhgtg.pandorasbox.ui.composables.NumberAdjuster
import dev.ujhhgtg.pandorasbox.ui.composables.OffsetAdjuster
import dev.ujhhgtg.pandorasbox.ui.composables.PackageChooserDialog
import dev.ujhhgtg.pandorasbox.utils.PermissionManager
import dev.ujhhgtg.pandorasbox.utils.SettingsRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverlayScreen(
    settings: SettingsRepository,
    scrollBehavior: TopAppBarScrollBehavior,
    activity: Activity,
    toggleService: () -> Unit
) {
    var serviceStarted by remember { OverlayService.isRunning }
    var selectedPackage by rememberSaveable { mutableStateOf("default") }
    var showPackageDialog by rememberSaveable { mutableStateOf(false) }

    val hOffset by settings.loadSingleConfigFlow(
        floatPreferencesKey("o_${selectedPackage}_h"),
        0f
    ).collectAsState(0f)
    val vOffset by settings.loadSingleConfigFlow(
        floatPreferencesKey("o_${selectedPackage}_v"),
        0f
    ).collectAsState(0f)
    val dotSize by settings.loadSingleConfigFlow(
        intPreferencesKey("o_${selectedPackage}_s"),
        20
    ).collectAsState(20)
    val lineWidth by settings.loadSingleConfigFlow(
        intPreferencesKey("o_${selectedPackage}_w"),
        15
    ).collectAsState(15)
    var localHOffset by rememberSaveable(hOffset) { mutableFloatStateOf(hOffset) }
    var localVOffset by rememberSaveable(vOffset) { mutableFloatStateOf(vOffset) }
    var localDotSize by rememberSaveable(dotSize) { mutableIntStateOf(dotSize) }
    var localLineWidth by rememberSaveable(lineWidth) { mutableIntStateOf(lineWidth) }

    LaunchedEffect(localHOffset) {
        if (localHOffset != hOffset) {
            settings.saveSingleConfig(
                floatPreferencesKey("o_${selectedPackage}_h"),
                localHOffset
            )
        }
    }
    LaunchedEffect(localVOffset) {
        if (localVOffset != vOffset) {
            settings.saveSingleConfig(
                floatPreferencesKey("o_${selectedPackage}_v"),
                localVOffset
            )
        }
    }
    LaunchedEffect(localDotSize) {
        if (localDotSize != dotSize) {
            settings.saveSingleConfig(
                intPreferencesKey("o_${selectedPackage}_s"),
                localDotSize
            )
        }
    }
    LaunchedEffect(localLineWidth) {
        if (localLineWidth != lineWidth) {
            settings.saveSingleConfig(
                intPreferencesKey("o_${selectedPackage}_w"),
                localLineWidth
            )
        }
    }

    DefaultColumn(scrollBehavior) {
        Button(onClick = { showPackageDialog = true }) {
            Icon(
                painter = painterResource(R.drawable.package_2_24px),
                contentDescription = null
            )
            ButtonSpacer()
            Text("Package: $selectedPackage")
        }
        Spacer(Modifier.height(24.dp))
        OffsetAdjuster(
            label = stringResource(R.string.horizontal_offset),
            value = hOffset
        ) {
            localHOffset = it
        }
        Spacer(Modifier.height(24.dp))
        OffsetAdjuster(
            label = stringResource(R.string.vertical_offset),
            value = vOffset
        ) {
            localVOffset = it
        }
        Spacer(Modifier.height(24.dp))
        NumberAdjuster(
            label = "${stringResource(R.string.dot_size)}: %d",
            value = dotSize,
            defaultValue = 20,
            minValue = 0,
            maxValue = 30,
            valueStep = 1
        ) {
            localDotSize = it
        }
        Spacer(Modifier.height(24.dp))
        NumberAdjuster(
            label = "${stringResource(R.string.line_width)}: %d",
            value = lineWidth,
            defaultValue = 15,
            minValue = 0,
            maxValue = 20,
            valueStep = 1
        ) {
            localLineWidth = it
        }
        Spacer(Modifier.height(24.dp))
        Button(onClick = {
            if (!PermissionManager.checkAndRequestNotifications(activity))
                return@Button

            if (!PermissionManager.checkAndRequestOverlay(activity))
                return@Button

            if (!PermissionManager.checkAndRequestUsageStats(activity))
                return@Button

            toggleService()
        }) {
            if (!serviceStarted) {
                Icon(
                    painter = painterResource(R.drawable.play_arrow_24px),
                    contentDescription = "show overlay",
                )
                ButtonSpacer()
                Text(stringResource(R.string.show_overlay))
            } else {
                Icon(
                    painter = painterResource(R.drawable.pause_24px),
                    contentDescription = "hide overlay",
                )
                ButtonSpacer()
                Text(stringResource(R.string.hide_overlay))
            }
        }
    }

    if (showPackageDialog) {
        PackageChooserDialog(
            settings = settings,
            onDismiss = { showPackageDialog = false },
            onSelect = {
                selectedPackage = it
                showPackageDialog = false
            }
        )
    }
}