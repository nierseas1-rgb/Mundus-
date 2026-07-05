package com.mundus.plugin

import com.mundus.data.http.Http
import kotlinx.serialization.json.Json
import java.util.UUID

/**
 * Loads plugin definitions the user adds by URL. Nothing is bundled — the catalogue of
 * available plugins is entirely user-driven (Kodi-style, but data-only and safe).
 */
class PluginRepository(private val http: Http) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /**
     * Fetch and parse a plugin from [url].
     *
     * Accepts either a JSON [PluginDefinition] document, or a direct link to an `.m3u`
     * playlist (which is auto-wrapped into an M3U plugin). This keeps "add a plugin" as
     * simple as pasting a link.
     */
    fun fetchDefinition(url: String): PluginDefinition {
        val trimmed = url.trim()

        // Pasting a MediaHubMX-family address (Vavoo/Huhu/Kool/…) spins up that connector.
        val mediaHubHosts = listOf("vavoo.", "huhu.", "kool.", "oha.", "ollytv", "doytv")
        if (mediaHubHosts.any { trimmed.contains(it, ignoreCase = true) }) {
            val host = extractOrigin(trimmed)
            val name = host.substringAfter("://").replaceFirstChar { it.uppercase() }
            return VavooConnector.definitionFor(UUID.randomUUID().toString(), name, host)
        }

        val looksLikeM3u = trimmed.substringBefore('?').endsWith(".m3u", true) ||
            trimmed.substringBefore('?').endsWith(".m3u8", true)

        if (!looksLikeM3u) {
            runCatching {
                val body = http.getText(trimmed)
                json.decodeFromString<PluginDefinition>(body)
            }.getOrNull()?.let { def ->
                return def.copy(
                    id = def.id.ifBlank { UUID.randomUUID().toString() },
                    sourceUrl = trimmed,
                )
            }
        }

        // Fallback: treat the URL as an M3U catalogue.
        val name = trimmed.substringAfterLast('/').substringBefore('?')
            .ifBlank { "Plugin M3U" }
        return PluginDefinition(
            id = UUID.randomUUID().toString(),
            name = name,
            description = "Plugin M3U ajouté par URL",
            type = PluginType.M3U,
            catalogUrl = trimmed,
            sourceUrl = trimmed,
        )
    }

    /** Extract "scheme://host" from a possibly-pathful url, defaulting to https. */
    private fun extractOrigin(url: String): String {
        var u = url.trim()
        if (!u.startsWith("http", ignoreCase = true)) u = "https://$u"
        val schemeEnd = u.indexOf("://").let { if (it < 0) return u else it }
        val host = u.substring(schemeEnd + 3).substringBefore('/').substringBefore('?')
        return u.substring(0, schemeEnd + 3) + host
    }

    /**
     * Fetch a repository's catalogue of installable add-ons. Accepts either a
     * `{ "name": ..., "addons": [ ... ] }` document or a bare array of definitions.
     */
    fun fetchCatalog(url: String): PluginCatalog {
        val body = http.getText(url.trim())
        runCatching { json.decodeFromString<PluginCatalog>(body) }
            .getOrNull()
            ?.takeIf { it.addons.isNotEmpty() }
            ?.let { return it }
        val addons = runCatching { json.decodeFromString<List<PluginDefinition>>(body) }
            .getOrDefault(emptyList())
        return PluginCatalog(name = "Dépôt", addons = addons)
    }
}
