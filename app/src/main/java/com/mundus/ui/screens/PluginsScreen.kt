package com.mundus.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mundus.core.model.Source
import com.mundus.core.model.SourceKind
import com.mundus.theme.LocalSectionTheme
import com.mundus.ui.MainViewModel
import com.mundus.ui.UiState
import java.util.UUID

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PluginsScreen(state: UiState, vm: MainViewModel) {
    val theme = LocalSectionTheme.current
    val plugins = vm.pluginManifests()

    Column(Modifier.fillMaxSize()) {
        Text("Plugins", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
        Text(
            "Des extensions façon Kodi qui ajoutent des chaînes et résolvent des flux (Vavoo, sites de streaming…).",
            color = Color.White.copy(alpha = 0.55f),
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 2.dp, bottom = 16.dp),
        )

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
            items(plugins, key = { it.id }) { manifest ->
                val installed = state.sources.any { it.pluginId == manifest.id }
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .padding(16.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "${manifest.name}  v${manifest.version}",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                            )
                            Text(manifest.description, color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
                        }
                        Text(
                            if (installed) "✓ Activé" else "Activer",
                            color = if (installed) theme.accent else Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (installed) Color.White.copy(alpha = 0.08f) else theme.accent)
                                .clickable(enabled = !installed) {
                                    vm.addSource(
                                        Source(
                                            id = UUID.randomUUID().toString(),
                                            name = manifest.name,
                                            kind = SourceKind.PLUGIN,
                                            pluginId = manifest.id,
                                        )
                                    )
                                }
                                .padding(horizontal = 16.dp, vertical = 9.dp),
                        )
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 10.dp),
                    ) {
                        manifest.sections.forEach { Badge(it.label, theme.accent) }
                        manifest.capabilities.forEach { Badge(it.name, Color.White.copy(alpha = 0.5f)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun Badge(text: String, color: Color) {
    Text(
        text,
        color = color,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}
