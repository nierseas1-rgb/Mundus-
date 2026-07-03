package com.mundus.plugin

import com.mundus.plugin.builtin.VavooPlugin

/**
 * Holds all known plugins. Built-ins are registered at startup; this is also the
 * insertion point for externally-loaded plugins in the future.
 */
class PluginRegistry {

    private val plugins = LinkedHashMap<String, MundusPlugin>()

    init {
        // Built-in "Kodi-style" plugins shipped with the app.
        register(VavooPlugin())
    }

    fun register(plugin: MundusPlugin) {
        plugins[plugin.manifest.id] = plugin
    }

    fun all(): List<MundusPlugin> = plugins.values.toList()

    fun get(id: String?): MundusPlugin? = id?.let { plugins[it] }

    fun manifests(): List<PluginManifest> = plugins.values.map { it.manifest }
}
