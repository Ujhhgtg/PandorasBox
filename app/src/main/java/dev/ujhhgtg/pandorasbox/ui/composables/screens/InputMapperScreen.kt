package dev.ujhhgtg.pandorasbox.ui.composables.screens

import android.content.Intent
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.ujhhgtg.pandorasbox.R
import dev.ujhhgtg.pandorasbox.services.InputMapperService
import dev.ujhhgtg.pandorasbox.ui.activities.InputMapperEditorActivity
import dev.ujhhgtg.pandorasbox.ui.activities.LocalActivityContext
import dev.ujhhgtg.pandorasbox.ui.activities.LocalScrollBehavior
import dev.ujhhgtg.pandorasbox.ui.composables.ButtonSpacer
import dev.ujhhgtg.pandorasbox.ui.composables.DefaultColumn
import dev.ujhhgtg.pandorasbox.ui.composables.Text
import dev.ujhhgtg.pandorasbox.utils.PermissionManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InputMapperScreen(toggleService: () -> Unit) {
    var serverUri by rememberSaveable { mutableStateOf("ws://192.168.1.6:8765") }
    val ctx = LocalActivityContext.current
    val scrollBehavior = LocalScrollBehavior.current

    DefaultColumn(scrollBehavior) {
        OutlinedTextField(
            value = serverUri,
            onValueChange = { serverUri = it },
            label = { Text("WebSocket URI") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = {
            if (!PermissionManager.checkAndRequestNotifications(ctx))
                return@Button
            if (!PermissionManager.checkAndRequestOverlay(ctx))
                return@Button
            if (!PermissionManager.checkAndRequestShizuku(ctx))
                return@Button

            val intent = Intent(ctx, InputMapperService::class.java)
            intent.putExtra("uri", serverUri)
            toggleService()
        }) {
            if (!InputMapperService.isRunning.value) {
                Icon(painterResource(R.drawable.play_arrow_24px), null)
                ButtonSpacer()
                Text(R.string.start_input_mapper)
            } else {
                Icon(painterResource(R.drawable.pause_24px), null)
                ButtonSpacer()
                Text(R.string.stop_input_mapper)
            }
        }
        Spacer(Modifier.height(24.dp))
        Button(onClick = {
            ctx.startActivity(Intent(ctx, InputMapperEditorActivity::class.java))
        }) {
            Icon(painterResource(R.drawable.edit_24px), null)
            ButtonSpacer()
            Text(R.string.edit_mappings)
        }
    }
}