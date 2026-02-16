package dev.ujhhgtg.pandorasbox.ui.composables.widgets

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

// InstallerX Revived
@Composable
fun SwitchWidget(
    leadingContent: @Composable () -> Unit = {},
    @StringRes headlineContentString: Int,
    description: String? = null,
    enabled: Boolean = true,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    isError: Boolean = false
) {
    val toggleAction = {
        if (enabled) {
            onCheckedChange(!checked)
        }
    }

    BaseWidget(
        leadingContent = leadingContent,
        headlineContentString = headlineContentString,
        enabled = enabled,
        isError = isError,
        onClick = toggleAction,
        hapticFeedbackType = HapticFeedbackType.ToggleOn,
        description = description
    ) {
        Switch(
            enabled = enabled,
            checked = checked,
            thumbContent = if (checked) {
                {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(SwitchDefaults.IconSize),
                    )
                }
            } else {
                {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.surfaceContainerHighest,
                        modifier = Modifier.size(SwitchDefaults.IconSize),
                    )
                }
            },
            onCheckedChange = null
        )
    }
}
