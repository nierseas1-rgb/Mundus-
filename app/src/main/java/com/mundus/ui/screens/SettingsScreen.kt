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
import kotlin.math.roundToInt

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
                    "Mémoire tampon et latence réglables de 1 à 10 pour rendre les flux fluides.",
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = 13.sp,
                )
            }
        }

        item {
            SettingCard("Mémoire tampon (1 → 10)") {
                LevelRow(
                    label = "Tampon minimum",
                    hint = "Plus haut = démarrage plus sûr",
                    level = s.minBufferLevel,
                    approxSeconds = s.minBufferMs / 1000f,
                    onChange = { vm.updateSettings(s.copy(minBufferLevel = it)) },
                )
                LevelRow(
                    label = "Tampon maximum",
                    hint = "Plus haut = plus fluide, plus de RAM",
                    level = s.maxBufferLevel,
                    approxSeconds = s.maxBufferMs / 1000f,
                    onChange = { vm.updateSettings(s.copy(maxBufferLevel = it)) },
                )
                LevelRow(
                    label = "Latence / démarrage",
                    hint = "Bas = zapping rapide • Haut = plus stable",
                    level = s.latencyLevel,
                    approxSeconds = s.bufferForPlaybackMs / 1000f,
                    onChange = { vm.updateSettings(s.copy(latencyLevel = it)) },
                )
                Text(
                    "Astuce : pour Vavoo / Huhu.to / Kool.to, montez « Latence » et « Tampon max » " +
                        "à 7–9 pour un rendu proche du vrai IPTV.",
                    color = Color.White.copy(alpha = 0.45f),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }

        item {
            SettingCard("Réseau") {
                SecondsRow(
                    label = "Délai de connexion",
                    valueMs = s.connectTimeoutMs,
                    range = 2_000f..60_000f,
                    onChange = { vm.updateSettings(s.copy(connectTimeoutMs = it)) },
                )
                SecondsRow(
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
                "Mundus v0.1 — projet ouvert. Les add-ons étendent les sources façon Kodi.",
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
private fun LevelRow(
    label: String,
    hint: String,
    level: Int,
    approxSeconds: Float,
    onChange: (Int) -> Unit,
) {
    val theme = LocalSectionTheme.current
    Column(Modifier.padding(vertical = 8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(label, color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text(hint, color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp)
            }
            Text(
                "$level/10",
                color = theme.accent,
                fontWeight = FontWeight.Black,
                fontSize = 16.sp,
            )
        }
        Slider(
            value = level.toFloat(),
            onValueChange = { onChange(it.roundToInt().coerceIn(1, 10)) },
            valueRange = 1f..10f,
            steps = 8,
        )
        Text(
            "≈ ${"%.1f".format(approxSeconds)} s",
            color = Color.White.copy(alpha = 0.4f),
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun SecondsRow(
    label: String,
    valueMs: Int,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Int) -> Unit,
) {
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
