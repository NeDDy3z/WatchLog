package com.neddy.watchlog.data.remote

import com.google.gson.annotations.SerializedName

// --- Auth ---

data class TvdbLoginRequest(val apikey: String)
data class TvdbLoginData(val token: String)
data class TvdbLoginResponse(val data: TvdbLoginData, val status: String)

// --- Search ---

data class TvdbSearchItem(
    @SerializedName("tvdb_id") val tvdbId: String?,
    val name: String?,
    val overview: String?,
    @SerializedName("image_url") val imageUrl: String?,
    val type: String?,
    val year: String?
)

data class TvdbSearchResponse(
    val data: List<TvdbSearchItem>?,
    val status: String
)

// --- Episodes ---

data class TvdbEpisodeEntry(
    val id: Long?,
    val number: Int?,
    val seasonNumber: Int?
)

data class TvdbEpisodesData(
    val episodes: List<TvdbEpisodeEntry>?
)

data class TvdbEpisodesResponse(
    val data: TvdbEpisodesData?,
    val status: String
)

// --- Computed result passed out of repository ---

data class TvdbSeasonInfo(val seasonNumber: Int, val episodeCount: Int)
