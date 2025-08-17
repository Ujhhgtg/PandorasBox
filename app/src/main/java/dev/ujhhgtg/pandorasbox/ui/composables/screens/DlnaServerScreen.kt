package dev.ujhhgtg.pandorasbox.ui.composables.screens

import android.app.Activity
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.ujhhgtg.pandorasbox.R
import dev.ujhhgtg.pandorasbox.services.DlnaServerService
import dev.ujhhgtg.pandorasbox.ui.composables.ButtonSpacer
import dev.ujhhgtg.pandorasbox.ui.composables.DefaultColumn
import dev.ujhhgtg.pandorasbox.utils.PermissionManager
import dev.ujhhgtg.pandorasbox.utils.ServiceLocator
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DlnaServerScreen(
    activity: Activity,
    scrollBehavior: TopAppBarScrollBehavior,
    pickAudio: ActivityResultLauncher<String>,
    pickVideo: ActivityResultLauncher<String>,
    toggleService: () -> Unit
) {
    val service = remember { ServiceLocator.get(DlnaServerService::class.java) }
    var serviceStarted by remember { DlnaServerService.isRunning }
    val scope = rememberCoroutineScope()

    val devices = service?.deviceListFlow?.collectAsState(initial = emptyList())?.value ?: emptyList()
    val selected = service?.selectedDeviceFlow?.collectAsState(initial = null)?.value
    val isPlaying = service?.isPlayingFlow?.collectAsState(initial = false)?.value ?: false
    val pos = service?.positionSecFlow?.collectAsState(initial = 0L)?.value ?: 0L
    val dur = service?.durationSecFlow?.collectAsState(initial = 0L)?.value ?: 0L
    val vol = service?.volumeFlow?.collectAsState(initial = 50)?.value ?: 50
    val audioTracks = service?.audioTracksFlow?.collectAsState(initial = emptyList())?.value ?: emptyList()
    val subTracks = service?.subtitleTracksFlow?.collectAsState(initial = emptyList())?.value ?: emptyList()
    val selectedAudio = service?.selectedAudioFlow?.collectAsState(initial = null)?.value
    val selectedSub = service?.selectedSubtitleFlow?.collectAsState(initial = null)?.value

    DefaultColumn(scrollBehavior) {
        Text(stringResource(R.string.devices))
        Spacer(Modifier.height(8.dp))
        LazyColumn(Modifier.heightIn(max = 220.dp)) {
            items(devices) { d ->
                Row(Modifier.fillMaxWidth().clickable { service?.selectDevice(d) }.padding(8.dp)) {
                    Text(d.details?.friendlyName ?: d.displayString)
                }
            }
        }
        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { pickAudio.launch("audio/*") }) { Text("Pick audio") }
            Button(onClick = { pickVideo.launch("video/*") }) { Text("Pick video") }
        }
        Spacer(Modifier.height(12.dp))

        Button(onClick = {
            if (!PermissionManager.checkAndRequestNotifications(activity))
                return@Button

            toggleService()
        }) {
            if (!serviceStarted) {
                Icon(
                    painter = painterResource(R.drawable.play_arrow_24px),
                    contentDescription = stringResource(R.string.start_server),
                )
                ButtonSpacer()
                Text(stringResource(R.string.start_server))
            } else {
                Icon(
                    painter = painterResource(R.drawable.pause_24px),
                    contentDescription = stringResource(R.string.stop_server),
                )
                ButtonSpacer()
                Text(stringResource(R.string.stop_server))
            }
        }
        Spacer(Modifier.height(12.dp))

        ControlPanel(
            isPlaying = isPlaying,
            position = pos,
            duration = dur,
            volume = vol,
            audioTracks = audioTracks,
            selectedAudio = selectedAudio,
            subtitleTracks = subTracks,
            selectedSubtitle = selectedSub,
            onPlayPause = { scope.launch { if (isPlaying) service.pause() else service?.play() } },
            onSeek = { target -> scope.launch { service?.seekTo(target) } },
            onNext = { scope.launch { service?.next() } },
            onPrev = { scope.launch { service?.previous() } },
            onForward = { scope.launch { service?.seekTo(pos + 10) } },
            onBackward = { scope.launch { service?.seekTo((pos - 10).coerceAtLeast(0)) } },
            onVolumeChange = { v -> scope.launch { service?.setVolume(v) } },
            onAudioTrackSelect = { id -> scope.launch { service?.selectAudioTrack(id) } },
            onSubtitleSelect = { id -> scope.launch { service?.selectSubtitle(id) } }
        )

        selected?.let { Text("Selected: ${it.details?.friendlyName}") }
    }
}


@Composable
fun ControlPanel(
    isPlaying: Boolean,
    position: Long,
    duration: Long,
    volume: Int,
    audioTracks: List<String>,
    selectedAudio: String?,
    subtitleTracks: List<String>,
    selectedSubtitle: String?,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onForward: () -> Unit,
    onBackward: () -> Unit,
    onVolumeChange: (Int) -> Unit,
    onAudioTrackSelect: (String) -> Unit,
    onSubtitleSelect: (String) -> Unit,
) {
    fun formatTime(sec: Long): String {
        val s = sec % 60
        val m = (sec / 60) % 60
        val h = (sec / 3600)
        return if (h > 0) String.format(Locale.ROOT, "%d:%02d:%02d", h, m, s) else String.format(Locale.ROOT, "%02d:%02d", m, s)
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onPrev) { Text("Prev") }
            Button(onClick = onBackward) { Text("-10s") }
            Button(onClick = onPlayPause) { Text(if (isPlaying) "Pause" else "Play") }
            Button(onClick = onForward) { Text("+10s") }
            Button(onClick = onNext) { Text("Next") }
        }
        Column(Modifier.fillMaxWidth()) {
            val max = duration.coerceAtLeast(1).toFloat()
            Slider(value = position.toFloat().coerceIn(0f, max), onValueChange = { onSeek(it.toLong()) }, valueRange = 0f..max)
            Text("${formatTime(position)} / ${formatTime(duration)}")
        }
        Column(Modifier.fillMaxWidth()) {
            Text("Volume: $volume")
            Slider(value = volume.toFloat(), onValueChange = { onVolumeChange(it.toInt()) }, valueRange = 0f..100f)
        }
        TrackDropdown("Audio Track", audioTracks, selectedAudio, onAudioTrackSelect)
        TrackDropdown("Subtitles", subtitleTracks, selectedSubtitle, onSubtitleSelect)
    }
}

@Composable
fun TrackDropdown(label: String, items: List<String>, selected: String?, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().clickable { expanded = true }.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("$label: ${selected ?: "None"}")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("None") },
                onClick = { expanded = false; onSelect("") })
            items.forEach { id ->
                DropdownMenuItem(
                    text = { Text(id) },
                    onClick = { expanded = false; onSelect(id) })
            }
        }
    }
}