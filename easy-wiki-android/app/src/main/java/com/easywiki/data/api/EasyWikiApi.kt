package com.easywiki.data.api

import com.easywiki.model.ApiResponse
import com.easywiki.model.AuthResponse
import com.easywiki.model.LoginRequest
import com.easywiki.model.RegisterRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface EasyWikiApi {

    @POST("api/v1/auth/register")
    suspend fun register(@Body request: RegisterRequest): ApiResponse<Unit?>

    @POST("api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): ApiResponse<AuthResponse>

    @GET("api/v1/health")
    suspend fun health(): ApiResponse<Map<String, String>>
}
