package dev.ujhhgtg.pandorasbox.ui.composables.screens

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.highcapable.yukihookapi.YukiHookAPI
import dev.ujhhgtg.pandorasbox.R
import dev.ujhhgtg.pandorasbox.ui.activities.LocalScrollBehavior
import dev.ujhhgtg.pandorasbox.ui.composables.DefaultColumn

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun XposedScreen() {
    DefaultColumn(LocalScrollBehavior.current) {
        Text("${stringResource(R.string.activated)}: " + YukiHookAPI.Status.isXposedModuleActive)

        
    }
}
