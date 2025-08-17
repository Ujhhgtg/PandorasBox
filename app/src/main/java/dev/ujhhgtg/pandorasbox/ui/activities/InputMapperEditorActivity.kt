package dev.ujhhgtg.pandorasbox.ui.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import dev.ujhhgtg.pandorasbox.ui.composables.screens.InputMapperEditorScreen
import dev.ujhhgtg.pandorasbox.ui.theme.AppTheme

class InputMapperEditorActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false) // true fullscreen

        setContent {
            AppTheme {
                InputMapperEditorScreen(
                    onClose = { finish() }
                )
            }
        }
    }
}
