package com.neddy.watchlog.data.update

import android.content.Context
import com.neddy.watchlog.BuildConfig
import com.neddy.watchlog.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first

data class UpdateInfo(
    val version: String,
    val releaseName: String,
    val notes: String,
    val releaseUrl: String
)

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data class Available(val info: UpdateInfo) : UpdateState
    data object UpToDate : UpdateState
    data class Error(val message: String) : UpdateState
}

/**
 * Checks the GitHub releases page for a newer version. State is held in a singleton so the
 * settings screen and the app-wide update dialog stay in sync.
 */
class UpdateRepository private constructor(
    private val prefs: UserPreferencesRepository
) {
    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    /** Runs on app start: honours the auto-check toggle, the daily interval and skipped versions. */
    suspend fun checkAutomatically() {
        if (!prefs.updateCheckEnabled.first()) return
        val last = prefs.lastUpdateCheckAt.first()
        if (System.currentTimeMillis() - last < CHECK_INTERVAL_MILLIS) return
        check(manual = false)
    }

    /** Runs from the settings screen: always hits the network and ignores skipped versions. */
    suspend fun checkNow() = check(manual = true)

    private suspend fun check(manual: Boolean) {
        if (_state.value is UpdateState.Checking) return
        _state.value = UpdateState.Checking
        try {
            val release = GithubApi.service.getLatestRelease(GithubApi.OWNER, GithubApi.REPO)
            prefs.setLastUpdateCheckAt(System.currentTimeMillis())

            val tag = release.tagName?.trim().orEmpty()
            if (tag.isEmpty() || release.draft) {
                _state.value = if (manual) UpdateState.UpToDate else UpdateState.Idle
                return
            }
            val isNewer = compareVersions(tag, BuildConfig.VERSION_NAME) > 0
            val skipped = !manual && normalizeVersion(tag) == normalizeVersion(prefs.skippedUpdateVersion.first())

            _state.value = when {
                !isNewer -> if (manual) UpdateState.UpToDate else UpdateState.Idle
                skipped -> UpdateState.Idle
                else -> UpdateState.Available(
                    UpdateInfo(
                        version = normalizeVersion(tag),
                        releaseName = release.name?.takeIf { it.isNotBlank() } ?: tag,
                        notes = release.body?.trim().orEmpty(),
                        releaseUrl = release.htmlUrl?.takeIf { it.isNotBlank() } ?: GithubApi.RELEASES_URL
                    )
                )
            }
        } catch (e: Exception) {
            _state.value = if (manual) {
                UpdateState.Error(e.message ?: "Could not check for updates")
            } else {
                UpdateState.Idle
            }
        }
    }

    /** "Later" - keep the update, just stop showing the dialog for now. */
    fun dismiss() {
        _state.value = UpdateState.Idle
    }

    /** "Never" - do not bring this version up again during automatic checks. */
    suspend fun skipVersion(version: String) {
        prefs.setSkippedUpdateVersion(version)
        _state.value = UpdateState.Idle
    }

    companion object {
        private const val CHECK_INTERVAL_MILLIS = 24 * 60 * 60 * 1000L

        @Volatile
        private var INSTANCE: UpdateRepository? = null

        fun getInstance(context: Context): UpdateRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: UpdateRepository(
                    UserPreferencesRepository.getInstance(context.applicationContext)
                ).also { INSTANCE = it }
            }

        fun normalizeVersion(raw: String): String = raw.trim().removePrefix("v").removePrefix("V")

        /** Numeric, segment by segment: "1.10" is newer than "1.9", missing segments count as 0. */
        fun compareVersions(a: String, b: String): Int {
            val left = versionSegments(a)
            val right = versionSegments(b)
            for (i in 0 until maxOf(left.size, right.size)) {
                val diff = left.getOrElse(i) { 0 } - right.getOrElse(i) { 0 }
                if (diff != 0) return diff
            }
            return 0
        }

        private fun versionSegments(raw: String): List<Int> =
            normalizeVersion(raw).split('.', '-', '_', ' ').mapNotNull { it.toIntOrNull() }
    }
}
