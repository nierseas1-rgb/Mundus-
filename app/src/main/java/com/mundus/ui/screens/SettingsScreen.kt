package com.mundus.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mundus.core.model.PlayerSettings
import com.mundus.theme.LocalSectionTheme
import com.mundus.ui.MainViewModel
import com.mundus.ui.UiState

@Composable
fun SettingsScreen(state: UiState, vm: MainViewModel) {
    val s = state.settings

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            Column {
                Text("Réglages", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
                Text(
                    "Options de lecture classiques : mémoire tampon, délais, décodage.",
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = 13.sp,
                )
            }
        }

        item {
            SettingCard("Mémoire tampon") {
                SliderRow(
                    label = "Tampon minimum",
                    valueMs = s.minBufferMs,
                    range = 2_000f..120_000f,
                    onChange = { vm.updateSettings(s.copy(minBufferMs = it)) },
                )
                SliderRow(
                    label = "Tampon maximum",
                    valueMs = s.maxBufferMs,
                    range = 10_000f..600_000f,
                    onChange = { vm.updateSettings(s.copy(maxBufferMs = it)) },
                )
                SliderRow(
                    label = "Tampon avant lecture (zapping)",
                    valueMs = s.bufferForPlaybackMs,
                    range = 500f..30_000f,
                    onChange = { vm.updateSettings(s.copy(bufferForPlaybackMs = it)) },
                )
                SliderRow(
                    label = "Tampon après coupure",
                    valueMs = s.bufferForPlaybackAfterRebufferMs,
                    range = 1_000f..60_000f,
                    onChange = { vm.updateSettings(s.copy(bufferForPlaybackAfterRebufferMs = it)) },
                )
            }
        }

        item {
            SettingCard("Réseau") {
                SliderRow(
                    label = "Délai de connexion",
                    valueMs = s.connectTimeoutMs,
                    range = 2_000f..60_000f,
                    onChange = { vm.updateSettings(s.copy(connectTimeoutMs = it)) },
                )
                SliderRow(
                    label = "Délai de lecture",
                    valueMs = s.readTimeoutMs,
                    range = 2_000f..60_000f,
                    onChange = { vm.updateSettings(s.copy(readTimeoutMs = it)) },
                )
                OutlinedTextField(
                    value = s.userAgent,
                    onValueChange = { vm.updateSettings(s.copy(userAgent = it)) },
                    label = { Text("User-Agent") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
        }

        item {
            SettingCard("Lecture") {
                ToggleRow("Reconnexion automatique", s.autoReconnect) {
                    vm.updateSettings(s.copy(autoReconnect = it))
                }
                ToggleRow("Préférer le décodage matériel", s.preferHardwareDecoding) {
                    vm.updateSettings(s.copy(preferHardwareDecoding = it))
                }
            }
        }

        item {
            Text(
                "Mundus v0.1 — projet ouvert. Les plugins étendent les sources façon Kodi.",
                color = Color.White.copy(alpha = 0.35f),
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun SettingCard(title: String, content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .padding(16.dp),
    ) {
        Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Column(Modifier.padding(top = 8.dp)) { content() }
    }
}

@Composable
private fun SliderRow(label: String, valueMs: Int, range: ClosedFloatingPointRange<Float>, onChange: (Int) -> Unit) {
    val theme = LocalSectionTheme.current
    Column(Modifier.padding(vertical = 6.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = Color.White.copy(alpha = 0.85f), fontSize = 14.sp, modifier = Modifier.weight(1f))
            Text("${valueMs / 1000f} s", color = theme.accent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
        Slider(
            value = valueMs.toFloat().coerceIn(range),
            onValueChange = { onChange(it.toInt()) },
            valueRange = range,
        )
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = Color.White.copy(alpha = 0.85f), fontSize = 14.sp, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
