package com.mundus.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mundus.theme.LocalSectionTheme
import com.mundus.ui.MainViewModel
import com.mundus.ui.UiState

@Composable
fun PluginsScreen(state: UiState, vm: MainViewModel) {
    val theme = LocalSectionTheme.current
    var url by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize()) {
        Text("Plugins", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
        Text(
            "Ajoutez n'importe quel plugin par URL : une définition JSON, ou un simple lien .m3u. " +
                "Rien n'est intégré — Vavoo, par exemple, s'ajoute ici via son URL, au même titre que les autres.",
            color = Color.White.copy(alpha = 0.55f),
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 2.dp, bottom = 14.dp),
        )

        // Add-by-URL
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .padding(14.dp),
        ) {
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("URL du plugin (.m3u ou définition JSON)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                Modifier.fillMaxWidth().padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                state.pluginMessage?.let {
                    Text(it, color = theme.accent, fontSize = 12.sp, modifier = Modifier.weight(1f))
                } ?: Text(
                    "Le plugin apparaîtra ci-dessous, prêt à être activé.",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "Ajouter",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(theme.accent)
                        .clickable {
                            vm.addPluginFromUrl(url)
                            url = ""
                        }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                )
            }
            Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Raccourcis :",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(end = 8.dp),
                )
                Text(
                    "＋ Vavoo (bêta)",
                    color = theme.accent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(theme.accent.copy(alpha = 0.16f))
                        .clickable { vm.addVavooPlugin() }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
        }

        Text(
            "PLUGINS INSTALLÉS",
            color = Color.White.copy(alpha = 0.4f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 18.dp, bottom = 8.dp),
        )

        if (state.plugins.isEmpty()) {
            Text(
                "Aucun plugin installé pour le moment.",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 13.sp,
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
                items(state.plugins, key = { it.id }) { def ->
                    val activated = state.sources.any { it.pluginId == def.id }
                    val count = state.sources.firstOrNull { it.pluginId == def.id }
                        ?.let { state.perSourceCounts[it.id] }
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
                                    "${def.name}  v${def.version}",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                )
                                Text(
                                    def.description.ifBlank { "${def.type} • ${def.catalogUrl}" },
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 12.sp,
                                    maxLines = 2,
                                )
                                if (count != null) {
                                    Text("$count chaînes", color = theme.accent, fontSize = 12.sp)
                                }
                            }
                            Text(
                                if (activated) "✓ Activé" else "Activer",
                                color = if (activated) theme.accent else Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (activated) Color.White.copy(alpha = 0.08f) else theme.accent)
                                    .clickable(enabled = !activated) { vm.activatePlugin(def) }
                                    .padding(horizontal = 16.dp, vertical = 9.dp),
                            )
                            Text(
                                "🗑",
                                fontSize = 18.sp,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { vm.removePlugin(def.id) }
                                    .padding(8.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
