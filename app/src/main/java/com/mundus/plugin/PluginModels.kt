package com.mundus.plugin

import com.mundus.core.model.Channel
import com.mundus.core.model.Section

/** What a plugin is able to do. Used by the UI to decide how to present it. */
enum class PluginCapability {
    /** Provides its own catalogue of channels (e.g. Vavoo). */
    PROVIDES_CHANNELS,

    /** Can turn a not-directly-playable channel into a concrete stream url. */
    RESOLVES_STREAMS,

    /** Exposes a search endpoint. */
    SEARCH,
}

/** Static description of a plugin, shown in the plugin manager. */
data class PluginManifest(
    val id: String,
    val name: String,
    val version: String,
    val author: String = "",
    val description: String = "",
    val sections: Set<Section> = setOf(Section.LIVE_TV),
    val capabilities: Set<PluginCapability> = emptySet(),
    /** Config keys the plugin understands, with human labels for the settings UI. */
    val configSchema: Map<String, String> = emptyMap(),
)

/** The outcome of [MundusPlugin.resolveStream]. */
data class ResolvedStream(
    val url: String,
    val headers: Map<String, String> = emptyMap(),
)
