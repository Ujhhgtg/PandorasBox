package dev.ujhhgtg.pandorasbox.ui.composables.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.highcapable.yukihookapi.YukiHookAPI
import com.highcapable.yukihookapi.hook.factory.prefs
import dev.ujhhgtg.pandorasbox.R
import dev.ujhhgtg.pandorasbox.ui.activities.LocalScrollBehavior
import dev.ujhhgtg.pandorasbox.ui.composables.dialogs.AppChooserDialog
import dev.ujhhgtg.pandorasbox.ui.composables.dialogs.XposedAppChooserDialog
import dev.ujhhgtg.pandorasbox.ui.composables.widgets.DefaultColumn
import dev.ujhhgtg.pandorasbox.ui.composables.widgets.NumberAdjuster
import dev.ujhhgtg.pandorasbox.ui.composables.widgets.Text

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun XposedScreen() {
    val ctx = LocalContext.current
    var showSelAppDialog by remember { mutableStateOf(false) }
    var showSelActDialog by remember { mutableStateOf(false) }
    var selectedApp by remember { mutableStateOf("default") }

    var localPbgEnabled by remember {
        mutableStateOf(
            ctx.prefs("xposed").getBoolean("${selectedApp}_pbg_e", false)
        )
    }
    var localPbgBlockedActs by
    remember {
        mutableStateOf(
            ctx.prefs("xposed").getStringSet("${selectedApp}_pbg_bl", emptySet()).toMutableSet()
        )
    }
    var localFontScale by remember {
        mutableFloatStateOf(
            ctx.prefs("xposed").getFloat("${selectedApp}_sc_fs", 1.0f)
        )
    }
    var localDensityScale by remember {
        mutableFloatStateOf(
            ctx.prefs("xposed").getFloat("${selectedApp}_sc_ds", 1.0f)
        )
    }
    var localSigSpoofEnabled by remember {
        mutableStateOf(
            ctx.prefs("xposed").getBoolean("${selectedApp}_ss_e", false)
        )
    }
    var localSigSpoofMakeSoleSignerEnabled by remember {
        mutableStateOf(
            ctx.prefs("xposed").getBoolean("${selectedApp}_ss_so", false)
        )
    }
    var localSignatureTextFieldValue by remember {
        mutableStateOf(
            TextFieldValue(
                ctx.prefs("xposed").getString("${selectedApp}_ss_s", "")
            )
        )
    }
    var localDefaultTransitionsEnabled by remember {
        mutableStateOf(
            ctx.prefs("xposed").getBoolean("${selectedApp}_dt_e", false)
        )
    }

    DefaultColumn(LocalScrollBehavior.current) {
        Text(if (YukiHookAPI.Status.isXposedModuleActive) R.string.activated else R.string.unactivated)

        Spacer(Modifier.height(12.dp))
        HorizontalDivider(thickness = 2.dp)
        Spacer(Modifier.height(12.dp))

        Button(onClick = { showSelAppDialog = true }) {
            Text("${stringResource(R.string._package)}: $selectedApp")
        }
        if (showSelAppDialog) {
            XposedAppChooserDialog({ showSelAppDialog = false }) {
                selectedApp = it
                localPbgEnabled = ctx.prefs("xposed").getBoolean("${selectedApp}_pbg_e", false)
                localPbgBlockedActs =
                    ctx.prefs("xposed").getStringSet("${selectedApp}_pbg_bl", emptySet())
                        .toMutableSet()
                localFontScale = ctx.prefs("xposed").getFloat("${selectedApp}_ss_fs", 1.0f)
                localDensityScale = ctx.prefs("xposed").getFloat("${selectedApp}_ss_ds", 1.0f)
                localSignatureTextFieldValue =
                    TextFieldValue(ctx.prefs("xposed").getString("${selectedApp}_ss_s", ""))
                showSelAppDialog = false
            }
        }

        Spacer(Modifier.height(12.dp))
        HorizontalDivider(thickness = 2.dp)
        Spacer(Modifier.height(12.dp))
        Text(R.string.predictive_back_gestures, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(8.dp))

        ListItem(
            headlineContent = { Text(R.string.enable) },
            trailingContent = {
                Switch(localPbgEnabled, onCheckedChange = {
                    localPbgEnabled = it
                    ctx.prefs("xposed").edit {
                        putBoolean("${selectedApp}_pbg_e", localPbgEnabled)
                    }
                })
            }
        )
        Button(onClick = {
            if (selectedApp != "default")
                showSelActDialog = true
        }) {
            Text(R.string.select_activity)
        }

        if (showSelActDialog) {
            AppChooserDialog({ showSelActDialog = false }, {
                localPbgBlockedActs += it.activity
                ctx.prefs("xposed").edit {
                    putStringSet("${selectedApp}_pbg_bl", localPbgBlockedActs)
                }
            }, withActivities = true, specificApp = selectedApp)
        }

        Spacer(Modifier.height(12.dp))
        HorizontalDivider(thickness = 2.dp)
        Spacer(Modifier.height(12.dp))
        Text(R.string.scaler, style = MaterialTheme.typography.bodyLarge)

        NumberAdjuster(
            "${stringResource(R.string.font_scale)}: %.3f",
            localFontScale,
            1.0f,
            0.1f,
            2.0f,
            valueStep = 0.1f,
        ) {
            localFontScale = it
            ctx.prefs("xposed").edit {
                putFloat("${selectedApp}_ss_fs", localFontScale)
            }
        }

        NumberAdjuster(
            "${stringResource(R.string.density_scale)}: %.3f",
            localDensityScale,
            1.0f,
            0.1f,
            2.0f,
            valueStep = 0.1f,
        ) {
            localDensityScale = it
            ctx.prefs("xposed").edit {
                putFloat("${selectedApp}_ss_ds", localDensityScale)
            }
        }

        HorizontalDivider(thickness = 2.dp)
        Spacer(Modifier.height(12.dp))
        Text(R.string.signature_spoof, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(8.dp))

        if (selectedApp == "default")
            ListItem(
                headlineContent = { Text(R.string.enable) },
                trailingContent = {
                    Switch(localSigSpoofEnabled, onCheckedChange = {
                        localSigSpoofEnabled = it
                        ctx.prefs("xposed").edit {
                            putBoolean("default_ss_e", localSigSpoofEnabled)
                        }
                    })
                }
            )
        var showInputSignatureDialog by remember { mutableStateOf(false) }
        if (selectedApp != "default") {
            Button(onClick = { showInputSignatureDialog = true }) {
                Text(R.string.input_signature)
            }
        }
        if (showInputSignatureDialog) {
            AlertDialog(
                onDismissRequest = {
                    localSignatureTextFieldValue =
                        TextFieldValue(ctx.prefs("xposed").getString("${selectedApp}_ss_s", ""))
                    showInputSignatureDialog = false
                },
                title = { Text(R.string.input_signature) },
                text = {
                    OutlinedTextField(
                        value = localSignatureTextFieldValue,
                        onValueChange = { localSignatureTextFieldValue = it },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                    )
                },
                dismissButton = {
                    TextButton(onClick = {
                        localSignatureTextFieldValue =
                            TextFieldValue(ctx.prefs("xposed").getString("${selectedApp}_ss_s", ""))
                        showInputSignatureDialog = false
                    }) { Text(R.string.cancel) }
                },
                confirmButton = {
                    TextButton(onClick = {
                        ctx.prefs("xposed").edit {
                            putString("${selectedApp}_ss_s", localSignatureTextFieldValue.text)
                        }
                        localSignatureTextFieldValue =
                            TextFieldValue(ctx.prefs("xposed").getString("${selectedApp}_ss_s", ""))
                        showInputSignatureDialog = false
                    }) { Text(R.string.ok) }
                })
        }
        ListItem(
            headlineContent = { Text(R.string.make_sole_signer) },
            trailingContent = {
                Switch(localSigSpoofMakeSoleSignerEnabled, onCheckedChange = {
                    localSigSpoofMakeSoleSignerEnabled = it
                    ctx.prefs("xposed").edit {
                        putBoolean("${selectedApp}_ss_so", localSigSpoofMakeSoleSignerEnabled)
                    }
                })
            }
        )

        HorizontalDivider(thickness = 2.dp)
        Spacer(Modifier.height(12.dp))
        Text(R.string.default_transitions, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(8.dp))

        ListItem(
            headlineContent = { Text(R.string.enable) },
            trailingContent = {
                Switch(localDefaultTransitionsEnabled, onCheckedChange = {
                    localDefaultTransitionsEnabled = it
                    ctx.prefs("xposed").edit {
                        putBoolean("${selectedApp}_dt_e", localDefaultTransitionsEnabled)
                    }
                })
            }
        )
    }
}
