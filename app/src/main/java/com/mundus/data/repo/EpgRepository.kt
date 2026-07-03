package com.mundus.data.repo

import com.mundus.core.model.ChannelGuide
import com.mundus.core.model.Source
import com.mundus.data.epg.XmltvParser
import com.mundus.data.http.Http
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Loads XMLTV guides from every source that declares an [Source.epgUrl] and exposes
 * them keyed by EPG channel id (tvg-id), ready for the timeline grid.
 */
class EpgRepository(private val http: Http) {

    @Volatile
    private var guides: Map<String, ChannelGuide> = emptyMap()

    suspend fun refresh(sources: List<Source>): Map<String, ChannelGuide> =
        withContext(Dispatchers.IO) {
            val merged = HashMap<String, ChannelGuide>()
            for (src in sources.filter { it.enabled && !it.epgUrl.isNullOrBlank() }) {
                runCatching {
                    http.client.newCall(
                        okhttp3.Request.Builder().url(src.epgUrl!!).build()
                    ).execute().use { resp ->
                        resp.body?.byteStream()?.let { stream ->
                            XmltvParser.parse(stream).forEach { (id, guide) -> merged[id] = guide }
                        }
                    }
                }
            }
            guides = merged
            merged
        }

    fun current(): Map<String, ChannelGuide> = guides

    fun guideFor(epgChannelId: String?): ChannelGuide? =
        epgChannelId?.let { guides[it] }
}
