package com.mundus.plugin

import com.mundus.core.model.Channel

/**
 * The Mundus plugin contract — the "Kodi add-on" surface.
 *
 * A plugin turns some external world (Vavoo, a streaming site, a custom API) into
 * Mundus [Channel]s and, when needed, resolves a channel into a playable stream.
 *
 * Built-in plugins implement this directly. The same interface is the target for
 * externally-loaded plugins (see docs/PLUGINS.md for the roadmap on dynamic loading
 * and sandboxing); keeping the contract free of Android UI types is deliberate.
 */
interface MundusPlugin {

    val manifest: PluginManifest

    /** Called once before first use, with the user's config for this instance. */
    suspend fun init(context: PluginContext) {}

    /**
     * Return the channels this plugin currently offers. Implementations should be
     * resilient: on network failure, return what they can (possibly empty) rather
     * than throwing, so one failing plugin never breaks the merged library.
     */
    suspend fun getChannels(context: PluginContext): List<Channel>

    /**
     * Turn a channel with [Channel.requiresResolution] == true into a concrete,
     * playable url. Default implementation assumes the url is already playable.
     */
    suspend fun resolveStream(context: PluginContext, channel: Channel): ResolvedStream =
        ResolvedStream(channel.streamUrl)

    /** Optional search. Default: filter [getChannels] by name. */
    suspend fun search(context: PluginContext, query: String): List<Channel> =
        getChannels(context).filter { it.name.contains(query, ignoreCase = true) }
}
