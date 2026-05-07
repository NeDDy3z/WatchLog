package com.neddy.watchlog.data.backup

import android.content.Context
import com.google.gson.Gson
import com.neddy.watchlog.data.repository.MediaRepository
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class DriveBackupRepository private constructor(private val context: Context) {

    private val gson = Gson()

    private val api: DriveApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://www.googleapis.com/")
            .client(OkHttpClient())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(DriveApiService::class.java)
    }

    suspend fun backup(accessToken: String, mediaRepository: MediaRepository): Result<Unit> = runCatching {
        val token = "Bearer $accessToken"
        val backup = mediaRepository.getFullBackup()
        val json = gson.toJson(backup)
        val jsonBody = json.toRequestBody("application/json".toMediaType())

        val listResponse = api.listFiles(
            token = token,
            spaces = "appDataFolder",
            query = "name='watchlog_backup.json'",
            fields = "files(id,name)"
        )
        val existingFileId = listResponse.body()?.files?.firstOrNull()?.id

        if (existingFileId != null) {
            api.uploadFileContent(
                token = token,
                fileId = existingFileId,
                uploadType = "media",
                content = jsonBody
            )
        } else {
            val metadataJson = """{"name":"watchlog_backup.json","parents":["appDataFolder"],"mimeType":"application/json"}"""
            val metadataBody = metadataJson.toRequestBody("application/json".toMediaType())
            val createResponse = api.createFileMetadata(token = token, metadata = metadataBody)
            val newFileId = createResponse.body()?.id ?: error("Failed to create backup file")
            api.uploadFileContent(
                token = token,
                fileId = newFileId,
                uploadType = "media",
                content = jsonBody
            )
        }
    }

    suspend fun restore(accessToken: String, mediaRepository: MediaRepository): Result<Unit> = runCatching {
        val token = "Bearer $accessToken"

        val listResponse = api.listFiles(
            token = token,
            spaces = "appDataFolder",
            query = "name='watchlog_backup.json'",
            fields = "files(id,name)"
        )
        val fileId = listResponse.body()?.files?.firstOrNull()?.id
            ?: error("No backup found on Google Drive")

        val downloadResponse = api.downloadFile(token = token, fileId = fileId, alt = "media")
        val jsonContent = downloadResponse.body()?.string()
            ?: error("Failed to download backup")

        val backup = gson.fromJson(jsonContent, WatchlogBackup::class.java)
        mediaRepository.restoreFromBackup(backup)
    }

    companion object {
        @Volatile
        private var INSTANCE: DriveBackupRepository? = null

        fun getInstance(context: Context): DriveBackupRepository =
            INSTANCE ?: synchronized(this) {
                DriveBackupRepository(context.applicationContext).also { INSTANCE = it }
            }
    }
}
