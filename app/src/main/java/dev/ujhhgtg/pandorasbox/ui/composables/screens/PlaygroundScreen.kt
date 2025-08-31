package dev.ujhhgtg.pandorasbox.ui.composables.screens

import android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_STRONG
import android.hardware.biometrics.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import android.hardware.biometrics.BiometricPrompt
import android.os.CancellationSignal
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.ujhhgtg.pandorasbox.R
import dev.ujhhgtg.pandorasbox.ui.activities.LocalActivityContext
import dev.ujhhgtg.pandorasbox.ui.activities.LocalScrollBehavior
import dev.ujhhgtg.pandorasbox.ui.activities.LocalSnackbarHostState
import dev.ujhhgtg.pandorasbox.ui.composables.ButtonSpacer
import dev.ujhhgtg.pandorasbox.ui.composables.ColorChooserDialog
import dev.ujhhgtg.pandorasbox.ui.composables.DefaultColumn
import dev.ujhhgtg.pandorasbox.ui.composables.NeonIndication
import dev.ujhhgtg.pandorasbox.ui.composables.Text
import dev.ujhhgtg.pandorasbox.utils.tooltip
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaygroundScreen() {
    val ctx = LocalActivityContext.current
    val scrollBehavior = LocalScrollBehavior.current
    val snackbarHostState = LocalSnackbarHostState.current
    val scope = rememberCoroutineScope()

    DefaultColumn(scrollBehavior = scrollBehavior) {
        val prompt = remember { BiometricPrompt.Builder(ctx)
                .setTitle("Biometric login for my app")
                .setSubtitle("Log in using your biometric credential")
                .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
                .build() }

        Button(
            onClick = {
                prompt.authenticate(CancellationSignal(), ctx.mainExecutor, object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationError(errorCode: Int,
                                                       errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                        Toast.makeText(ctx,
                            "Authentication error: $errString", Toast.LENGTH_SHORT)
                            .show()
                    }

                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        Toast.makeText(ctx,
                            "Authentication succeeded!", Toast.LENGTH_SHORT)
                            .show()
                    }

                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                        Toast.makeText(ctx, "Authentication failed",
                            Toast.LENGTH_SHORT)
                            .show()
                    }
                })
            }
        ) {
            Icon(painterResource(R.drawable.fingerprint_24px), null)
            ButtonSpacer()
            Text(R.string.show_biometric_prompt)
        }
        Spacer(Modifier.height(24.dp))

        val interactionSource = remember { MutableInteractionSource() }
        Row(
            modifier = Modifier
                .defaultMinSize(minWidth = 76.dp, minHeight = 48.dp)
                .clickable(
                    indication = NeonIndication(CircleShape, 2.dp),
                    interactionSource = interactionSource,
                    onClick = {
                        scope.launch {
                            val result = snackbarHostState.showSnackbar(
                                message = "Open in external app?",
                                actionLabel = "Open",
                                withDismissAction = true,
                                duration = SnackbarDuration.Long
                            )

                            if (result == SnackbarResult.ActionPerformed) {
                                Toast.makeText(ctx, ctx.getString(R.string.none), Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
                .border(width = 2.dp, color = Color.Gray, shape = CircleShape)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(R.string.neon_indication)
        }
        Spacer(Modifier.height(24.dp))

        var showDialog by remember { mutableStateOf(false) }
        var selectedColor by remember { mutableStateOf(Color.Red) }

        Box(
            modifier = Modifier
                .size(100.dp)
                .background(selectedColor, shape = CircleShape)
                .clip(CircleShape)
                .clickable { showDialog = true }
        )

        if (showDialog) {
            ColorChooserDialog(
                initialColor = selectedColor,
                onDismiss = { showDialog = false },
                onColorSelected = {
                    selectedColor = it
                    showDialog = false
                }
            )
        }
    }
}