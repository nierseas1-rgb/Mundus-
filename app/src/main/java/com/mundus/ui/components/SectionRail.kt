package com.mundus.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mundus.core.model.Section
import com.mundus.theme.LocalSectionTheme
import com.mundus.theme.SectionThemes
import com.mundus.ui.Route

private data class SectionEntry(val section: Section, val emoji: String)
private data class RouteEntry(val route: Route, val emoji: String, val label: String)

private val sections = listOf(
    SectionEntry(Section.LIVE_TV, "📡"),
    SectionEntry(Section.MOVIES, "🎬"),
    SectionEntry(Section.SERIES, "📺"),
    SectionEntry(Section.ANIME, "🌸"),
)

private val routes = listOf(
    RouteEntry(Route.CHANNELS, "▦", "Chaînes"),
    RouteEntry(Route.GUIDE, "🕒", "Guide"),
    RouteEntry(Route.SOURCES, "＋", "Sources"),
    RouteEntry(Route.PLUGINS, "🧩", "Plugins"),
    RouteEntry(Route.SETTINGS, "⚙", "Réglages"),
)

@Composable
fun SectionRail(
    activeSection: Section,
    route: Route,
    onSection: (Section) -> Unit,
    onRoute: (Route) -> Unit,
) {
    val theme = LocalSectionTheme.current
    Column(
        Modifier
            .fillMaxHeight()
            .width(190.dp)
            .background(Color.Black.copy(alpha = 0.28f))
            .padding(vertical = 20.dp, horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            "MUNDUS",
            color = theme.accent,
            fontWeight = FontWeight.Black,
            fontSize = 22.sp,
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp),
        )

        Text(
            "SECTIONS",
            color = Color.White.copy(alpha = 0.4f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 8.dp, top = 4.dp),
        )
        sections.forEach { entry ->
            val selected = entry.section == activeSection
            val accent = SectionThemes.of(entry.section).accent
            RailItem(
                emoji = entry.emoji,
                label = entry.section.label,
                selected = selected,
                accent = accent,
                onClick = { onSection(entry.section) },
            )
        }

        Spacer(Modifier.size(16.dp))
        Text(
            "NAVIGATION",
            color = Color.White.copy(alpha = 0.4f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 8.dp),
        )
        routes.forEach { entry ->
            RailItem(
                emoji = entry.emoji,
                label = entry.label,
                selected = entry.route == route,
                accent = theme.accent,
                onClick = { onRoute(entry.route) },
            )
        }
    }
}

@Composable
private fun RailItem(
    emoji: String,
    label: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
) {
    val bg = if (selected) accent.copy(alpha = 0.22f) else Color.Transparent
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(
            text = "$emoji  $label",
            color = if (selected) accent else Color.White.copy(alpha = 0.82f),
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 15.sp,
            textAlign = TextAlign.Start,
        )
    }
}
