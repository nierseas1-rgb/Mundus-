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
 * Best-effort connector for Vavoo (vavoo.to).
 *
 * Vavoo doesn't serve a plain playlist: each stream needs a short-lived signature
 * (`mediahubmx-signature`) obtained from a signing endpoint and sent as an HTTP header,
 * together with the MediaHubMX user-agent. This connector:
 *   1. lists channels from the catalogue endpoint,
 *   2. at play time, requests a signature and returns the play url + required headers.
 *
 * IMPORTANT: Vavoo's endpoints/params are unofficial and change over time. Everything
 * here is overridable via the [PluginDefinition] so it can be fixed without a new build,
 * and failures degrade gracefully (the channel list still loads; a stream that can't be
 * signed is attempted with just the user-agent).
 */
object VavooConnector {

    private const val DEFAULT_CATALOG = "https://vavoo.to/channels"
    private const val DEFAULT_SIGN = "https://vavoo.to/vto-cluster/mediahubmx-signature.json"
    private const val DEFAULT_UA = "MediaHubMX/2"
    private const val DEFAULT_SIG_HEADER = "mediahubmx-signature"

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun defaultDefinition(id: String): PluginDefinition = PluginDefinition(
        id = id,
        name = "Vavoo",
        description = "Chaînes Vavoo (connecteur natif, bêta).",
        type = PluginType.VAVOO,
        catalogUrl = DEFAULT_CATALOG,
        signUrl = DEFAULT_SIGN,
        userAgent = DEFAULT_UA,
        requiresResolution = true,
        sourceUrl = "vavoo.to",
    )

    fun loadChannels(def: PluginDefinition, http: Http, sourceId: String, sourceName: String): List<Channel> {
        val catalog = def.catalogUrl.ifBlank { DEFAULT_CATALOG }
        val ua = def.userAgent?.ifBlank { null } ?: DEFAULT_UA
        val body = http.getText(catalog, mapOf("User-Agent" to ua))
        val array = json.parseToJsonElement(body) as? JsonArray ?: return emptyList()

        return array.mapNotNull { el ->
            val obj = el as? JsonObject ?: return@mapNotNull null
            val name = obj.str("name") ?: return@mapNotNull null
            val id = obj.str("id") ?: name
            val group = obj.str("group") ?: obj.str("category")
            val logo = obj.str("logo") ?: obj.str("image")
            // Some catalogues include a ready url; otherwise derive the play url.
            val play = obj.str("url") ?: "https://vavoo.to/play/$id/index.m3u8"
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
        val ua = def.userAgent?.ifBlank { null } ?: DEFAULT_UA
        val sigHeader = def.signatureHeader?.ifBlank { null } ?: DEFAULT_SIG_HEADER
        val signUrl = def.signUrl?.ifBlank { null } ?: DEFAULT_SIGN
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

        val headers = if (!signature.isNullOrBlank()) {
            baseHeaders + (sigHeader to signature)
        } else {
            baseHeaders
        }
        return PlayableStream(channel.streamUrl, headers)
    }

    private fun JsonObject.str(key: String): String? =
        (this[key] as? JsonPrimitive)?.contentOrNull?.ifBlank { null }
}
