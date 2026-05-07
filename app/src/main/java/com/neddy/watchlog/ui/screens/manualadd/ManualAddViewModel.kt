package com.neddy.watchlog.ui.screens.manualadd

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.neddy.watchlog.data.local.entity.MediaItemEntity
import com.neddy.watchlog.data.local.entity.SeasonInfoEntity
import com.neddy.watchlog.data.preferences.UserPreferencesRepository
import com.neddy.watchlog.data.remote.TvdbRepository
import com.neddy.watchlog.data.remote.TvdbSearchItem
import com.neddy.watchlog.data.repository.MediaRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SeasonEntry(val season: Int, val episodes: Int)

data class ManualAddUiState(
    val title: String = "",
    val description: String = "",
    val mediaType: String = "Movie",
    val posterUrl: String = "",
    val seasonEntries: List<SeasonEntry> = emptyList(),
    val titleError: String? = null,
    val isDuplicate: Boolean = false,
    val isEditing: Boolean = false,
    val isSaving: Boolean = false,
    val apiId: String? = null
)

@OptIn(FlowPreview::class)
class ManualAddViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val repository = MediaRepository.getInstance(application)
    private val prefsRepository = UserPreferencesRepository.getInstance(application)
    private val tvdbRepository = TvdbRepository()

    private val defaultEpisodes = prefsRepository.defaultEpisodesPerSeason
        .stateIn(viewModelScope, SharingStarted.Eagerly, 10)

    private val editMediaId: Long? =
        savedStateHandle.get<Long>("mediaId")?.takeIf { it != -1L }

    private val _uiState = MutableStateFlow(ManualAddUiState())
    val uiState: StateFlow<ManualAddUiState> = _uiState.asStateFlow()

    private val titleInputFlow = MutableSharedFlow<String>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private val _searchSuggestions = MutableStateFlow<List<TvdbSearchItem>>(emptyList())
    val searchSuggestions: StateFlow<List<TvdbSearchItem>> = _searchSuggestions.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    init {
        editMediaId?.let { id ->
            viewModelScope.launch {
                repository.getMediaById(id)?.let { media ->
                    val existingSeasons = repository.getSeasonsForMediaSync(id)
                        .map { SeasonEntry(it.seasonNumber, it.episodeCount) }
                    _uiState.value = ManualAddUiState(
                        title = media.title,
                        description = media.description,
                        mediaType = media.mediaType,
                        posterUrl = media.posterUrl ?: "",
                        seasonEntries = existingSeasons,
                        isEditing = true,
                        apiId = media.apiId
                    )
                }
            }
        }

        viewModelScope.launch {
            titleInputFlow
                .debounce(400)
                .filter { it.length >= 2 }
                .collect { query ->
                    _isSearching.value = true
                    val duplicate = editMediaId == null && repository.existsByTitle(query.trim())
                    _uiState.value = _uiState.value.copy(isDuplicate = duplicate)
                    try {
                        _searchSuggestions.value = tvdbRepository.search(query)
                    } catch (e: Exception) {
                        _searchSuggestions.value = emptyList()
                    } finally {
                        _isSearching.value = false
                    }
                }
        }
    }

    fun updateTitle(title: String) {
        _uiState.value = _uiState.value.copy(title = title, titleError = null, isDuplicate = false)
        if (title.length < 2) _searchSuggestions.value = emptyList()
        titleInputFlow.tryEmit(title)
    }

    fun clearSuggestions() {
        _searchSuggestions.value = emptyList()
    }

    fun selectSuggestion(item: TvdbSearchItem) {
        _searchSuggestions.value = emptyList()
        _isSearching.value = true
        viewModelScope.launch {
            try {
                val isTV = item.type == "series"
                val tvdbId = item.tvdbId?.toLongOrNull()

                val seasons = if (isTV && tvdbId != null) {
                    tvdbRepository.getSeasons(tvdbId)
                        .map { SeasonEntry(season = it.seasonNumber, episodes = it.episodeCount) }
                } else {
                    emptyList()
                }

                val name = item.name ?: ""
                val duplicate = editMediaId == null && repository.existsByTitle(name.trim())
                _uiState.value = _uiState.value.copy(
                    title = name,
                    description = item.overview ?: "",
                    posterUrl = item.imageUrl ?: "",
                    mediaType = if (isTV) "TV Show" else "Movie",
                    seasonEntries = seasons,
                    apiId = item.tvdbId,
                    isDuplicate = duplicate
                )
            } catch (e: Exception) {
                // silently ignore — user can still fill in manually
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun updateDescription(desc: String) {
        _uiState.value = _uiState.value.copy(description = desc)
    }

    fun updateMediaType(type: String) {
        _uiState.value = _uiState.value.copy(
            mediaType = type,
            seasonEntries = if (type == "TV Show") _uiState.value.seasonEntries else emptyList()
        )
    }

    fun updatePosterUrl(url: String) {
        _uiState.value = _uiState.value.copy(posterUrl = url)
    }

    fun addSeason() {
        val current = _uiState.value.seasonEntries
        val nextNum = if (current.isEmpty()) 1 else current.last().season + 1
        _uiState.value = _uiState.value.copy(
            seasonEntries = current + SeasonEntry(season = nextNum, episodes = defaultEpisodes.value)
        )
    }

    fun removeSeason(index: Int) {
        val updated = _uiState.value.seasonEntries.toMutableList().also { it.removeAt(index) }
        _uiState.value = _uiState.value.copy(seasonEntries = updated)
    }

    fun updateSeasonEpisodes(index: Int, episodes: Int) {
        val updated = _uiState.value.seasonEntries.toMutableList()
        updated[index] = updated[index].copy(episodes = episodes.coerceAtLeast(1))
        _uiState.value = _uiState.value.copy(seasonEntries = updated)
    }

    fun saveMedia(onSuccess: () -> Unit) {
        val state = _uiState.value
        if (state.title.isBlank()) {
            _uiState.value = state.copy(titleError = "Title is required")
            return
        }
        if (state.isDuplicate) return
        _uiState.value = state.copy(isSaving = true)
        viewModelScope.launch {
            val mediaId: Long
            if (editMediaId != null) {
                repository.getMediaById(editMediaId)?.let { existing ->
                    repository.updateMedia(
                        existing.copy(
                            apiId = state.apiId ?: existing.apiId,
                            title = state.title.trim(),
                            description = state.description.trim(),
                            mediaType = state.mediaType,
                            posterUrl = state.posterUrl.trim().takeIf { it.isNotBlank() }
                        )
                    )
                }
                mediaId = editMediaId
            } else {
                mediaId = repository.insertMedia(
                    MediaItemEntity(
                        apiId = state.apiId,
                        title = state.title.trim(),
                        description = state.description.trim(),
                        mediaType = state.mediaType,
                        posterUrl = state.posterUrl.trim().takeIf { it.isNotBlank() },
                        dateAdded = System.currentTimeMillis()
                    )
                )
            }
            if (state.mediaType == "TV Show") {
                repository.replaceSeasons(
                    mediaId,
                    state.seasonEntries.map { entry ->
                        SeasonInfoEntity(
                            mediaId = mediaId,
                            seasonNumber = entry.season,
                            episodeCount = entry.episodes
                        )
                    }
                )
            }
            onSuccess()
        }
    }
}
