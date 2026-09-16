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
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

sealed class DeviceApiException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    object NoAddressConfigured : DeviceApiException("Enter the logger's local network IP address first.")
    class Http(val code: Int) : DeviceApiException("The device returned HTTP $code.")
    class Transport(cause: Throwable) : DeviceApiException(
        "Couldn't reach the device: ${cause.message}. Make sure your phone is on the same Wi-Fi network as the logger.",
        cause,
    )
    class Decoding(cause: Throwable) : DeviceApiException("Could not understand the device's response.", cause)
}

/** A decoded model plus the exact raw JSON text the device returned. */
data class DeviceApiResult<T>(val value: T, val rawJson: String)

/**
 * Thin client for the logger device's own local HTTP API (`/status`,
 * `/probe`, `/reset`), served directly by the device on the LAN -- distinct
 * from [LakeApiClient], which talks to the feslabs.com cloud API. Only
 * reachable when the phone and the device are on the same local network.
 */
class DeviceApiClient(
    private val settingsStore: DeviceSettingsStore,
    private val client: OkHttpClient = OkHttpClient.Builder()
        // The device is on the LAN; if it doesn't respond quickly it's
        // almost certainly unreachable (wrong network, device down), so
        // fail fast rather than using the default (much longer) timeouts.
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .build(),
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetchStatus(): DeviceApiResult<DeviceStatus> {
        val body = get("status")
        val value = try {
            json.decodeFromString(DeviceStatus.serializer(), body)
        } catch (e: Exception) {
            throw DeviceApiException.Decoding(e)
        }
        return DeviceApiResult(value, body)
    }

    suspend fun fetchProbe(): DeviceApiResult<DeviceProbeReading> {
        val body = get("probe")
        val value = try {
            json.decodeFromString(DeviceProbeReading.serializer(), body)
        } catch (e: Exception) {
            throw DeviceApiException.Decoding(e)
        }
        return DeviceApiResult(value, body)
    }

    /**
     * Reboots the device (`NVIC_SystemReset()` on the firmware side). Used
     * to recover from the known clock-skew scheduler stall until the
     * firmware fix is flashed to the physical device.
     */
    suspend fun reset() {
        try {
            get("reset")
        } catch (e: DeviceApiException.Transport) {
            // The device deliberately closes the connection as it reboots,
            // so a transport error here is expected and not a failure.
        }
    }

    private suspend fun get(path: String): String {
        val baseUrl = settingsStore.loadIpAddress()?.trim()
        if (baseUrl.isNullOrEmpty()) throw DeviceApiException.NoAddressConfigured

        return suspendCancellableCoroutine { continuation ->
            val request = Request.Builder().url("http://$baseUrl/$path").get().build()
            val call = client.newCall(request)
            continuation.invokeOnCancellation { call.cancel() }

            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    continuation.resumeWithException(DeviceApiException.Transport(e))
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        if (!it.isSuccessful) {
                            continuation.resumeWithException(DeviceApiException.Http(it.code))
                            return
                        }
                        val text = try {
                            it.body?.string().orEmpty()
                        } catch (e: IOException) {
                            continuation.resumeWithException(DeviceApiException.Transport(e))
                            return
                        }
                        continuation.resume(text)
                    }
                }
            })
        }
    }
}
