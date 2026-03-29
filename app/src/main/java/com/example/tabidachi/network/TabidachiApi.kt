package com.example.tabidachi.network

import com.example.tabidachi.data.SecureStorage
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.serialization.kotlinx.json.json

class TabidachiApi(private val secureStorage: SecureStorage) {

    private val client by lazy {
        HttpClient(Android) {
            install(ContentNegotiation) {
                json(AppJson)
            }
            defaultRequest {
                url(secureStorage.serverUrl.trimEnd('/') + "/")
                header("Authorization", "Bearer ${secureStorage.apiToken}")
            }
        }
    }

    // Reusable unauthenticated client for public share endpoints. No Bearer token, no base URL.
    private val shareClient by lazy {
        HttpClient(Android) {
            install(ContentNegotiation) { json(AppJson) }
        }
    }

    suspend fun listTrips(): ApiResult<List<ApiTripSummary>> = safeApiCall {
        client.get("api/v1/trips").body()
    }

    suspend fun getTrip(id: String): ApiResult<ApiTripDetail> = safeApiCall {
        client.get("api/v1/trips/$id").body()
    }

    suspend fun getSharedTrip(serverUrl: String, shareToken: String): ApiResult<ApiTripDetail> = safeApiCall {
        shareClient.get(serverUrl.trimEnd('/') + "/api/v1/share/$shareToken").body()
    }

    suspend fun testConnection(serverUrl: String, token: String): ApiResult<List<ApiTripSummary>> = safeApiCall {
        val testClient = HttpClient(Android) {
            install(ContentNegotiation) {
                json(AppJson)
            }
        }
        testClient.use { c ->
            c.get(serverUrl.trimEnd('/') + "/api/v1/trips") {
                header("Authorization", "Bearer $token")
            }.body()
        }
    }
}
