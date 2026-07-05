package com.mundus.data.repo

import android.content.Context
import com.mundus.core.model.PlayerSettings
import com.mundus.core.model.Source
import com.mundus.plugin.AddonRepo
import com.mundus.plugin.PluginDefinition
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/** Everything we persist to disk, as one JSON document. */
@Serializable
data class AppState(
    val sources: List<Source> = emptyList(),
    val plugins: List<PluginDefinition> = emptyList(),
    val repos: List<AddonRepo> = emptyList(),
    val settings: PlayerSettings = PlayerSettings.Default,
    val favorites: Set<String> = emptySet(),
    val activeSectionId: String = "live_tv",
)

/**
 * Lightweight JSON-file persistence in the app's private storage. Chosen over Room
 * for the MVP to keep the build free of annotation processors; the repository
 * boundary means it can be swapped for Room/DataStore later without touching callers.
 */
class PersistentStore(context: Context) {

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = true }
    private val file = File(context.filesDir, "mundus_state.json")

    @Synchronized
    fun load(): AppState = runCatching {
        if (!file.exists()) AppState()
        else json.decodeFromString<AppState>(file.readText())
    }.getOrDefault(AppState())

    @Synchronized
    fun save(state: AppState) {
        runCatching { file.writeText(json.encodeToString(state)) }
    }
}
