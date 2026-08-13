package com.neddy.watchlog.ui.screens.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.neddy.watchlog.data.local.entity.MediaWithProgress
import com.neddy.watchlog.data.preferences.SortOrder
import com.neddy.watchlog.data.preferences.SwipeAction
import com.neddy.watchlog.data.preferences.SwipeWatchedScope
import com.neddy.watchlog.data.preferences.UserPreferencesRepository
import com.neddy.watchlog.data.preferences.WatchlistDisplayMode
import com.neddy.watchlog.data.repository.MarkWatchedOutcome
import com.neddy.watchlog.data.repository.MediaRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class SearchViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MediaRepository.getInstance(application)
    private val prefsRepository = UserPreferencesRepository.getInstance(application)

    private val _filterType = MutableStateFlow<String?>(null)
    val filterType: StateFlow<String?> = _filterType.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Backed directly by the persisted preference so settings and the chip stay in sync
    val showWatched: StateFlow<Boolean> = prefsRepository.showWatchedDefault.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), false
    )

    val sortOrder: StateFlow<String> = prefsRepository.sortOrder.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), SortOrder.DATE_ADDED
    )

    val displayMode: StateFlow<WatchlistDisplayMode> = prefsRepository.watchlistDisplayMode.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), WatchlistDisplayMode.LIST
    )

    val swipeLeftAction: StateFlow<SwipeAction> = prefsRepository.swipeLeftAction.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), SwipeAction.DELETE
    )

    val swipeRightAction: StateFlow<SwipeAction> = prefsRepository.swipeRightAction.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), SwipeAction.MARK_WATCHED
    )

    // Read imperatively when a swipe lands, so it has to be collected eagerly
    private val swipeWatchedScope: StateFlow<SwipeWatchedScope> = prefsRepository.swipeWatchedScope.stateIn(
        viewModelScope, SharingStarted.Eagerly, SwipeWatchedScope.WHOLE_SHOW
    )

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private val debouncedQuery: Flow<String> = _searchQuery.debounce(300L)

    private data class SearchFilter(
        val type: String?, val query: String, val sort: String, val showWatched: Boolean
    )

    val mediaList: StateFlow<List<MediaWithProgress>> = combine(
        _filterType, debouncedQuery, prefsRepository.sortOrder, prefsRepository.showWatchedDefault
    ) { type, query, sort, showWatched ->
        SearchFilter(type, query, sort, showWatched)
    }.flatMapLatest { f ->
        val base = when {
            f.query.isNotBlank() -> repository.searchMediaWithProgress(f.query)
            f.type != null -> repository.getMediaByTypeWithProgress(f.type)
            else -> repository.getAllMediaWithProgress()
        }
        base.map { list ->
            val result = if (!f.showWatched && f.query.isBlank()) {
                list.filter { it.progress?.isFinished != true }
            } else list
            when (f.sort) {
                SortOrder.TITLE -> result.sortedBy { it.media.title.lowercase() }
                SortOrder.OLDER -> result.sortedBy { it.media.dateAdded }
                else -> result.sortedByDescending { it.media.dateAdded }
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    fun setFilter(type: String?) { _filterType.value = type }
    fun updateSearchQuery(query: String) { _searchQuery.value = query }
    fun clearQuery() { _searchQuery.value = "" }
    fun setSortOrder(order: String) { viewModelScope.launch { prefsRepository.setSortOrder(order) } }
    fun toggleShowWatched() {
        viewModelScope.launch { prefsRepository.setShowWatchedDefault(!showWatched.value) }
    }
    fun deleteMedia(id: Long) { viewModelScope.launch { repository.deleteMediaById(id) } }

    fun markWatched(item: MediaWithProgress) {
        viewModelScope.launch {
            val title = item.media.title
            val outcome = repository.markWatched(item.media.id, swipeWatchedScope.value)
            _messages.tryEmit(
                when (outcome) {
                    is MarkWatchedOutcome.WholeWatched -> "$title marked as watched"
                    is MarkWatchedOutcome.SeasonWatched -> "$title season ${outcome.season} marked as watched"
                    is MarkWatchedOutcome.EpisodeWatched -> "$title S${outcome.season}E${outcome.episode} marked as watched"
                    is MarkWatchedOutcome.AlreadyWatched -> "$title is already fully watched"
                }
            )
        }
    }
}
