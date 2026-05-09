package com.neddy.watchlog.data.remote

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

// --- Auth ---

@Keep
data class TvdbLoginRequest(val apikey: String)
@Keep
data class TvdbLoginData(val token: String)
@Keep
data class TvdbLoginResponse(val data: TvdbLoginData, val status: String)

// --- Search ---

@Keep
data class TvdbSearchItem(
    @SerializedName("tvdb_id") val tvdbId: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("overview") val overview: String?,
    @SerializedName("image_url") val imageUrl: String?,
    @SerializedName("type") val type: String?,
    @SerializedName("year") val year: String?
)

@Keep
data class TvdbSearchResponse(
    @SerializedName("data") val data: List<TvdbSearchItem>?,
    @SerializedName("status") val status: String
)

// --- Episodes ---

@Keep
data class TvdbEpisodeEntry(
    @SerializedName("id") val id: Long?,
    @SerializedName("number") val number: Int?,
    @SerializedName("seasonNumber") val seasonNumber: Int?
)

@Keep
data class TvdbEpisodesData(
    @SerializedName("episodes") val episodes: List<TvdbEpisodeEntry>?
)

@Keep
data class TvdbEpisodesResponse(
    @SerializedName("data") val data: TvdbEpisodesData?,
    @SerializedName("status") val status: String
)

// --- Computed result passed out of repository ---
@Keep
data class TvdbSeasonInfo(val seasonNumber: Int, val episodeCount: Int)
