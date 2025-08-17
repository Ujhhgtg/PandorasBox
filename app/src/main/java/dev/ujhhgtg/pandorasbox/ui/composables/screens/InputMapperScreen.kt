package dev.ujhhgtg.pandorasbox.ui.composables.screens

import android.app.Activity
import android.content.Intent
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.ujhhgtg.pandorasbox.R
import dev.ujhhgtg.pandorasbox.services.InputMapperService
import dev.ujhhgtg.pandorasbox.ui.activities.InputMapperEditorActivity
import dev.ujhhgtg.pandorasbox.ui.composables.ButtonSpacer
import dev.ujhhgtg.pandorasbox.ui.composables.DefaultColumn
import dev.ujhhgtg.pandorasbox.utils.PermissionManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InputMapperScreen(
    activity: Activity,
    scrollBehavior: TopAppBarScrollBehavior,
    toggleService: () -> Unit
) {
    var serviceStarted by remember { InputMapperService.isRunning }
    var serverUri by rememberSaveable { mutableStateOf("ws://192.168.1.6:8765") }
    val context = LocalContext.current

    DefaultColumn(scrollBehavior) {
        OutlinedTextField(
            value = serverUri,
            onValueChange = { serverUri = it },
            label = { Text("WebSocket URI") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = {
            if (!PermissionManager.checkAndRequestNotifications(activity))
                return@Button
            if (!PermissionManager.checkAndRequestOverlay(activity))
                return@Button
            if (!PermissionManager.checkAndRequestShizuku(activity))
                return@Button

            val intent = Intent(activity, InputMapperService::class.java)
            intent.putExtra("uri", serverUri)
            toggleService()
        }) {
            if (!serviceStarted) {
                Icon(
                    painter = painterResource(R.drawable.play_arrow_24px),
                    contentDescription = "start input mapper",
                )
                ButtonSpacer()
                Text(stringResource(R.string.start_input_mapper))
            } else {
                Icon(
                    painter = painterResource(R.drawable.pause_24px),
                    contentDescription = "stop input mapper",
                )
                ButtonSpacer()
                Text(stringResource(R.string.stop_input_mapper))
            }
        }
        Spacer(Modifier.height(24.dp))
        Button(onClick = {
            context.startActivity(Intent(context, InputMapperEditorActivity::class.java))
        }) {
            Icon(
                painter = painterResource(R.drawable.edit_24px),
                contentDescription = "edit mappings"
            )
            ButtonSpacer()
            Text(stringResource(R.string.edit_mappings))
        }
    }
}