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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.mundus.core.model.Source
import com.mundus.core.model.SourceKind
import com.mundus.theme.LocalSectionTheme
import com.mundus.ui.MainViewModel
import com.mundus.ui.UiState
import java.util.UUID

@Composable
fun SourcesScreen(state: UiState, vm: MainViewModel) {
    val theme = LocalSectionTheme.current
    var showAdd by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text("Sources", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
                Text(
                    "Toutes les sources activées sont fusionnées en une seule bibliothèque.",
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = 13.sp,
                )
            }
            Text(
                "＋ Ajouter",
                color = Color.Black,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(theme.accent)
                    .clickable { showAdd = true }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            )
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize().padding(top = 16.dp),
        ) {
            items(state.sources, key = { it.id }) { source ->
                SourceRow(
                    source = source,
                    count = state.perSourceCounts[source.id],
                    error = state.errors[source.id],
                    accent = theme.accent,
                    onToggle = { vm.toggleSource(source.id) },
                    onDelete = { vm.removeSource(source.id) },
                )
            }
            if (state.sources.isEmpty()) {
                item {
                    Text(
                        "Aucune source. Ajoutez une playlist M3U, un compte Xtream Codes, ou activez le plugin Vavoo.",
                        color = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.padding(24.dp),
                    )
                }
            }
        }
    }

    if (showAdd) {
        AddSourceDialog(
            onDismiss = { showAdd = false },
            onAdd = { vm.addSource(it); showAdd = false },
        )
    }
}

@Composable
private fun SourceRow(
    source: Source,
    count: Int?,
    error: String?,
    accent: Color,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(source.name, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            val subtitle = when (source.kind) {
                SourceKind.M3U -> "M3U • ${source.m3uUrl}"
                SourceKind.XTREAM -> "Xtream • ${source.xtreamHost}"
                SourceKind.PLUGIN -> "Plugin • ${source.pluginId}"
            }
            Text(subtitle, color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp, maxLines = 1)
            when {
                error != null -> Text("⚠ $error", color = Color(0xFFE0736B), fontSize = 12.sp)
                count != null -> Text("$count chaînes", color = accent, fontSize = 12.sp)
            }
        }
        Switch(checked = source.enabled, onCheckedChange = { onToggle() })
        Text(
            "🗑",
            fontSize = 18.sp,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onDelete)
                .padding(8.dp),
        )
    }
}

@Composable
private fun AddSourceDialog(onDismiss: () -> Unit, onAdd: (Source) -> Unit) {
    var kind by remember { mutableStateOf(SourceKind.M3U) }
    var name by remember { mutableStateOf("") }
    var m3uUrl by remember { mutableStateOf("") }
    var host by remember { mutableStateOf("") }
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var epgUrl by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val id = UUID.randomUUID().toString()
                val src: Source? = when (kind) {
                    SourceKind.M3U -> Source(
                        id = id,
                        name = name.ifBlank { "Playlist M3U" },
                        kind = kind,
                        m3uUrl = m3uUrl.trim(),
                        epgUrl = epgUrl.trim().ifBlank { null },
                    )
                    SourceKind.XTREAM -> Source(
                        id = id,
                        name = name.ifBlank { "Compte Xtream" },
                        kind = kind,
                        xtreamHost = host.trim(),
                        xtreamUsername = user.trim(),
                        xtreamPassword = pass.trim(),
                        epgUrl = epgUrl.trim().ifBlank { null },
                    )
                    // Plugins are activated from the Plugins tab, not here.
                    SourceKind.PLUGIN -> null
                }
                if (src != null) onAdd(src)
            }) { Text("Ajouter") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
        title = { Text("Ajouter une source") },
        text = {
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KindTab("M3U", kind == SourceKind.M3U) { kind = SourceKind.M3U }
                    KindTab("Xtream", kind == SourceKind.XTREAM) { kind = SourceKind.XTREAM }
                }
                Field("Nom", name) { name = it }
                when (kind) {
                    SourceKind.XTREAM -> {
                        Field("Hôte (http://serveur:port)", host) { host = it }
                        Field("Utilisateur", user) { user = it }
                        Field("Mot de passe", pass) { pass = it }
                        Field("URL EPG XMLTV (optionnel)", epgUrl) { epgUrl = it }
                    }
                    else -> {
                        Field("URL M3U", m3uUrl) { m3uUrl = it }
                        Field("URL EPG XMLTV (optionnel)", epgUrl) { epgUrl = it }
                    }
                }
            }
        },
    )
}

@Composable
private fun KindTab(label: String, selected: Boolean, onClick: () -> Unit) {
    val theme = LocalSectionTheme.current
    Text(
        label,
        color = if (selected) Color.Black else Color.White.copy(alpha = 0.8f),
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) theme.accent else Color.White.copy(alpha = 0.1f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

@Composable
private fun Field(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    )
}
