package com.mundus.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mundus.core.model.Channel
import com.mundus.core.model.Programme
import com.mundus.theme.LocalSectionTheme
import com.mundus.ui.UiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val PX_PER_MIN = 4 // dp per minute of programming
private const val WINDOW_HOURS = 6
private val LABEL_WIDTH = 180.dp

@Composable
fun GuideScreen(state: UiState, onPlay: (Channel) -> Unit) {
    val theme = LocalSectionTheme.current
    val hScroll = rememberScrollState()
    val timeFmt = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    val nowMs = remember { System.currentTimeMillis() }
    val startMs = remember(nowMs) { nowMs - (nowMs % (30 * 60_000L)) } // floor to 30 min
    val endMs = startMs + WINDOW_HOURS * 3600_000L

    // Show channels of the active section; EPG-matched ones first.
    val channels = remember(state.activeSection, state.allChannels) {
        state.sectionChannels.sortedByDescending { state.guides.containsKey(it.epgChannelId) }
    }

    Column(Modifier.fillMaxSize()) {
        Text(
            "Guide — ${state.activeSection.label}",
            color = Color.White,
            fontSize = 26.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        Text(
            "Fenêtre ${timeFmt.format(Date(startMs))} → ${timeFmt.format(Date(endMs))} • défilez horizontalement",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 12.dp),
        )

        // Time ruler
        Row(Modifier.fillMaxWidth()) {
            Spacer(Modifier.width(LABEL_WIDTH))
            Row(Modifier.horizontalScroll(hScroll)) {
                var t = startMs
                while (t < endMs) {
                    Text(
                        timeFmt.format(Date(t)),
                        color = Color.White.copy(alpha = 0.55f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width((30 * PX_PER_MIN).dp).padding(start = 4.dp),
                    )
                    t += 30 * 60_000L
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        if (channels.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Aucune chaîne dans cette section.", color = Color.White.copy(alpha = 0.6f))
            }
            return
        }

        LazyColumn(Modifier.fillMaxSize()) {
            items(channels, key = { it.id }) { channel ->
                GuideRow(
                    channel = channel,
                    programmes = state.guides[channel.epgChannelId]?.programmes.orEmpty(),
                    startMs = startMs,
                    endMs = endMs,
                    nowMs = nowMs,
                    hScroll = hScroll,
                    accent = theme.accent,
                    timeFmt = timeFmt,
                    onPlay = { onPlay(channel) },
                )
            }
        }
    }
}

@Composable
private fun GuideRow(
    channel: Channel,
    programmes: List<Programme>,
    startMs: Long,
    endMs: Long,
    nowMs: Long,
    hScroll: androidx.compose.foundation.ScrollState,
    accent: Color,
    timeFmt: SimpleDateFormat,
    onPlay: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Fixed channel label
        Box(
            Modifier
                .width(LABEL_WIDTH)
                .fillMaxSize()
                .clip(RoundedCornerShape(10.dp))
                .background(Color.Black.copy(alpha = 0.30f))
                .clickable(onClick = onPlay)
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                (channel.number?.let { "$it  " } ?: "") + channel.name,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        // Scrollable programme lane
        Row(
            Modifier
                .padding(start = 6.dp)
                .horizontalScroll(hScroll)
                .height(58.dp),
        ) {
            val visible = programmes.filter { it.stopMs > startMs && it.startMs < endMs }
            if (visible.isEmpty()) {
                ProgrammeBlock(
                    title = "Pas d'info EPG",
                    widthDp = ((endMs - startMs) / 60_000L * PX_PER_MIN).toInt().dp,
                    live = false,
                    progress = 0f,
                    subtitle = null,
                    accent = accent,
                    onClick = onPlay,
                )
            } else {
                for (p in visible) {
                    val vs = p.startMs.coerceAtLeast(startMs)
                    val ve = p.stopMs.coerceAtMost(endMs)
                    val minutes = ((ve - vs) / 60_000L).coerceAtLeast(4)
                    val live = p.isLiveAt(nowMs)
                    ProgrammeBlock(
                        title = p.title,
                        widthDp = (minutes * PX_PER_MIN).toInt().dp,
                        live = live,
                        progress = if (live) p.progressAt(nowMs) else 0f,
                        subtitle = "${timeFmt.format(Date(p.startMs))}",
                        accent = accent,
                        onClick = onPlay,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProgrammeBlock(
    title: String,
    widthDp: Dp,
    live: Boolean,
    progress: Float,
    subtitle: String?,
    accent: Color,
    onClick: () -> Unit,
) {
    val bg = if (live) accent.copy(alpha = 0.28f) else Color.White.copy(alpha = 0.06f)
    Column(
        Modifier
            .width(widthDp)
            .fillMaxSize()
            .padding(end = 3.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(
                width = if (live) 1.dp else 0.dp,
                color = if (live) accent else Color.Transparent,
                shape = RoundedCornerShape(8.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        Text(
            title,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = if (live) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (subtitle != null) {
            Text(subtitle, color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
        }
    }
}
