package com.example.tabidachi.network

import kotlinx.serialization.json.Json

val AppJson = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    isLenient = true
}

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val message: String, val code: Int? = null) : ApiResult<Nothing>()
}

suspend inline fun <T> safeApiCall(block: () -> T): ApiResult<T> = try {
    ApiResult.Success(block())
} catch (e: Exception) {
    ApiResult.Error(e.message ?: "Unknown error")
}
