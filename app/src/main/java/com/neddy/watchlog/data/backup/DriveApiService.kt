package com.neddy.watchlog.data.backup

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

data class DriveFile(val id: String, val name: String?)
data class DriveFileList(val files: List<DriveFile>)

interface DriveApiService {

    @POST("drive/v3/files")
    suspend fun createFileMetadata(
        @Header("Authorization") token: String,
        @Body metadata: RequestBody
    ): Response<DriveFile>

    @PATCH("upload/drive/v3/files/{fileId}")
    suspend fun uploadFileContent(
        @Header("Authorization") token: String,
        @Path("fileId") fileId: String,
        @Query("uploadType") uploadType: String,
        @Body content: RequestBody
    ): Response<DriveFile>

    @GET("drive/v3/files")
    suspend fun listFiles(
        @Header("Authorization") token: String,
        @Query("spaces") spaces: String,
        @Query("q") query: String,
        @Query("fields") fields: String
    ): Response<DriveFileList>

    @GET("drive/v3/files/{fileId}")
    suspend fun downloadFile(
        @Header("Authorization") token: String,
        @Path("fileId") fileId: String,
        @Query("alt") alt: String
    ): Response<ResponseBody>
}
