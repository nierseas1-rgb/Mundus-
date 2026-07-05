package com.mundus.data.http

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Shared OkHttp client + tiny helpers used by parsers, the Xtream client and plugins.
 * Kept intentionally small; plugins receive this through their PluginContext.
 */
class Http(
    private val defaultUserAgent: String = "Mundus/0.1 (Android)",
) {
    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .retryOnConnectionFailure(true)
        .build()

    /** Blocking GET returning the body as text. Call from a background dispatcher. */
    fun getText(url: String, headers: Map<String, String> = emptyMap()): String {
        val req = Request.Builder()
            .url(url)
            .header("User-Agent", defaultUserAgent)
            .apply { headers.forEach { (k, v) -> header(k, v) } }
            .build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) error("HTTP ${resp.code} for $url")
            return resp.body?.string() ?: ""
        }
    }

    /** Blocking POST of a JSON body, returning the response body as text. */
    fun postJson(url: String, json: String, headers: Map<String, String> = emptyMap()): String {
        val body = json.toRequestBody("application/json; charset=utf-8".toMediaType())
        val req = Request.Builder()
            .url(url)
            .post(body)
            .header("User-Agent", defaultUserAgent)
            .apply { headers.forEach { (k, v) -> header(k, v) } }
            .build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) error("HTTP ${resp.code} for $url")
            return resp.body?.string() ?: ""
        }
    }
}
