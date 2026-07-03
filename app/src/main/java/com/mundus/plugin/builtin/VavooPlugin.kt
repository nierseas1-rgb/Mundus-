package com.mundus.plugin.builtin

import com.mundus.core.model.Channel
import com.mundus.core.model.Section
import com.mundus.data.m3u.SectionInference
import com.mundus.plugin.MundusPlugin
import com.mundus.plugin.PluginCapability
import com.mundus.plugin.PluginContext
import com.mundus.plugin.PluginManifest
import com.mundus.plugin.ResolvedStream
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Example built-in plugin that surfaces a Vavoo-style channel catalogue.
 *
 * Design notes:
 *  - The catalogue endpoint is **configurable** (`catalog_url`) rather than hard-coded,
 *    because such endpoints move and their exact shape varies. We parse defensively.
 *  - Streams are exposed with [Channel.requiresResolution] = true and resolved lazily,
 *    so the (sometimes signed/short-lived) playback url is only fetched at play time.
 *  - If the network fails, we fall back to a tiny sample set so the UI stays usable
 *    and the plugin flow is demonstrable offline. Replace/point `catalog_url` at a
 *    real source to get live content.
 */
class VavooPlugin : MundusPlugin {

    override val manifest = PluginManifest(
        id = "vavoo",
        name = "Vavoo",
        version = "0.1.0",
        author = "Mundus",
        description = "Catalogue de chaînes façon Vavoo (URL de catalogue configurable).",
        sections = setOf(Section.LIVE_TV, Section.MOVIES, Section.SERIES, Section.ANIME),
        capabilities = setOf(
            PluginCapability.PROVIDES_CHANNELS,
            PluginCapability.RESOLVES_STREAMS,
            PluginCapability.SEARCH,
        ),
        configSchema = mapOf(
            "catalog_url" to "URL du catalogue JSON",
            "resolver_url" to "URL de résolution des flux (optionnel)",
            "user_agent" to "User-Agent à utiliser (optionnel)",
        ),
    )

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    override suspend fun getChannels(context: PluginContext): List<Channel> {
        val url = context.config["catalog_url"]?.ifBlank { null } ?: return sample()
        val ua = context.config["user_agent"]?.ifBlank { null }
        val headers = if (ua != null) mapOf("User-Agent" to ua) else emptyMap()

        return runCatching {
            val body = context.http.getText(url, headers)
            parseCatalog(body)
        }.getOrElse {
            context.log("Vavoo catalog fetch failed: ${it.message}; using sample data")
            sample()
        }
    }

    override suspend fun resolveStream(context: PluginContext, channel: Channel): ResolvedStream {
        val resolver = context.config["resolver_url"]?.ifBlank { null }
            ?: return ResolvedStream(channel.streamUrl)
        // A real resolver typically signs/pings the source url and returns a fresh one.
        return runCatching {
            val resolved = context.http.getText("$resolver?url=${channel.streamUrl}").trim()
            ResolvedStream(resolved.ifBlank { channel.streamUrl })
        }.getOrElse { ResolvedStream(channel.streamUrl) }
    }

    private fun parseCatalog(body: String): List<Channel> {
        val root = json.parseToJsonElement(body)
        val array = (root as? JsonArray)
            ?: (root as? JsonObject)?.get("channels") as? JsonArray
            ?: return emptyList()

        return array.mapNotNull { el ->
            val obj = el as? JsonObject ?: return@mapNotNull null
            val name = obj.str("name") ?: obj.str("title") ?: return@mapNotNull null
            val streamUrl = obj.str("url") ?: obj.str("stream") ?: return@mapNotNull null
            val group = obj.str("group") ?: obj.str("category")
            val logo = obj.str("logo") ?: obj.str("icon")
            val id = obj.str("id") ?: streamUrl
            Channel(
                id = "vavoo:$id",
                name = name,
                streamUrl = streamUrl,
                logoUrl = logo,
                section = SectionInference.fromGroup(group, name),
                categoryName = group,
                sourceId = "vavoo",
                sourceName = "Vavoo",
                requiresResolution = true,
            )
        }
    }

    private fun JsonObject.str(key: String): String? =
        (this[key] as? kotlinx.serialization.json.JsonPrimitive)?.contentOrNull?.ifBlank { null }

    /** Offline-friendly demo data so the plugin flow is visible without a live endpoint. */
    private fun sample(): List<Channel> = listOf(
        demo("vavoo-demo-1", "Vavoo Demo — Actualités", "Info"),
        demo("vavoo-demo-2", "Vavoo Demo — Sport", "Sport"),
        demo("vavoo-demo-3", "Vavoo Demo — Cinéma", "Films"),
        demo("vavoo-demo-4", "Vavoo Demo — Anime TV", "Animés"),
    )

    private fun demo(id: String, name: String, group: String) = Channel(
        id = "vavoo:$id",
        name = name,
        // Public test HLS stream, so "playable MVP" works out of the box.
        streamUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
        section = SectionInference.fromGroup(group, name),
        categoryName = group,
        sourceId = "vavoo",
        sourceName = "Vavoo",
        requiresResolution = false,
    )
}
