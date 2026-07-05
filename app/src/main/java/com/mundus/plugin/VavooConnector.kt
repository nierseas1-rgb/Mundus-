package com.mundus.plugin

import com.mundus.core.model.Channel
import com.mundus.data.http.Http
import com.mundus.data.m3u.SectionInference
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put

/**
 * Best-effort connector for the MediaHubMX family of free services — **Vavoo.to,
 * Huhu.to, Kool.to** and clones — which all share the same backend and signing scheme.
 *
 * These don't serve a plain playlist: each stream needs a short-lived signature
 * (`mediahubmx-signature`) obtained from a signing endpoint and sent as an HTTP header,
 * with the MediaHubMX user-agent. This connector:
 *   1. lists channels from `<host>/channels`,
 *   2. at play time, requests a signature and returns the play url + required headers.
 *
 * The host is taken from [PluginDefinition.catalogUrl], so one connector serves any of
 * these services. Everything is overridable and failures degrade gracefully.
 *
 * IMPORTANT: these endpoints are unofficial and change over time; adjust via the
 * definition if a service tweaks its API.
 */
object VavooConnector {

    private const val UA = "MediaHubMX/2"
    private const val SIG_HEADER = "mediahubmx-signature"

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /** Build a definition for any MediaHubMX host (e.g. https://huhu.to). */
    fun definitionFor(id: String, name: String, host: String): PluginDefinition {
        val h = host.trim().trimEnd('/')
        return PluginDefinition(
            id = id,
            name = name,
            description = "$name — connecteur MediaHubMX (bêta).",
            type = PluginType.VAVOO,
            catalogUrl = "$h/channels",
            signUrl = "$h/vto-cluster/mediahubmx-signature.json",
            userAgent = UA,
            requiresResolution = true,
            sourceUrl = h,
        )
    }

    fun defaultDefinition(id: String): PluginDefinition =
        definitionFor(id, "Vavoo", "https://vavoo.to")

    private fun baseOf(def: PluginDefinition): String {
        val c = def.catalogUrl.trim()
        val base = if (c.contains("/channels")) c.substringBefore("/channels") else c.trimEnd('/')
        return base.ifBlank { "https://vavoo.to" }
    }

    fun loadChannels(def: PluginDefinition, http: Http, sourceId: String, sourceName: String): List<Channel> {
        val base = baseOf(def)
        val ua = def.userAgent?.ifBlank { null } ?: UA
        val body = http.getText(def.catalogUrl.ifBlank { "$base/channels" }, mapOf("User-Agent" to ua))
        val array = json.parseToJsonElement(body) as? JsonArray ?: return emptyList()

        return array.mapNotNull { el ->
            val obj = el as? JsonObject ?: return@mapNotNull null
            val name = obj.str("name") ?: return@mapNotNull null
            val id = obj.str("id") ?: name
            val group = obj.str("group") ?: obj.str("category")
            val logo = obj.str("logo") ?: obj.str("image")
            val play = obj.str("url") ?: "$base/play/$id/index.m3u8"
            Channel(
                id = "$sourceId:$id",
                name = name,
                streamUrl = play,
                logoUrl = logo,
                section = SectionInference.fromGroup(group, name),
                categoryName = group,
                sourceId = sourceId,
                sourceName = sourceName,
                requiresResolution = true,
            )
        }
    }

    fun resolve(def: PluginDefinition, http: Http, channel: Channel): PlayableStream {
        val base = baseOf(def)
        val ua = def.userAgent?.ifBlank { null } ?: UA
        val sigHeader = def.signatureHeader?.ifBlank { null } ?: SIG_HEADER
        val signUrl = def.signUrl?.ifBlank { null } ?: "$base/vto-cluster/mediahubmx-signature.json"
        val baseHeaders = mapOf("User-Agent" to ua)

        val signature = runCatching {
            val payload = buildJsonObject {
                put("url", channel.streamUrl)
                put("clientVersion", "3.0.2")
                put("language", "fr")
                put("region", "FR")
            }.toString()
            val resp = http.postJson(signUrl, payload, baseHeaders)
            val obj = json.parseToJsonElement(resp) as? JsonObject
            obj?.str("signature") ?: obj?.str("signed") ?: obj?.str("data")
        }.getOrNull()

        val headers = if (!signature.isNullOrBlank()) baseHeaders + (sigHeader to signature) else baseHeaders
        return PlayableStream(channel.streamUrl, headers)
    }

    private fun JsonObject.str(key: String): String? =
        (this[key] as? JsonPrimitive)?.contentOrNull?.ifBlank { null }
}
