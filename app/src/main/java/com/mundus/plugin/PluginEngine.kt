package com.mundus.plugin

import com.mundus.core.model.Channel
import com.mundus.data.http.Http
import com.mundus.data.m3u.M3uParser
import com.mundus.data.m3u.SectionInference
import com.mundus.data.xtream.XtreamClient
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * Runs a [PluginDefinition]: turns it into channels and resolves streams. This is the
 * generic, data-driven replacement for hard-coded plugins — any plugin the user adds
 * flows through here, whatever its type.
 */
object PluginEngine {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun loadChannels(
        def: PluginDefinition,
        http: Http,
        sourceId: String,
        sourceName: String,
    ): List<Channel> = when (def.type) {
        PluginType.M3U -> {
            val body = http.getText(def.catalogUrl, def.headers())
            M3uParser.parse(body, sourceId, sourceName)
                .map { it.copy(requiresResolution = def.requiresResolution) }
        }

        PluginType.XTREAM -> XtreamClient(http).fetchAll(
            baseHost = def.catalogUrl,
            username = def.xtreamUsername.orEmpty(),
            password = def.xtreamPassword.orEmpty(),
            sourceId = sourceId,
            sourceName = sourceName,
        )

        PluginType.JSON -> {
            val body = http.getText(def.catalogUrl, def.headers())
            parseJson(def, body, sourceId, sourceName)
        }
    }

    /** Resolve a channel to a concrete playable url, if the plugin needs it. */
    fun resolve(def: PluginDefinition, http: Http, channel: Channel): String {
        val resolver = def.resolverUrl?.takeIf { it.isNotBlank() } ?: return channel.streamUrl
        return runCatching {
            http.getText("$resolver${channel.streamUrl}", def.headers()).trim()
                .ifBlank { channel.streamUrl }
        }.getOrDefault(channel.streamUrl)
    }

    private fun parseJson(
        def: PluginDefinition,
        body: String,
        sourceId: String,
        sourceName: String,
    ): List<Channel> {
        val root = json.parseToJsonElement(body)
        val array: JsonArray = when {
            root is JsonArray -> root
            root is JsonObject && def.json.root != null -> root[def.json.root] as? JsonArray
                ?: return emptyList()
            root is JsonObject -> root.values.firstOrNull { it is JsonArray } as? JsonArray
                ?: return emptyList()
            else -> return emptyList()
        }
        val map = def.json
        return array.mapNotNull { el ->
            val obj = el as? JsonObject ?: return@mapNotNull null
            val name = obj.first(map.name) ?: return@mapNotNull null
            val url = obj.first(map.url) ?: return@mapNotNull null
            val group = obj.first(map.group)
            val logo = obj.first(map.logo)
            val id = obj.first(map.id) ?: url
            Channel(
                id = "$sourceId:$id",
                name = name,
                streamUrl = url,
                logoUrl = logo,
                section = SectionInference.fromGroup(group, name),
                categoryName = group,
                sourceId = sourceId,
                sourceName = sourceName,
                requiresResolution = def.requiresResolution,
            )
        }
    }

    private fun JsonObject.first(keys: List<String>): String? {
        for (k in keys) {
            val v = (this[k] as? JsonPrimitive)?.contentOrNull
            if (!v.isNullOrBlank()) return v
        }
        return null
    }
}
