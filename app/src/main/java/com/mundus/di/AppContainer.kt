package com.mundus.di

import android.content.Context
import com.mundus.data.http.Http
import com.mundus.data.repo.EpgRepository
import com.mundus.data.repo.PersistentStore
import com.mundus.data.repo.SourceRepository
import com.mundus.data.xtream.XtreamClient
import com.mundus.plugin.PluginRegistry

/**
 * Manual dependency container (no Hilt/kapt on purpose — keeps the build simple and
 * annotation-processor-free). Constructed once by [com.mundus.MundusApp] and reused.
 */
class AppContainer(context: Context) {
    val http = Http()
    val xtreamClient = XtreamClient(http)
    val pluginRegistry = PluginRegistry()
    val store = PersistentStore(context.applicationContext)

    val sourceRepository = SourceRepository(http, xtreamClient, pluginRegistry)
    val epgRepository = EpgRepository(http)
}
