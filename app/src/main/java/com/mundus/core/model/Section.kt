package com.mundus.core.model

/**
 * A [Section] is a top-level content universe in Mundus. Each section gets its own
 * theme (see [com.mundus.theme.SectionThemes]) and its own aggregated content feed.
 *
 * Sections are the backbone of the "thème d'affichage en fonction de la section"
 * requirement: switching the active section re-skins the whole UI.
 */
enum class Section(
    val id: String,
    val label: String,
) {
    LIVE_TV("live_tv", "TV en direct"),
    MOVIES("movies", "Films"),
    SERIES("series", "Séries"),
    ANIME("anime", "Animés");

    companion object {
        fun fromId(id: String?): Section = entries.firstOrNull { it.id == id } ?: LIVE_TV
    }
}
