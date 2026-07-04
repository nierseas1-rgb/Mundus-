package com.mundus.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items as rowItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import com.mundus.core.model.Channel
import com.mundus.theme.LocalSectionTheme
import com.mundus.ui.MainViewModel
import com.mundus.ui.UiState
import com.mundus.ui.components.ChannelCard

@Composable
fun ChannelsScreen(
    state: UiState,
    vm: MainViewModel,
    onPlay: (List<Channel>, Int) -> Unit,
) {
    val theme = LocalSectionTheme.current
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf<String?>(null) }
    var favoritesOnly by remember { mutableStateOf(false) }
    val nowMs = remember { System.currentTimeMillis() }

    // Reset the category filter when the section changes.
    androidx.compose.runtime.LaunchedEffect(state.activeSection) {
        category = null
        favoritesOnly = false
    }

    val channels = state.sectionChannels
        .filter { category == null || it.categoryName == category }
        .filter { !favoritesOnly || state.favorites.contains(it.id) }
        .filter { query.isBlank() || it.name.contains(query, ignoreCase = true) }

    Column(Modifier.fillMaxSize()) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text(
                    state.activeSection.label,
                    color = Color.White,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    "${state.sectionChannels.size} chaînes • ${state.sources.count { it.enabled }} source(s) fusionnée(s)",
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = 13.sp,
                )
            }
            if (state.loading) {
                CircularProgressIndicator(color = theme.accent, strokeWidth = 3.dp, modifier = Modifier.padding(end = 12.dp))
            }
            Chip("↻ Actualiser", theme.accent) { vm.refresh() }
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Rechercher une chaîne…", color = Color.White.copy(alpha = 0.4f)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
        )

        // Category chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            item {
                FilterChip("Tout", selected = category == null && !favoritesOnly, theme.accent) {
                    category = null; favoritesOnly = false
                }
            }
            item {
                FilterChip("★ Favoris", selected = favoritesOnly, theme.accent) {
                    favoritesOnly = !favoritesOnly
                }
            }
            rowItems(state.categories) { cat ->
                FilterChip("${cat.name} (${cat.channelCount})", selected = category == cat.name, theme.accent) {
                    category = if (category == cat.name) null else cat.name
                    favoritesOnly = false
                }
            }
        }

        if (channels.isEmpty()) {
            EmptyState(state)
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 300.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                itemsIndexed(channels, key = { _, it -> it.id }) { index, channel ->
                    ChannelCard(
                        channel = channel,
                        guide = state.guides[channel.epgChannelId],
                        nowMs = nowMs,
                        isFavorite = state.favorites.contains(channel.id),
                        onClick = { onPlay(channels, index) },
                        onToggleFavorite = { vm.toggleFavorite(channel.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState(state: UiState) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                if (state.sources.isEmpty()) "Aucune source configurée" else "Aucune chaîne dans cette section",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                if (state.sources.isEmpty())
                    "Ajoutez une playlist M3U, un compte Xtream ou activez un plugin dans « Sources »."
                else "Essayez une autre section ou ajoutez d'autres sources.",
                color = Color.White.copy(alpha = 0.45f),
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun Chip(label: String, accent: Color, onClick: () -> Unit) {
    Text(
        label,
        color = accent,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(accent.copy(alpha = 0.16f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    )
}

@Composable
private fun FilterChip(label: String, selected: Boolean, accent: Color, onClick: () -> Unit) {
    val bg = if (selected) accent.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.08f)
    val fg = if (selected) Color.Black else Color.White.copy(alpha = 0.85f)
    Text(
        label,
        color = fg,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}
