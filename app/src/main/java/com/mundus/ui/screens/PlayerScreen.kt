package com.mundus.ui.screens

import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.mundus.core.model.Channel
import com.mundus.player.PlayerFactory
import com.mundus.ui.MainViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * A TiViMate-style full-screen player:
 *  - custom auto-hiding OSD (no default ExoPlayer controls),
 *  - bottom info bar with logo / number / name / source and now+next EPG with progress,
 *  - channel zapping (previous / next) through the list you launched from,
 *  - a channel-list panel over the video,
 *  - aspect-ratio cycling (fit / zoom / fill).
 */
@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    channels: List<Channel>,
    startIndex: Int,
    vm: MainViewModel,
    onClose: () -> Unit,
) {
    if (channels.isEmpty()) {
        onClose()
        return
    }

    val context = LocalContext.current
    val state by vm.state.collectAsState()
    val settings = remember { vm.state.value.settings }
    val exo = remember { PlayerFactory.create(context, settings) }
    val timeFmt = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    var index by remember { mutableIntStateOf(startIndex.coerceIn(0, channels.lastIndex)) }
    val channel = channels[index]

    var controlsVisible by remember { mutableStateOf(true) }
    var showChannelList by remember { mutableStateOf(false) }
    var buffering by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var resizeMode by remember { mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Keep "now" fresh for EPG progress.
    LaunchedEffect(Unit) {
        while (true) {
            nowMs = System.currentTimeMillis()
            delay(30_000)
        }
    }

    DisposableEffect(exo) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                buffering = playbackState == Player.STATE_BUFFERING
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlayerError(e: PlaybackException) {
                error = e.message ?: "Erreur de lecture"
            }
        }
        exo.addListener(listener)
        onDispose {
            exo.removeListener(listener)
            exo.release()
        }
    }

    // Load / switch channel.
    LaunchedEffect(index) {
        error = null
        buffering = true
        controlsVisible = true
        val url = runCatching { vm.resolvePlayableUrl(channel) }.getOrElse {
            error = it.message; null
        }
        if (url != null) {
            exo.setMediaItem(MediaItem.fromUri(url))
            exo.prepare()
            exo.playWhenReady = true
        }
    }

    // Auto-hide the OSD.
    LaunchedEffect(controlsVisible, index) {
        if (controlsVisible && !showChannelList) {
            delay(4500)
            controlsVisible = false
        }
    }

    fun zap(delta: Int) {
        index = (index + delta + channels.size) % channels.size
    }

    BackHandler {
        if (showChannelList) showChannelList = false else onClose()
    }

    val noRipple = remember { MutableInteractionSource() }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(interactionSource = noRipple, indication = null) {
                if (showChannelList) showChannelList = false else controlsVisible = !controlsVisible
            },
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exo
                    useController = false
                    this.resizeMode = resizeMode
                }
            },
            update = { it.resizeMode = resizeMode },
            modifier = Modifier.fillMaxSize(),
        )

        if (buffering && error == null) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.align(Alignment.Center))
        }
        error?.let {
            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("⚠ $it", color = Color(0xFFE0736B), fontSize = 15.sp)
                Text(
                    "Réessayer",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White)
                        .clickable { exo.prepare(); error = null }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }

        // --- OSD ---
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
        ) {
            InfoBar(
                channel = channel,
                guide = state.guides[channel.epgChannelId],
                nowMs = nowMs,
                total = channels.size,
                position = index + 1,
                isPlaying = isPlaying,
                timeFmt = timeFmt,
                onPrev = { zap(-1) },
                onNext = { zap(1) },
                onPlayPause = {
                    if (exo.isPlaying) exo.pause() else exo.play()
                    controlsVisible = true
                },
                onAspect = {
                    resizeMode = when (resizeMode) {
                        AspectRatioFrameLayout.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                        else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                    }
                    controlsVisible = true
                },
                onChannelList = { showChannelList = true; controlsVisible = true },
                onClose = onClose,
            )
        }

        // --- Channel list panel over the video ---
        AnimatedVisibility(
            visible = showChannelList,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.CenterEnd),
        ) {
            ChannelListPanel(
                channels = channels,
                currentIndex = index,
                onPick = { i -> index = i; showChannelList = false },
                onDismiss = { showChannelList = false },
            )
        }
    }
}

@Composable
private fun InfoBar(
    channel: Channel,
    guide: com.mundus.core.model.ChannelGuide?,
    nowMs: Long,
    total: Int,
    position: Int,
    isPlaying: Boolean,
    timeFmt: SimpleDateFormat,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onPlayPause: () -> Unit,
    onAspect: () -> Unit,
    onChannelList: () -> Unit,
    onClose: () -> Unit,
) {
    val now = guide?.nowPlaying(nowMs)
    val next = guide?.programmes?.firstOrNull { it.startMs >= (now?.stopMs ?: nowMs) }

    Column(
        Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.92f))))
            .padding(start = 24.dp, end = 24.dp, top = 40.dp, bottom = 20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Logo
            Box(
                Modifier.size(56.dp).clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center,
            ) {
                if (!channel.logoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = channel.logoUrl,
                        contentDescription = channel.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(50.dp),
                    )
                } else {
                    Text(channel.name.take(2).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${channel.number ?: position}",
                        color = Color.White.copy(alpha = 0.55f),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        channel.name,
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 22.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    "${channel.sourceName}  •  chaîne $position / $total",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp,
                )
            }
        }

        // Now / next EPG
        if (now != null) {
            Spacer(Modifier.height(12.dp))
            Text(
                "${timeFmt.format(Date(now.startMs))}  ${now.title}",
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            LinearProgressIndicator(
                progress = { now.progressAt(nowMs) },
                color = Color.White,
                trackColor = Color.White.copy(alpha = 0.2f),
                modifier = Modifier.fillMaxWidth().height(4.dp).padding(top = 6.dp).clip(RoundedCornerShape(2.dp)),
            )
            if (next != null) {
                Text(
                    "à suivre  ${timeFmt.format(Date(next.startMs))}  ${next.title}",
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }

        // Controls
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            CtrlButton("⏮") { onPrev() }
            Spacer(Modifier.width(10.dp))
            CtrlButton(if (isPlaying) "⏸" else "▶") { onPlayPause() }
            Spacer(Modifier.width(10.dp))
            CtrlButton("⏭") { onNext() }
            Spacer(Modifier.weight(1f))
            CtrlButton("⛶") { onAspect() }
            Spacer(Modifier.width(10.dp))
            CtrlButton("☰") { onChannelList() }
            Spacer(Modifier.width(10.dp))
            CtrlButton("✕") { onClose() }
        }
    }
}

@Composable
private fun CtrlButton(label: String, onClick: () -> Unit) {
    Text(
        label,
        color = Color.White,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    )
}

@Composable
private fun ChannelListPanel(
    channels: List<Channel>,
    currentIndex: Int,
    onPick: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(Unit) {
        listState.scrollToItem(currentIndex.coerceAtLeast(0))
    }
    Column(
        Modifier
            .width(340.dp)
            .fillMaxHeight()
            .background(Color.Black.copy(alpha = 0.86f))
            .padding(vertical = 16.dp, horizontal = 12.dp),
    ) {
            Text(
                "Chaînes",
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
                modifier = Modifier.padding(start = 8.dp, bottom = 10.dp),
            )
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                itemsIndexed(channels, key = { _, it -> it.id }) { i, ch ->
                    val selected = i == currentIndex
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selected) Color.White.copy(alpha = 0.18f) else Color.Transparent)
                            .clickable { onPick(i) }
                            .padding(horizontal = 10.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "${ch.number ?: (i + 1)}",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(34.dp),
                        )
                        Text(
                            ch.name,
                            color = if (selected) Color.White else Color.White.copy(alpha = 0.82f),
                            fontSize = 14.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
