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

    suspend fun listTrips(): ApiResult<List<ApiTripSummary>> = safeApiCall {
        client.get("api/v1/trips").body()
    }

    suspend fun getTrip(id: String): ApiResult<ApiTripDetail> = safeApiCall {
        client.get("api/v1/trips/$id").body()
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
