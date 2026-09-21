package com.feslabs.lakelogger.data

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
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
 * `/probe`, `/reset`, `/rs485/selftest`, `/display/<command>`), served directly by
 * the device on the LAN -- distinct
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

    /**
     * Runs the device's internal RS485 bridge self-test
     * (`POST /rs485/selftest`). This is a loopback test of each bridge's
     * SC16IS752 UART core only -- it does not exercise the physical bus or
     * the downstream sensor -- and is disruptive to in-flight Modbus
     * transactions, which is why the firmware requires POST rather than GET.
     */
    suspend fun triggerRs485SelfTest(): DeviceApiResult<DeviceRs485SelfTestResult> {
        val body = post("rs485/selftest")
        val value = try {
            json.decodeFromString(DeviceRs485SelfTestResult.serializer(), body)
        } catch (e: Exception) {
            throw DeviceApiException.Decoding(e)
        }
        return DeviceApiResult(value, body)
    }

    /**
     * Runs a display command (`status`, `refresh`, `clear`, `pause`,
     * `resume`, `reboot`, or `sleep`) via `/display/<command>`, the
     * Inkplate e-paper display's own local control API. `status` is a
     * read-only GET; every other command mutates display state and is
     * POST-only by firmware design.
     */
    suspend fun runDisplayCommand(command: String): DeviceApiResult<DeviceDisplayCommandResult> {
        val body = if (command == "status") get("display/status") else post("display/$command")
        val value = try {
            json.decodeFromString(DeviceDisplayCommandResult.serializer(), body)
        } catch (e: Exception) {
            throw DeviceApiException.Decoding(e)
        }
        return DeviceApiResult(value, body)
    }

    private suspend fun get(path: String): String = send(path, "GET")

    private suspend fun post(path: String): String = send(path, "POST")

    private suspend fun send(path: String, method: String): String {
        val baseUrl = settingsStore.loadIpAddress()?.trim()
        if (baseUrl.isNullOrEmpty()) throw DeviceApiException.NoAddressConfigured

        return suspendCancellableCoroutine { continuation ->
            val requestBuilder = Request.Builder().url("http://$baseUrl/$path")
            if (method == "POST") {
                requestBuilder.post("".toRequestBody(null))
            } else {
                requestBuilder.get()
            }
            val request = requestBuilder.build()
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
