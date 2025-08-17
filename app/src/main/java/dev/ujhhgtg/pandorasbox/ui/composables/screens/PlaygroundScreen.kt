package dev.ujhhgtg.pandorasbox.ui.composables.screens

import android.content.Context
import android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_STRONG
import android.hardware.biometrics.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import android.hardware.biometrics.BiometricPrompt
import android.os.CancellationSignal
import android.widget.Toast
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import dev.ujhhgtg.pandorasbox.R
import dev.ujhhgtg.pandorasbox.ui.composables.ButtonSpacer
import dev.ujhhgtg.pandorasbox.ui.composables.DefaultColumn
import dev.ujhhgtg.pandorasbox.utils.tooltip

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaygroundScreen(ctx: Context, scrollBehavior: TopAppBarScrollBehavior) {
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
            Icon(
                painter = painterResource(R.drawable.fingerprint_24px),
                contentDescription = stringResource(R.string.show_biometric_prompt),
                modifier = Modifier.tooltip(stringResource(R.string.show_biometric_prompt))
            )
            ButtonSpacer()
            Text(stringResource(R.string.show_biometric_prompt))
        }
    }
}