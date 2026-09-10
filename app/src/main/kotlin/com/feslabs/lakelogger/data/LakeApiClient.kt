package com.feslabs.lakelogger.data

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.resume

sealed class LakeApiException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class Http(val code: Int) : LakeApiException("The server returned HTTP $code.")
    class ApiFailure(message: String) : LakeApiException(message)
    class Transport(cause: Throwable) : LakeApiException(cause.message ?: "Network error", cause)
    class Decoding(cause: Throwable) : LakeApiException("Could not understand the server's response.", cause)
}

/**
 * Thin client for the public, unauthenticated fesLabs Lake Logger read API.
 * Used by both the main app UI and the widget's background refresh worker.
 */
class LakeApiClient(
    private val baseUrl: String = "https://feslabs.com/api",
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build(),
) {
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Fetches the most recent reading. Returns `null` if the sheet has no
     * readings yet (not an error condition).
     */
    suspend fun fetchCurrent(): LakeReading? {
        val body = get("$baseUrl/lake/current")
        val decoded = try {
            json.decodeFromString(LakeCurrentResponse.serializer(), body)
        } catch (e: Exception) {
            throw LakeApiException.Decoding(e)
        }
        if (!decoded.ok) throw LakeApiException.ApiFailure("The lake logger API reported a failure.")
        return decoded.reading
    }

    /** Fetches readings for the trailing [days] days, oldest first. */
    suspend fun fetchHistory(days: Int): List<LakeReading> {
        val body = get("$baseUrl/lake/history?days=$days")
        val decoded = try {
            json.decodeFromString(LakeHistoryResponse.serializer(), body)
        } catch (e: Exception) {
            throw LakeApiException.Decoding(e)
        }
        if (!decoded.ok) throw LakeApiException.ApiFailure("The lake logger API reported a failure.")
        return decoded.readings
    }

    private suspend fun get(url: String): String = suspendCancellableCoroutine { continuation ->
        val request = Request.Builder().url(url).get().build()
        val call = client.newCall(request)
        continuation.invokeOnCancellation { call.cancel() }

        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                continuation.resumeWithException(LakeApiException.Transport(e))
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!it.isSuccessful) {
                        continuation.resumeWithException(LakeApiException.Http(it.code))
                        return
                    }
                    val text = try {
                        it.body?.string().orEmpty()
                    } catch (e: IOException) {
                        continuation.resumeWithException(LakeApiException.Transport(e))
                        return
                    }
                    continuation.resume(text)
                }
            }
        })
    }
}
