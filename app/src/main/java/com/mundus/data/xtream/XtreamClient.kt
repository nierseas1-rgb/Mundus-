package com.mundus.data.xtream

import com.mundus.core.model.Channel
import com.mundus.core.model.Section
import com.mundus.data.http.Http
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/**
 * Talks to an Xtream Codes panel and turns its catalogue into Mundus [Channel]s.
 *
 * Live streams populate [Section.LIVE_TV]; VOD entries are routed to Movies / Series /
 * Anime by the same keyword inference used for M3U groups.
 */
class XtreamClient(private val http: Http) {

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true; isLenient = true }

    /**
     * @param baseHost full base url of the panel, e.g. "http://example.com:8080"
     */
    fun fetchAll(
        baseHost: String,
        username: String,
        password: String,
        sourceId: String,
        sourceName: String,
    ): List<Channel> {
        val base = baseHost.trimEnd('/')
        val api = "$base/player_api.php?username=$username&password=$password"

        val liveCats = categories("$api&action=get_live_categories")
        val vodCats = categories("$api&action=get_vod_categories")

        val channels = ArrayList<Channel>()

        // --- Live TV ---
        val liveStreams: List<XtreamLiveStream> =
            json.decodeFromString(http.getText("$api&action=get_live_streams"))
        for (s in liveStreams) {
            val ext = "ts" // classic Xtream live container; ExoPlayer reads MPEG-TS
            channels += Channel(
                id = "$sourceId:live:${s.streamId}",
                name = s.name,
                streamUrl = "$base/live/$username/$password/${s.streamId}.$ext",
                logoUrl = s.streamIcon.ifBlank { null },
                section = Section.LIVE_TV,
                categoryName = liveCats[s.categoryId],
                epgChannelId = s.epgChannelId?.ifBlank { null },
                sourceId = sourceId,
                sourceName = sourceName,
                number = s.num.takeIf { it > 0 },
            )
        }

        // --- VOD (movies / series / anime by inference) ---
        val vodStreams: List<XtreamVodStream> =
            json.decodeFromString(http.getText("$api&action=get_vod_streams"))
        for (s in vodStreams) {
            val group = vodCats[s.categoryId]
            channels += Channel(
                id = "$sourceId:vod:${s.streamId}",
                name = s.name,
                streamUrl = "$base/movie/$username/$password/${s.streamId}.${s.containerExtension}",
                logoUrl = s.streamIcon.ifBlank { null },
                section = com.mundus.data.m3u.SectionInference.fromGroup(group, s.name)
                    .let { if (it == Section.LIVE_TV) Section.MOVIES else it },
                categoryName = group,
                sourceId = sourceId,
                sourceName = sourceName,
            )
        }
        return channels
    }

    private fun categories(url: String): Map<String, String> = runCatching {
        json.decodeFromString<List<XtreamCategory>>(http.getText(url))
            .associate { it.categoryId to it.categoryName }
    }.getOrDefault(emptyMap())
}
