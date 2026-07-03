package com.mundus.core.model

import kotlinx.serialization.Serializable

/**
 * A single playable item: a live TV channel, a movie (VOD) or a series episode.
 *
 * Channels coming from different sources are merged into one library. [sourceId]
 * keeps the origin so the UI can show badges and de-duplication can prefer one
 * source over another.
 */
@Serializable
data class Channel(
    /** Stable id, unique across all sources (usually "<sourceId>:<streamKey>"). */
    val id: String,
    val name: String,

    /** Direct, playable stream url. May be resolved lazily for plugin channels. */
    val streamUrl: String,

    val logoUrl: String? = null,

    /** The section this channel belongs to (drives theming and top-level nav). */
    val section: Section = Section.LIVE_TV,

    /** Category / group name as declared by the source ("group-title" in M3U). */
    val categoryName: String? = null,

    /** tvg-id used to match this channel against XMLTV EPG data. */
    val epgChannelId: String? = null,

    /** Which [Source] produced this channel. */
    val sourceId: String,

    /** Human-readable source label, for badges in the merged library. */
    val sourceName: String = "",

    /** True when the stream url must be resolved through a plugin before playback. */
    val requiresResolution: Boolean = false,

    /** Channel number, when the source provides one (M3U "tvg-chno"). */
    val number: Int? = null,
)
