package com.neddy.watchlog.data.remote

class TvdbRepository {
    private val service = TvdbApi.service

    suspend fun search(query: String): List<TvdbSearchItem> {
        TvdbApi.ensureToken()
        return service.search(query, MAX_SUGGESTIONS).data
            ?.filter { it.type == "series" || it.type == "movie" }
            ?.filter { !it.tvdbId.isNullOrBlank() }
            ?: emptyList()
    }

    suspend fun getSeasons(tvdbId: Long): List<TvdbSeasonInfo> {
        TvdbApi.ensureToken()
        val episodes = service.getEpisodes(tvdbId, 0).data?.episodes ?: return emptyList()
        return episodes
            .filter { (it.seasonNumber ?: 0) > 0 }
            .groupBy { it.seasonNumber!! }
            .entries
            .sortedBy { it.key }
            .map { (num, eps) -> TvdbSeasonInfo(num, eps.size) }
    }

    companion object {
        const val MAX_SUGGESTIONS = 10
    }
}
