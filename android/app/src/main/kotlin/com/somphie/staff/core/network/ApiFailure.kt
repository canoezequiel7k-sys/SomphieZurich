package com.somphie.staff.core.network

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import retrofit2.HttpException

class ApiFailure(
    val status: Int,
    val code: String,
    val detail: String?,
) : Exception(detail)

suspend fun <T> apiCall(json: Json, call: suspend () -> T): T =
    try {
        call()
    } catch (exception: HttpException) {
        val response = exception.response()
        val body = response?.errorBody()?.use { it.string() }
        val problem = body?.let {
            try {
                json.decodeFromString(ApiProblem.serializer(), it)
            } catch (_: SerializationException) {
                null
            }
        }
        throw ApiFailure(
            status = exception.code(),
            code = problem?.code ?: "HTTP_ERROR",
            detail = problem?.detail ?: problem?.title,
        )
    }
