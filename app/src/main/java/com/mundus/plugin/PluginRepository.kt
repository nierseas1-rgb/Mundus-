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

        // Pasting a Vavoo address spins up the native Vavoo connector.
        if (trimmed.contains("vavoo.", ignoreCase = true)) {
            return VavooConnector.defaultDefinition(UUID.randomUUID().toString())
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
}
