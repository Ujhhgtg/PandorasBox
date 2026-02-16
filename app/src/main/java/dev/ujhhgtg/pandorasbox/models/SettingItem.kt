package dev.ujhhgtg.pandorasbox.models

import androidx.compose.runtime.Composable

sealed class SettingItem(open val label: String) {
    data class Toggle(
        override val label: String,
        val defaultValue: Boolean = false,
        val description: String = "",
        val icon: (@Composable () -> Unit)? = null
    ) : SettingItem(label)

    data class Selection(
        override val label: String,
        val options: List<String>,
        val selectedIndex: Int = 0,
        val defaultIndex: Int = 0,
        val description: String = "",
        val icon: (@Composable () -> Unit)? = null
    ) : SettingItem(label) {
        val selectedOption: String
            get() = options[selectedIndex]
    }

    data class Input(
        override val label: String,
        val defaultValue: String = "",
        val validator: ((String) -> Boolean)? = null,
        val validationFailMessage: String = "",
        val description: String = "",
        val icon: (@Composable () -> Unit)? = null
    ) : SettingItem(label)

    data class SubPage(
        override val label: String,
        val children: List<SettingItem>,
        val description: String = "",
        val icon: (@Composable () -> Unit)? = null
    ) : SettingItem(label)

    data class CustomPage(
        override val label: String,
        val content: @Composable () -> Unit,
        val description: String = "",
        val icon: (@Composable () -> Unit)? = null
    ) : SettingItem(label)

    data class Action(
        override val label: String,
        val onClick: () -> Unit,
        val content: (@Composable () -> Unit)? = null,
        val description: String = "",
        val icon: (@Composable () -> Unit)? = null
    ) : SettingItem(label)

    val id: String
        get() = "settings_" + label.lowercase().replace(' ', '_')
}
