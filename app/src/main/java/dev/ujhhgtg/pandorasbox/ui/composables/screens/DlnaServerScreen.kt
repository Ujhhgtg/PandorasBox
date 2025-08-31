package dev.ujhhgtg.pandorasbox.ui.composables.screens

import android.app.Activity
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.ujhhgtg.pandorasbox.R
import dev.ujhhgtg.pandorasbox.services.DlnaServerService
import dev.ujhhgtg.pandorasbox.ui.activities.LocalActivityContext
import dev.ujhhgtg.pandorasbox.ui.activities.LocalScrollBehavior
import dev.ujhhgtg.pandorasbox.ui.composables.ButtonSpacer
import dev.ujhhgtg.pandorasbox.ui.composables.Text
import dev.ujhhgtg.pandorasbox.utils.PermissionManager
import dev.ujhhgtg.pandorasbox.utils.ServiceLocator
import dev.ujhhgtg.pandorasbox.utils.tooltip
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DlnaServerScreen(toggleService: () -> Unit) {
    val service = remember { ServiceLocator.get(DlnaServerService::class.java) }
    val scope = rememberCoroutineScope()
    val ctx = LocalActivityContext.current
    val scrollBehavior = LocalScrollBehavior.current

    val pickAudio = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { playUri(ctx, it) }
    val pickVideo = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { playUri(ctx, it) }

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = {
            if (!PermissionManager.checkAndRequestNotifications(ctx))
                return@Button

            toggleService()
        }) {
            if (!DlnaServerService.isRunning.value) {
                Icon(
                    painter = painterResource(R.drawable.play_arrow_24px),
                    contentDescription = null
                )
                ButtonSpacer()
                Text(R.string.start_server)
            } else {
                Icon(
                    painter = painterResource(R.drawable.pause_24px),
                    contentDescription = null
                )
                ButtonSpacer()
                Text(R.string.stop_server)
            }
        }
        Spacer(Modifier.height(12.dp))

        Text(
            stringResource(R.string.devices),
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.heightIn(max = 220.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(devices) { d ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { service?.selectDevice(d) },
                    shape = MaterialTheme.shapes.medium,
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.devices_24px),
                            contentDescription = null,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                        Text(
                            text = d.details?.friendlyName ?: d.displayString,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
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
            onSubtitleSelect = { id -> scope.launch { service?.selectSubtitle(id) } },
            pickAudio = pickAudio,
            pickVideo = pickVideo
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
    pickAudio: ActivityResultLauncher<String>,
    pickVideo: ActivityResultLauncher<String>,
) {
    fun formatTime(sec: Long): String {
        val s = sec % 60
        val m = (sec / 60) % 60
        val h = (sec / 3600)
        return if (h > 0)
            String.format(Locale.ROOT, "%d:%02d:%02d", h, m, s)
        else
            String.format(Locale.ROOT, "%02d:%02d", m, s)
    }

//    Column(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(16.dp),
//        verticalArrangement = Arrangement.spacedBy(12.dp)
//    ) {
        SectionCard {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPrev) {
                    Icon(
                        modifier = Modifier.tooltip(stringResource(R.string.previous)),
                        painter = painterResource(R.drawable.skip_previous_24px),
                        contentDescription = stringResource(R.string.previous)
                    )
                }
                IconButton(onClick = onBackward) {
                    Icon(
                        modifier = Modifier.tooltip(stringResource(R.string.rewind_10s)),
                        painter = painterResource(R.drawable.replay_10_24px),
                        contentDescription = stringResource(R.string.rewind_10s)
                    )
                }
                FilledIconButton(onClick = onPlayPause) {
                    if (isPlaying) {
                        Icon(
                            modifier = Modifier.tooltip(stringResource(R.string.pause)),
                            painter = painterResource(R.drawable.pause_24px),
                            contentDescription = stringResource(R.string.pause)
                        )
                    } else {
                        Icon(
                            modifier = Modifier.tooltip(stringResource(R.string.play)),
                            painter = painterResource(R.drawable.play_arrow_24px),
                            contentDescription = stringResource(R.string.play)
                        )
                    }
                }
                IconButton(onClick = onForward) {
                    Icon(
                        modifier = Modifier.tooltip(stringResource(R.string.forward_10s)),
                        painter = painterResource(R.drawable.forward_10_24px),
                        contentDescription = stringResource(R.string.forward_10s)
                    )
                }
                IconButton(onClick = onNext) {
                    Icon(
                        modifier = Modifier.tooltip(stringResource(R.string.next)),
                        painter = painterResource(R.drawable.skip_next_24px),
                        contentDescription = stringResource(R.string.next)
                    )
                }
            }
        }

        SectionCard {
            val max = duration.coerceAtLeast(1).toFloat()
            Column(Modifier.fillMaxWidth()) {
                Slider(
                    value = position.toFloat().coerceIn(0f, max),
                    onValueChange = { onSeek(it.toLong()) },
                    valueRange = 0f..max
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(formatTime(position), style = MaterialTheme.typography.labelSmall)
                    Text(formatTime(duration), style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        SectionCard {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = onNext) {
                    Icon(
                        modifier = Modifier.tooltip(stringResource(R.string.volume_down)),
                        painter = painterResource(R.drawable.volume_down_24px),
                        contentDescription = stringResource(R.string.volume_down)
                    )
                }
                Slider(
                    value = volume.toFloat(),
                    onValueChange = { onVolumeChange(it.toInt()) },
                    valueRange = 0f..100f,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onNext) {
                    Icon(
                        modifier = Modifier.tooltip(stringResource(R.string.volume_up)),
                        painter = painterResource(R.drawable.volume_up_24px),
                        contentDescription = stringResource(R.string.volume_up)
                    )
                }
            }
        }

        SectionCard {
            TrackDropdown(stringResource(R.string.audio_track), painterResource(R.drawable.album_24px), audioTracks, selectedAudio, onAudioTrackSelect)
        }
        SectionCard {
            TrackDropdown(stringResource(R.string.subtitles), painterResource(R.drawable.subtitles_24px), subtitleTracks, selectedSubtitle, onSubtitleSelect)
        }

        SectionCard {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = { pickAudio.launch("audio/*") },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(painter = painterResource(R.drawable.music_note_24px), contentDescription = null)
                    ButtonSpacer()
                    Text(R.string.select_audio)
                }
                Button(
                    onClick = { pickVideo.launch("video/*") },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(painter = painterResource(R.drawable.movie_24px), contentDescription = null)
                    ButtonSpacer()
                    Text(R.string.select_video)
                }
            }
        }
//    }
}

@Composable
fun SectionCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Box(Modifier.padding(12.dp)) {
            content()
        }
    }
}

@Composable
fun TrackDropdown(
    label: String,
    icon: Painter,
    items: List<String>,
    selected: String?,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = label, modifier = Modifier.padding(end = 8.dp))
            Text(
                "$label: ${selected ?: stringResource(R.string.none)}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Icon(Icons.Default.ArrowDropDown, contentDescription = stringResource(R.string.more), modifier = Modifier.tooltip(stringResource(R.string.more)))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(R.string.none) },
                onClick = { expanded = false; onSelect("") }
            )
            items.forEach { id ->
                DropdownMenuItem(
                    text = { Text(id) },
                    onClick = { expanded = false; onSelect(id) }
                )
            }
        }
    }
}

private fun playUri(ctx: Activity, uri: Uri?) {
    if (uri == null) {
        Toast.makeText(ctx, ctx.getString(R.string.no_file_selected), Toast.LENGTH_SHORT).show()
        return
    }

    val type = ctx.contentResolver.getType(uri) ?: "application/octet-stream"
    val dlnaService = ServiceLocator.get(DlnaServerService::class.java)
    dlnaService?.serveAndPlayUri(ctx.contentResolver, uri, type)
}
