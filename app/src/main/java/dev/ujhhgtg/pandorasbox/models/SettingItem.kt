package dev.ujhhgtg.pandorasbox.models

import androidx.compose.runtime.Composable

sealed class SettingItem(val title: String) {
    data class Toggle(
        val label: String,
        val defaultValue: Boolean = false,
        val description: String = "",
        val icon: (@Composable () -> Unit)? = null
    ) : SettingItem(title = label) {
        val key: String
            get() = label.lowercase().replace(' ', '_')
    }

    data class Selection(
        val label: String,
        val options: List<String>,
        val selectedIndex: Int = 0,
        val defaultIndex: Int = 0,
        val description: String = "",
        val icon: (@Composable () -> Unit)? = null
    ) : SettingItem(title = label) {
        val selectedOption: String
            get() = options[selectedIndex]

        val key: String
            get() = label.lowercase().replace(' ', '_')
    }

    data class Input(
        val label: String,
        val defaultValue: String = "",
        val validator: ((String) -> Boolean)? = null,
        val validationFailMessage: String = "",
        val description: String = "",
        val icon: (@Composable () -> Unit)? = null
    ) : SettingItem(title = label) {
        val key: String
            get() = label.lowercase().replace(' ', '_')
    }

    data class SubPage(
        val label: String,
        val children: List<SettingItem>,
        val description: String = "",
        val icon: (@Composable () -> Unit)? = null
    ) : SettingItem(title = label) {
        val route: String
            get() = label.lowercase().replace(' ', '_')
    }

    data class CustomPage(
        val label: String,
        val content: @Composable () -> Unit,
        val description: String = "",
        val icon: (@Composable () -> Unit)? = null
    ) : SettingItem(title = label) {
        val route: String
            get() = label.lowercase().replace(' ', '_')
    }

    data class Action(
        val label: String,
        val onClick: () -> Unit,
        val content: (@Composable () -> Unit)? = null,
        val description: String = "",
        val icon: (@Composable () -> Unit)? = null
    ) : SettingItem(title = label)
}
