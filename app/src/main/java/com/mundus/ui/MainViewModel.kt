package com.mundus.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mundus.core.model.Category
import com.mundus.core.model.Channel
import com.mundus.core.model.ChannelGuide
import com.mundus.core.model.PlayerSettings
import com.mundus.core.model.Section
import com.mundus.core.model.Source
import com.mundus.data.repo.AppState
import com.mundus.di.AppContainer
import com.mundus.plugin.AddonRepo
import com.mundus.plugin.BuiltInCatalog
import com.mundus.plugin.PlayableStream
import com.mundus.plugin.PluginDefinition
import com.mundus.plugin.PluginEngine
import com.mundus.plugin.PluginType
import com.mundus.plugin.VavooConnector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class UiState(
    val activeSection: Section = Section.LIVE_TV,
    val sources: List<Source> = emptyList(),
    val plugins: List<PluginDefinition> = emptyList(),
    val repos: List<AddonRepo> = emptyList(),
    /** Add-ons available to install (built-in catalogue + repositories). */
    val catalogAddons: List<PluginDefinition> = emptyList(),
    val allChannels: List<Channel> = emptyList(),
    val settings: PlayerSettings = PlayerSettings.Default,
    val favorites: Set<String> = emptySet(),
    val guides: Map<String, ChannelGuide> = emptyMap(),
    val loading: Boolean = false,
    val perSourceCounts: Map<String, Int> = emptyMap(),
    val errors: Map<String, String> = emptyMap(),
    /** Transient message for the plugin screen (add success/failure). */
    val pluginMessage: String? = null,
) {
    /** Channels of the active section (the merged, multi-playlist library). */
    val sectionChannels: List<Channel>
        get() = allChannels.filter { it.section == activeSection }

    val categories: List<Category>
        get() = sectionChannels
            .groupBy { it.categoryName ?: "Autres" }
            .map { (name, list) ->
                Category(id = name, name = name, section = activeSection, channelCount = list.size)
            }
            .sortedByDescending { it.channelCount }
}

class MainViewModel(private val container: AppContainer) : ViewModel() {

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        val persisted = container.store.load()
        _state.update {
            it.copy(
                sources = persisted.sources,
                plugins = persisted.plugins,
                repos = persisted.repos,
                catalogAddons = BuiltInCatalog.addons(),
                settings = persisted.settings,
                favorites = persisted.favorites,
                activeSection = Section.fromId(persisted.activeSectionId),
            )
        }
        refresh()
        refreshCatalog()
    }

    fun setSection(section: Section) {
        _state.update { it.copy(activeSection = section) }
        persist()
    }

    fun refresh() {
        val s = _state.value
        if (s.sources.none { it.enabled }) return
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            val result = container.sourceRepository.loadAll(s.sources, s.plugins)
            val guides = container.epgRepository.refresh(s.sources)
            _state.update {
                it.copy(
                    loading = false,
                    allChannels = result.channels,
                    perSourceCounts = result.perSource,
                    errors = result.errors,
                    guides = guides,
                )
            }
        }
    }

    fun addSource(source: Source) {
        _state.update { it.copy(sources = it.sources + source.copy(order = it.sources.size)) }
        persist()
        refresh()
    }

    fun removeSource(id: String) {
        _state.update { st ->
            st.copy(
                sources = st.sources.filterNot { it.id == id },
                allChannels = st.allChannels.filterNot { it.sourceId == id },
            )
        }
        persist()
    }

    fun toggleSource(id: String) {
        _state.update { st ->
            st.copy(sources = st.sources.map {
                if (it.id == id) it.copy(enabled = !it.enabled) else it
            })
        }
        persist()
        refresh()
    }

    // --- Plugins (user-installable, nothing bundled) ---

    /** Add a plugin from a URL (JSON definition or direct .m3u). */
    fun addPluginFromUrl(url: String) {
        if (url.isBlank()) return
        viewModelScope.launch {
            _state.update { it.copy(pluginMessage = "Chargement du plugin…") }
            val result = withContext(Dispatchers.IO) {
                runCatching { container.pluginRepository.fetchDefinition(url) }
            }
            result.onSuccess { def ->
                _state.update {
                    val others = it.plugins.filterNot { p -> p.id == def.id }
                    it.copy(plugins = others + def, pluginMessage = "Plugin ajouté : ${def.name}")
                }
                persist()
            }.onFailure { e ->
                _state.update { it.copy(pluginMessage = "Échec : ${e.message ?: "URL invalide"}") }
            }
        }
    }

    /** One-tap add of the native Vavoo connector (VAVOO type needs no URL). */
    fun addVavooPlugin() {
        val def = VavooConnector.defaultDefinition(java.util.UUID.randomUUID().toString())
        _state.update {
            val others = it.plugins.filterNot { p -> p.type == PluginType.VAVOO }
            it.copy(plugins = others + def, pluginMessage = "Vavoo ajouté — activez-le ci-dessous.")
        }
        persist()
    }

    fun removePlugin(id: String) {
        _state.update { st ->
            st.copy(
                plugins = st.plugins.filterNot { it.id == id },
                sources = st.sources.filterNot { it.pluginId == id },
                allChannels = st.allChannels.filterNot { c ->
                    st.sources.any { it.id == c.sourceId && it.pluginId == id }
                },
            )
        }
        persist()
    }

    fun clearPluginMessage() {
        _state.update { it.copy(pluginMessage = null) }
    }

    // --- Add-on catalogue / repositories (Kodi-style) ---

    /** Re-fetch every repository and merge with the built-in catalogue. */
    fun refreshCatalog() {
        val repos = _state.value.repos
        viewModelScope.launch {
            val repoAddons = withContext(Dispatchers.IO) {
                repos.flatMap { repo ->
                    runCatching { container.pluginRepository.fetchCatalog(repo.url).addons }
                        .getOrDefault(emptyList())
                }
            }
            _state.update { it.copy(catalogAddons = mergeCatalog(BuiltInCatalog.addons(), repoAddons)) }
        }
    }

    /** Add an add-on repository by URL and merge its catalogue. */
    fun addRepo(url: String) {
        if (url.isBlank()) return
        viewModelScope.launch {
            _state.update { it.copy(pluginMessage = "Chargement du dépôt…") }
            val result = withContext(Dispatchers.IO) {
                runCatching { container.pluginRepository.fetchCatalog(url) }
            }
            result.onSuccess { cat ->
                val repo = AddonRepo(java.util.UUID.randomUUID().toString(), cat.name, url.trim())
                _state.update {
                    val repos = it.repos.filterNot { r -> r.url == repo.url } + repo
                    it.copy(
                        repos = repos,
                        catalogAddons = mergeCatalog(it.catalogAddons, cat.addons),
                        pluginMessage = "Dépôt ajouté : ${cat.name} (${cat.addons.size} add-ons)",
                    )
                }
                persist()
            }.onFailure { e ->
                _state.update { it.copy(pluginMessage = "Échec du dépôt : ${e.message ?: "URL invalide"}") }
            }
        }
    }

    fun removeRepo(id: String) {
        _state.update { it.copy(repos = it.repos.filterNot { r -> r.id == id }) }
        persist()
        refreshCatalog()
    }

    /** Install an add-on from the catalogue (adds its definition to installed plugins). */
    fun installAddon(def: PluginDefinition) {
        _state.update {
            val others = it.plugins.filterNot { p -> p.id == def.id }
            it.copy(plugins = others + def, pluginMessage = "Add-on installé : ${def.name}")
        }
        persist()
    }

    private fun mergeCatalog(
        a: List<PluginDefinition>,
        b: List<PluginDefinition>,
    ): List<PluginDefinition> {
        val map = LinkedHashMap<String, PluginDefinition>()
        (a + b).forEach { map[it.id] = it }
        return map.values.toList()
    }

    /** Activate an installed plugin as a merged source. */
    fun activatePlugin(def: PluginDefinition) {
        if (_state.value.sources.any { it.pluginId == def.id }) return
        addSource(
            Source(
                id = java.util.UUID.randomUUID().toString(),
                name = def.name,
                kind = com.mundus.core.model.SourceKind.PLUGIN,
                pluginId = def.id,
            )
        )
    }

    fun updateSettings(settings: PlayerSettings) {
        _state.update { it.copy(settings = PlayerSettings.sanitize(settings)) }
        persist()
    }

    fun toggleFavorite(channelId: String) {
        _state.update { st ->
            val favs = st.favorites.toMutableSet()
            if (!favs.add(channelId)) favs.remove(channelId)
            st.copy(favorites = favs)
        }
        persist()
    }

    fun channelById(id: String): Channel? = _state.value.allChannels.firstOrNull { it.id == id }

    /**
     * Resolve a channel to a concrete playable stream (url + headers) just before
     * playback. Plugin channels may need a resolve/signature step; direct urls pass
     * through with no extra headers.
     */
    suspend fun resolvePlayable(channel: Channel): PlayableStream {
        if (!channel.requiresResolution) return PlayableStream(channel.streamUrl)
        val s = _state.value
        val source = s.sources.firstOrNull { it.id == channel.sourceId }
        val def = s.plugins.firstOrNull { it.id == source?.pluginId }
            ?: return PlayableStream(channel.streamUrl)
        return withContext(Dispatchers.IO) {
            runCatching { PluginEngine.resolve(def, container.http, channel) }
                .getOrDefault(PlayableStream(channel.streamUrl))
        }
    }

    private fun persist() {
        val s = _state.value
        container.store.save(
            AppState(
                sources = s.sources,
                plugins = s.plugins,
                repos = s.repos,
                settings = s.settings,
                favorites = s.favorites,
                activeSectionId = s.activeSection.id,
            )
        )
    }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            MainViewModel(container) as T
    }
}
