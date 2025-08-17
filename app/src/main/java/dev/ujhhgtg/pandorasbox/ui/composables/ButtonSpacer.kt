package dev.ujhhgtg.pandorasbox.ui.composables

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun ButtonSpacer() {
    Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
}