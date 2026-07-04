package com.mundus.plugin

import kotlinx.serialization.Serializable

/** How a plugin exposes its catalogue. */
enum class PluginType { M3U, JSON, XTREAM }

/**
 * Field mapping for [PluginType.JSON] plugins: which JSON keys hold the channel
 * name / url / logo / group. Each entry is a list of candidate keys, tried in order,
 * so one engine can read many differently-shaped feeds.
 */
@Serializable
data class JsonFieldMap(
    /** Optional key holding the array of items (e.g. "channels"); null = root is the array. */
    val root: String? = null,
    val name: List<String> = listOf("name", "title"),
    val url: List<String> = listOf("url", "stream", "link"),
    val logo: List<String> = listOf("logo", "icon", "image"),
    val group: List<String> = listOf("group", "category", "genre"),
    val id: List<String> = listOf("id"),
)

/**
 * A **user-installable** plugin, described entirely by data — no native code, nothing
 * baked into the app. The user adds one by pasting a URL that points either to a JSON
 * document of this shape, or directly to an `.m3u` playlist (auto-wrapped as an M3U
 * plugin). Vavoo is simply one such definition among any number the user may add.
 */
@Serializable
data class PluginDefinition(
    val id: String,
    val name: String,
    val version: String = "1.0.0",
    val author: String = "",
    val description: String = "",

    val type: PluginType = PluginType.M3U,

    /** Where the catalogue lives (M3U playlist url, JSON feed url, or Xtream base host). */
    val catalogUrl: String = "",

    /** Optional endpoint that turns a raw stream url into a fresh/playable one. */
    val resolverUrl: String? = null,
    /** True when streams must be passed through [resolverUrl] before playback. */
    val requiresResolution: Boolean = false,

    /** Optional HTTP headers some sources require. */
    val userAgent: String? = null,
    val referer: String? = null,

    /** JSON field mapping (used when [type] == JSON). */
    val json: JsonFieldMap = JsonFieldMap(),

    /** Xtream credentials (used when [type] == XTREAM). */
    val xtreamUsername: String? = null,
    val xtreamPassword: String? = null,

    /** The URL this definition was fetched from, kept so it can be refreshed. */
    val sourceUrl: String? = null,
) {
    fun headers(): Map<String, String> = buildMap {
        userAgent?.takeIf { it.isNotBlank() }?.let { put("User-Agent", it) }
        referer?.takeIf { it.isNotBlank() }?.let { put("Referer", it) }
    }
}
