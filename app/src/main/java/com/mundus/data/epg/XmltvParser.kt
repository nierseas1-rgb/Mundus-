package com.mundus.data.epg

import android.util.Xml
import com.mundus.core.model.ChannelGuide
import com.mundus.core.model.Programme
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.util.Calendar
import java.util.TimeZone

/**
 * Streaming XMLTV parser. Builds one [ChannelGuide] per `channel` id so the EPG
 * grid can look up programmes by a channel's `tvg-id`.
 *
 * XMLTV timestamps look like `20240101203000 +0100`.
 */
object XmltvParser {

    fun parse(input: InputStream): Map<String, ChannelGuide> {
        val byChannel = HashMap<String, MutableList<Programme>>()
        input.use { stream ->
            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(stream, null)

            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG && parser.name == "programme") {
                    readProgramme(parser)?.let { p ->
                        byChannel.getOrPut(p.channelId) { ArrayList() }.add(p)
                    }
                }
                event = parser.next()
            }
        }
        return byChannel.mapValues { (id, list) ->
            ChannelGuide(id, list.sortedBy { it.startMs })
        }
    }

    private fun readProgramme(parser: XmlPullParser): Programme? {
        val channel = parser.getAttributeValue(null, "channel") ?: return null
        val start = parseXmltvTime(parser.getAttributeValue(null, "start")) ?: return null
        val stop = parseXmltvTime(parser.getAttributeValue(null, "stop")) ?: (start + 30 * 60_000)

        var title = ""
        var desc: String? = null
        var category: String? = null

        var event = parser.next()
        while (!(event == XmlPullParser.END_TAG && parser.name == "programme")) {
            if (event == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "title" -> title = parser.nextText()
                    "desc" -> desc = parser.nextText()
                    "category" -> if (category == null) category = parser.nextText()
                    else -> skip(parser)
                }
            }
            event = parser.next()
        }
        return Programme(
            channelId = channel,
            title = title.ifBlank { "Programme" },
            description = desc,
            startMs = start,
            stopMs = stop,
            category = category,
        )
    }

    private fun skip(parser: XmlPullParser) {
        if (parser.eventType != XmlPullParser.START_TAG) return
        var depth = 1
        while (depth != 0) {
            when (parser.next()) {
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.START_TAG -> depth++
            }
        }
    }

    /** Parses `YYYYMMDDHHMMSS[ +ZZZZ]` into epoch millis. */
    fun parseXmltvTime(raw: String?): Long? {
        if (raw.isNullOrBlank()) return null
        val s = raw.trim()
        val datePart = s.takeWhile { it.isDigit() }
        if (datePart.length < 14) return null
        return runCatching {
            val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            cal.clear()
            cal.set(
                datePart.substring(0, 4).toInt(),
                datePart.substring(4, 6).toInt() - 1,
                datePart.substring(6, 8).toInt(),
                datePart.substring(8, 10).toInt(),
                datePart.substring(10, 12).toInt(),
                datePart.substring(12, 14).toInt(),
            )
            var ms = cal.timeInMillis
            // Apply the trailing "+HHMM" / "-HHMM" offset if present.
            val tz = s.drop(datePart.length).trim()
            if (tz.length >= 5 && (tz[0] == '+' || tz[0] == '-')) {
                val sign = if (tz[0] == '-') 1 else -1 // subtract offset to reach UTC
                val h = tz.substring(1, 3).toInt()
                val m = tz.substring(3, 5).toInt()
                ms += sign * (h * 3600_000L + m * 60_000L)
            }
            ms
        }.getOrNull()
    }
}
