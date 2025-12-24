package com.example.aduanjalan.data.remote.api


import com.example.aduanjalan.domain.model.OverpassResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface OverpassApiService {
    @GET("api/interpreter")
    suspend fun getRoadData(
        @Query("data") dataQuery: String
    ): Response<OverpassResponse>
}