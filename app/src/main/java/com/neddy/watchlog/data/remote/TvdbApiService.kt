package com.neddy.watchlog.data.remote

import com.neddy.watchlog.BuildConfig
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface TvdbApiService {
    @POST("login")
    suspend fun login(@Body request: TvdbLoginRequest): TvdbLoginResponse

    @GET("search")
    suspend fun search(
        @Query("query") query: String,
        @Query("limit") limit: Int
    ): TvdbSearchResponse

    @GET("series/{id}/episodes/official")
    suspend fun getEpisodes(
        @Path("id") seriesId: Long,
        @Query("page") page: Int
    ): TvdbEpisodesResponse
}

object TvdbApi {
    private const val BASE_URL = "https://api4.thetvdb.com/v4/"

    private val apiKey = BuildConfig.TVDB_API_KEY

    @Volatile private var cachedToken: String? = null

    private val httpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = cachedToken?.let {
                chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer $it")
                    .build()
            } ?: chain.request()
            chain.proceed(request)
        }
        .build()

    val service: TvdbApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TvdbApiService::class.java)
    }

    suspend fun ensureToken() {
        if (cachedToken == null) {
            require(apiKey.isNotBlank()) {
                "TVDB_API_KEY is missing. Add it to local.properties."
            }
            cachedToken = service.login(TvdbLoginRequest(apiKey)).data.token
        }
    }
}
