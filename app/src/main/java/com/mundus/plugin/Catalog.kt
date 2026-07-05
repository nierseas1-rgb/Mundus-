package com.mundus.plugin

import kotlinx.serialization.Serializable

/**
 * An add-on repository the user has added (Kodi-style). Its [url] returns a
 * [PluginCatalog] (or a bare JSON array of [PluginDefinition]).
 */
@Serializable
data class AddonRepo(
    val id: String,
    val name: String,
    val url: String,
)

/** A list of installable add-ons, as returned by a repository. */
@Serializable
data class PluginCatalog(
    val name: String = "Dépôt",
    val addons: List<PluginDefinition> = emptyList(),
)

/**
 * A small, curated catalogue that ships with the app so the Add-ons screen has
 * something to browse out of the box — just like Kodi's default repository.
 *
 * Nothing here is active until the user explicitly **installs** then **activates** it,
 * so this stays consistent with "nothing bundled/native". Vavoo is one entry among
 * several; the free IPTV-Org playlists are public, legal channels.
 */
object BuiltInCatalog {
    fun addons(): List<PluginDefinition> = listOf(
        VavooConnector.definitionFor("builtin-vavoo", "Vavoo", "https://vavoo.to"),
        VavooConnector.definitionFor("builtin-huhu", "Huhu.to", "https://huhu.to"),
        VavooConnector.definitionFor("builtin-kool", "Kool.to", "https://kool.to"),
        m3u(
            "builtin-iptvorg-fr", "IPTV-Org France",
            "Chaînes publiques françaises (gratuit, iptv-org)",
            "https://iptv-org.github.io/iptv/countries/fr.m3u",
        ),
        m3u(
            "builtin-iptvorg-news", "IPTV-Org Actualités",
            "Chaînes d'information du monde (gratuit)",
            "https://iptv-org.github.io/iptv/categories/news.m3u",
        ),
        m3u(
            "builtin-iptvorg-movies", "IPTV-Org Cinéma",
            "Chaînes cinéma/films publiques (gratuit)",
            "https://iptv-org.github.io/iptv/categories/movies.m3u",
        ),
        m3u(
            "builtin-iptvorg-animation", "IPTV-Org Animation",
            "Chaînes animation/animés publiques (gratuit)",
            "https://iptv-org.github.io/iptv/categories/animation.m3u",
        ),
    )

    private fun m3u(id: String, name: String, desc: String, url: String) = PluginDefinition(
        id = id,
        name = name,
        description = desc,
        type = PluginType.M3U,
        catalogUrl = url,
        sourceUrl = url,
    )
}
