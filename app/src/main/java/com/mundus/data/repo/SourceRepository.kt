package com.mundus.data.repo

import com.mundus.core.model.Channel
import com.mundus.core.model.Source
import com.mundus.core.model.SourceKind
import com.mundus.data.http.Http
import com.mundus.data.m3u.M3uParser
import com.mundus.data.xtream.XtreamClient
import com.mundus.plugin.PluginContext
import com.mundus.plugin.PluginRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

/**
 * Fetches every enabled [Source] and merges the results into a single channel
 * library. This is what lets the user run several playlists at once and browse all
 * channels together, without ever switching between playlists.
 *
 * Sources are loaded in parallel; a failing source degrades gracefully (it simply
 * contributes no channels) instead of breaking the whole library.
 */
class SourceRepository(
    private val http: Http,
    private val xtream: XtreamClient,
    private val plugins: PluginRegistry,
) {
    data class LoadResult(
        val channels: List<Channel>,
        val perSource: Map<String, Int>,
        val errors: Map<String, String>,
    )

    suspend fun loadAll(sources: List<Source>): LoadResult = coroutineScope {
        val enabled = sources.filter { it.enabled }
        val jobs = enabled.map { source ->
            async(Dispatchers.IO) { source.id to runCatching { load(source) } }
        }
        val results = jobs.awaitAll()

        val merged = ArrayList<Channel>()
        val perSource = LinkedHashMap<String, Int>()
        val errors = LinkedHashMap<String, String>()
        for ((id, res) in results) {
            res.onSuccess { list ->
                perSource[id] = list.size
                merged += list
            }.onFailure { errors[id] = it.message ?: "Erreur inconnue" }
        }
        LoadResult(channels = dedupe(merged), perSource = perSource, errors = errors)
    }

    private suspend fun load(source: Source): List<Channel> = when (source.kind) {
        SourceKind.M3U -> {
            val url = source.m3uUrl ?: return emptyList()
            M3uParser.parse(http.getText(url), source.id, source.name)
        }

        SourceKind.XTREAM -> xtream.fetchAll(
            baseHost = source.xtreamHost.orEmpty(),
            username = source.xtreamUsername.orEmpty(),
            password = source.xtreamPassword.orEmpty(),
            sourceId = source.id,
            sourceName = source.name,
        )

        SourceKind.PLUGIN -> {
            val plugin = plugins.get(source.pluginId) ?: return emptyList()
            val ctx = PluginContext(http, source.pluginConfig, plugin.manifest.id)
            withContext(Dispatchers.IO) {
                plugin.init(ctx)
                plugin.getChannels(ctx)
                    // Re-stamp origin so badges reflect the user's source name.
                    .map { it.copy(sourceId = source.id, sourceName = source.name) }
            }
        }
    }

    /** Drop exact duplicates (same playable url) that appear across playlists. */
    private fun dedupe(channels: List<Channel>): List<Channel> {
        val seen = HashSet<String>()
        val out = ArrayList<Channel>(channels.size)
        for (c in channels) {
            val key = c.streamUrl.ifBlank { c.id }
            if (seen.add(key)) out += c
        }
        return out
    }
}
