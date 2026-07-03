package com.mundus.plugin

import android.util.Log
import com.mundus.data.http.Http

/**
 * The services Mundus hands to a plugin. Keeping plugins dependent on this narrow
 * surface (rather than on Android internals) is what will make out-of-process /
 * dynamically-loaded plugins feasible later without changing plugin code.
 */
class PluginContext(
    val http: Http,
    /** Per-source configuration the user entered (region, credentials, base url...). */
    val config: Map<String, String>,
    private val tag: String,
) {
    fun log(message: String) {
        Log.d("MundusPlugin/$tag", message)
    }

    fun configOr(key: String, default: String): String = config[key]?.ifBlank { null } ?: default
}
