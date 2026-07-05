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
import com.mundus.plugin.PluginDefinition
import com.mundus.theme.LocalSectionTheme
import com.mundus.ui.MainViewModel
import com.mundus.ui.UiState

@Composable
fun PluginsScreen(state: UiState, vm: MainViewModel) {
    val theme = LocalSectionTheme.current
    var url by remember { mutableStateOf("") }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            Column {
                Text("Add-ons", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
                Text(
                    "Comme sur Kodi : parcourez le catalogue, installez un add-on, puis activez-le. " +
                        "Ajoutez d'autres dépôts par URL pour en avoir plus. Rien n'est actif tant que vous n'installez pas.",
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }

        // Add by URL (add-on definition) or repository
        item {
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
                    label = { Text("URL — add-on (.m3u / JSON) ou dépôt") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pill("＋ Installer l'add-on", theme.accent, filled = true) {
                        vm.addPluginFromUrl(url); url = ""
                    }
                    Pill("＋ Ajouter le dépôt", theme.accent, filled = false) {
                        vm.addRepo(url); url = ""
                    }
                }
                state.pluginMessage?.let {
                    Text(it, color = theme.accent, fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp))
                }
            }
        }

        // --- Catalogue (installable) ---
        item { SectionLabel("CATALOGUE") }
        items(state.catalogAddons, key = { "cat_${it.id}" }) { def ->
            val installed = state.plugins.any { it.id == def.id }
            CatalogRow(def = def, installed = installed, accent = theme.accent) {
                vm.installAddon(def)
            }
        }

        // --- Installed add-ons ---
        item { SectionLabel("INSTALLÉS") }
        if (state.plugins.isEmpty()) {
            item {
                Text(
                    "Aucun add-on installé. Installez-en un depuis le catalogue ci-dessus.",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 13.sp,
                )
            }
        }
        items(state.plugins, key = { "inst_${it.id}" }) { def ->
            val activated = state.sources.any { it.pluginId == def.id }
            val count = state.sources.firstOrNull { it.pluginId == def.id }
                ?.let { state.perSourceCounts[it.id] }
            InstalledRow(
                def = def,
                activated = activated,
                count = count,
                accent = theme.accent,
                onActivate = { vm.activatePlugin(def) },
                onRemove = { vm.removePlugin(def.id) },
            )
        }

        // --- Repositories ---
        item { SectionLabel("DÉPÔTS") }
        if (state.repos.isEmpty()) {
            item {
                Text(
                    "Aucun dépôt ajouté. Le catalogue intégré reste disponible.",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 13.sp,
                )
            }
        }
        items(state.repos, key = { "repo_${it.id}" }) { repo ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(repo.name, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Text(repo.url, color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp, maxLines = 1)
                }
                Text(
                    "🗑",
                    fontSize = 18.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { vm.removeRepo(repo.id) }
                        .padding(8.dp),
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        color = Color.White.copy(alpha = 0.4f),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 10.dp),
    )
}

@Composable
private fun CatalogRow(def: PluginDefinition, installed: Boolean, accent: Color, onInstall: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(def.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    "  ${def.type}",
                    color = accent.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                def.description.ifBlank { def.catalogUrl },
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp,
                maxLines = 2,
            )
        }
        Text(
            if (installed) "✓ Installé" else "Installer",
            color = if (installed) accent else Color.Black,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(if (installed) Color.White.copy(alpha = 0.08f) else accent)
                .clickable(enabled = !installed, onClick = onInstall)
                .padding(horizontal = 16.dp, vertical = 9.dp),
        )
    }
}

@Composable
private fun InstalledRow(
    def: PluginDefinition,
    activated: Boolean,
    count: Int?,
    accent: Color,
    onActivate: () -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("${def.name}  v${def.version}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(
                def.description.ifBlank { "${def.type} • ${def.catalogUrl}" },
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp,
                maxLines = 2,
            )
            if (count != null) Text("$count chaînes", color = accent, fontSize = 12.sp)
        }
        Text(
            if (activated) "✓ Activé" else "Activer",
            color = if (activated) accent else Color.Black,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(if (activated) Color.White.copy(alpha = 0.08f) else accent)
                .clickable(enabled = !activated, onClick = onActivate)
                .padding(horizontal = 16.dp, vertical = 9.dp),
        )
        Text(
            "🗑",
            fontSize = 18.sp,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onRemove)
                .padding(8.dp),
        )
    }
}

@Composable
private fun Pill(label: String, accent: Color, filled: Boolean, onClick: () -> Unit) {
    Text(
        label,
        color = if (filled) Color.Black else accent,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (filled) accent else accent.copy(alpha = 0.16f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
    )
}
