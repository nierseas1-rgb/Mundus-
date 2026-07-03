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
import com.mundus.plugin.PluginContext
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
    val allChannels: List<Channel> = emptyList(),
    val settings: PlayerSettings = PlayerSettings.Default,
    val favorites: Set<String> = emptySet(),
    val guides: Map<String, ChannelGuide> = emptyMap(),
    val loading: Boolean = false,
    val perSourceCounts: Map<String, Int> = emptyMap(),
    val errors: Map<String, String> = emptyMap(),
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
                settings = persisted.settings,
                favorites = persisted.favorites,
                activeSection = Section.fromId(persisted.activeSectionId),
            )
        }
        refresh()
    }

    fun setSection(section: Section) {
        _state.update { it.copy(activeSection = section) }
        persist()
    }

    fun refresh() {
        val sources = _state.value.sources
        if (sources.none { it.enabled }) return
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            val result = container.sourceRepository.loadAll(sources)
            val guides = container.epgRepository.refresh(sources)
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

    fun pluginManifests() = container.pluginRegistry.manifests()

    /**
     * Resolve a channel to a concrete playable url just before playback. Plugin
     * channels may need a signing/resolve step; direct urls pass through.
     */
    suspend fun resolvePlayableUrl(channel: Channel): String {
        if (!channel.requiresResolution) return channel.streamUrl
        val source = _state.value.sources.firstOrNull { it.id == channel.sourceId }
        val plugin = container.pluginRegistry.get(source?.pluginId ?: channel.sourceId)
            ?: return channel.streamUrl
        val ctx = PluginContext(container.http, source?.pluginConfig ?: emptyMap(), plugin.manifest.id)
        return withContext(Dispatchers.IO) {
            runCatching { plugin.resolveStream(ctx, channel).url }.getOrDefault(channel.streamUrl)
        }
    }

    private fun persist() {
        val s = _state.value
        container.store.save(
            AppState(
                sources = s.sources,
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
