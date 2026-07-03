package com.mundus.data.m3u

import com.mundus.core.model.Channel
import com.mundus.core.model.Section

/**
 * A tolerant `#EXTM3U` parser. Handles the common extended tags produced by IPTV
 * panels: `tvg-id`, `tvg-name`, `tvg-logo`, `tvg-chno`, `group-title`.
 *
 * The section is inferred from the group title so that a single playlist can feed
 * the Live TV / Movies / Series / Anime universes.
 */
object M3uParser {

    private val ATTR_REGEX = Regex("""([\w-]+)="([^"]*)"""")

    fun parse(content: String, sourceId: String, sourceName: String): List<Channel> {
        val channels = ArrayList<Channel>()
        val lines = content.lineSequence().iterator()

        var pending: ExtInf? = null
        while (lines.hasNext()) {
            val raw = lines.next().trim()
            when {
                raw.startsWith("#EXTM3U", ignoreCase = true) -> Unit
                raw.startsWith("#EXTINF", ignoreCase = true) -> pending = parseExtInf(raw)
                raw.isEmpty() || raw.startsWith("#") -> Unit // skip other directives/comments
                else -> {
                    val info = pending
                    pending = null
                    val url = raw
                    val name = info?.title?.ifBlank { url } ?: url
                    val group = info?.attrs?.get("group-title")
                    val logo = info?.attrs?.get("tvg-logo")
                    val epgId = info?.attrs?.get("tvg-id")?.ifBlank { null }
                    val chno = info?.attrs?.get("tvg-chno")?.toIntOrNull()
                    val streamKey = epgId ?: url
                    channels += Channel(
                        id = "$sourceId:$streamKey",
                        name = name,
                        streamUrl = url,
                        logoUrl = logo?.ifBlank { null },
                        section = SectionInference.fromGroup(group, name),
                        categoryName = group?.ifBlank { null },
                        epgChannelId = epgId,
                        sourceId = sourceId,
                        sourceName = sourceName,
                        number = chno,
                    )
                }
            }
        }
        return channels
    }

    private fun parseExtInf(line: String): ExtInf {
        // Format: #EXTINF:-1 tvg-id="..." group-title="...",Channel Name
        val commaIdx = line.indexOf(',')
        val title = if (commaIdx >= 0) line.substring(commaIdx + 1).trim() else ""
        val attrsPart = if (commaIdx >= 0) line.substring(0, commaIdx) else line
        val attrs = ATTR_REGEX.findAll(attrsPart)
            .associate { it.groupValues[1].lowercase() to it.groupValues[2] }
        return ExtInf(title = title, attrs = attrs)
    }

    private data class ExtInf(val title: String, val attrs: Map<String, String>)
}

/**
 * Heuristics that map a playlist group title to a Mundus [Section]. Deliberately
 * simple and overridable later by user rules.
 */
object SectionInference {
    fun fromGroup(group: String?, name: String): Section {
        val hay = "${group.orEmpty()} $name".lowercase()
        return when {
            listOf("anime", "animé", "anim", "manga").any { hay.contains(it) } -> Section.ANIME
            listOf("serie", "série", "tv show", "shows", "s01", "saison").any { hay.contains(it) } -> Section.SERIES
            listOf("film", "movie", "cinema", "cinéma", "vod").any { hay.contains(it) } -> Section.MOVIES
            else -> Section.LIVE_TV
        }
    }
}
