package com.mundus.core.model

import kotlinx.serialization.Serializable

/**
 * The kind of a content [Source].
 */
enum class SourceKind {
    /** A remote or local M3U/M3U8 playlist. */
    M3U,

    /** An Xtream Codes account (host + username + password). */
    XTREAM,

    /** Content provided by a user-installed plugin definition (e.g. Vavoo added by URL). */
    PLUGIN,
}

/**
 * A user-configured content source. Multiple sources can be enabled at once; the
 * [com.mundus.data.repo.SourceRepository] merges channels from every enabled source
 * into a single unified library, so the user never has to switch playlists manually.
 */
@Serializable
data class Source(
    val id: String,
    val name: String,
    val kind: SourceKind,
    val enabled: Boolean = true,

    // --- M3U ---
    val m3uUrl: String? = null,

    // --- Xtream Codes ---
    val xtreamHost: String? = null,
    val xtreamUsername: String? = null,
    val xtreamPassword: String? = null,

    // --- Plugin ---
    val pluginId: String? = null,
    /** Free-form config passed to the plugin (e.g. region, base url). */
    val pluginConfig: Map<String, String> = emptyMap(),

    /** Optional XMLTV EPG url used to enrich channels from this source. */
    val epgUrl: String? = null,

    /** Display order in the source list. */
    val order: Int = 0,
)
