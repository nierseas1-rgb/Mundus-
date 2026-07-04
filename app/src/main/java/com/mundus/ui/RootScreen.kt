package com.mundus.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.mundus.core.model.Channel
import com.mundus.theme.LocalSectionTheme
import com.mundus.theme.MundusTheme
import com.mundus.ui.components.SectionRail
import com.mundus.ui.screens.ChannelsScreen
import com.mundus.ui.screens.GuideScreen
import com.mundus.ui.screens.PlayerScreen
import com.mundus.ui.screens.PluginsScreen
import com.mundus.ui.screens.SettingsScreen
import com.mundus.ui.screens.SourcesScreen

enum class Route { CHANNELS, GUIDE, SOURCES, PLUGINS, SETTINGS }

/** A playback request: the channel list to zap through and the starting index. */
data class Playback(val channels: List<Channel>, val index: Int)

@Composable
fun RootScreen(vm: MainViewModel) {
    val state by vm.state.collectAsState()
    var route by remember { mutableStateOf(Route.CHANNELS) }
    var playback by remember { mutableStateOf<Playback?>(null) }

    MundusTheme(section = state.activeSection) {
        val theme = LocalSectionTheme.current
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(theme.gradient))) {
            Row(Modifier.fillMaxSize()) {
                SectionRail(
                    activeSection = state.activeSection,
                    route = route,
                    onSection = { vm.setSection(it) },
                    onRoute = { route = it },
                )
                Box(Modifier.weight(1f).fillMaxSize().padding(24.dp)) {
                    when (route) {
                        Route.CHANNELS -> ChannelsScreen(state, vm, onPlay = { list, i -> playback = Playback(list, i) })
                        Route.GUIDE -> GuideScreen(state, onPlay = { list, i -> playback = Playback(list, i) })
                        Route.SOURCES -> SourcesScreen(state, vm)
                        Route.PLUGINS -> PluginsScreen(state, vm)
                        Route.SETTINGS -> SettingsScreen(state, vm)
                    }
                }
            }

            playback?.let { pb ->
                PlayerScreen(
                    channels = pb.channels,
                    startIndex = pb.index,
                    vm = vm,
                    onClose = { playback = null },
                )
            }
        }
    }
}
