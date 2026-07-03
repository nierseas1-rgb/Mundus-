package com.mundus.ui.screens

import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import com.mundus.core.model.Channel
import com.mundus.player.PlayerFactory
import com.mundus.ui.MainViewModel

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(channel: Channel, vm: MainViewModel, onClose: () -> Unit) {
    val context = LocalContext.current
    val settings = remember { vm.state.value.settings }
    val exo = remember { PlayerFactory.create(context, settings) }
    var resolving by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(channel.id) {
        resolving = true
        error = null
        val url = runCatching { vm.resolvePlayableUrl(channel) }
            .getOrElse { error = it.message; null }
        if (url != null) {
            exo.setMediaItem(MediaItem.fromUri(url))
            exo.prepare()
            exo.playWhenReady = true
        }
        resolving = false
    }

    DisposableEffect(Unit) {
        onDispose { exo.release() }
    }

    BackHandler(onBack = onClose)

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exo
                    useController = true
                    setShowNextButton(false)
                    setShowPreviousButton(false)
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        // Top overlay: channel name + close
        Box(Modifier.fillMaxSize().padding(20.dp)) {
            Column(Modifier.align(Alignment.TopStart)) {
                Text(channel.name, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(channel.sourceName, color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
            }
            Text(
                "✕",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(onClick = onClose)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }

        if (resolving) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.align(Alignment.Center))
        }
        error?.let {
            Text(
                "Erreur de lecture : $it",
                color = Color(0xFFE0736B),
                modifier = Modifier.align(Alignment.Center).padding(24.dp),
            )
        }
    }
}
