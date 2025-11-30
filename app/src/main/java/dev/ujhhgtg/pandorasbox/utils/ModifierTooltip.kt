package dev.ujhhgtg.pandorasbox.utils

import android.annotation.SuppressLint
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.res.stringResource

@SuppressLint("SuspiciousModifierThen")
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun Modifier.tooltip(
    text: String
): Modifier = this.then(composed {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = {
            PlainTooltip {
                Text(text)
            }
        },
        state = rememberTooltipState()
    ) {
        Box(modifier = this)
    }
    Modifier
})

@Composable
fun Modifier.tooltip(
    @StringRes id: Int
): Modifier = this.tooltip(stringResource(id))