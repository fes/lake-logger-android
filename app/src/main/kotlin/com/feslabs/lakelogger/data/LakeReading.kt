package com.feslabs.lakelogger.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A single lake logger reading as returned by the fesLabs API
 * (`GET /api/lake/current` and `GET /api/lake/history`).
 *
 * Field names mirror the API's camelCase JSON keys 1:1 via [SerialName] so no
 * manual mapping drifts out of sync. Keep this in sync with
 * `functions/src/index.ts`'s `LakeReading` type in the feslabs-web repo, and
 * with `LakeReading.swift` in the iOS lake-logger-app repo.
 */
@Serializable
data class LakeReading(
    @SerialName("receivedAtUtc") val receivedAtUtc: String? = null,
    @SerialName("timestampUtc") val timestampUtc: String? = null,
    @SerialName("deviceId") val deviceId: String? = null,
    @SerialName("modbusId") val modbusId: Int? = null,
    @SerialName("serialNumber") val serialNumber: String? = null,
    @SerialName("firmware") val firmware: String? = null,

    @SerialName("waterLevelM") val waterLevelM: Double? = null,
    @SerialName("temperatureC") val temperatureC: Double? = null,

    @SerialName("batteryOutputMonitorPresent") val batteryOutputMonitorPresent: Boolean? = null,
    @SerialName("batteryOutputMonitorValid") val batteryOutputMonitorValid: Boolean? = null,
    @SerialName("batteryOutputVoltageV") val batteryOutputVoltageV: Double? = null,
    @SerialName("batteryOutputCurrentA") val batteryOutputCurrentA: Double? = null,
    @SerialName("batteryOutputPowerW") val batteryOutputPowerW: Double? = null,

    @SerialName("solarInputMonitorPresent") val solarInputMonitorPresent: Boolean? = null,
    @SerialName("solarInputMonitorValid") val solarInputMonitorValid: Boolean? = null,
    @SerialName("solarInputVoltageV") val solarInputVoltageV: Double? = null,
    @SerialName("solarInputCurrentA") val solarInputCurrentA: Double? = null,
    @SerialName("solarInputPowerW") val solarInputPowerW: Double? = null,

    @SerialName("batteryChargeLevelPctApprox") val batteryChargeLevelPctApprox: Double? = null,
    @SerialName("solarChargingBattery") val solarChargingBattery: Boolean? = null,

    @SerialName("weatherEnabled") val weatherEnabled: Boolean? = null,
    @SerialName("weatherPresent") val weatherPresent: Boolean? = null,
    @SerialName("weatherValid") val weatherValid: Boolean? = null,
    @SerialName("weatherModbusId") val weatherModbusId: Int? = null,
    @SerialName("weatherReadUtc") val weatherReadUtc: String? = null,
    @SerialName("weatherAirTemperatureC") val weatherAirTemperatureC: Double? = null,
    @SerialName("weatherRelativeHumidityPct") val weatherRelativeHumidityPct: Double? = null,
    @SerialName("weatherBarometricPressureHpa") val weatherBarometricPressureHpa: Double? = null,
    @SerialName("weatherWindSpeedMS") val weatherWindSpeedMS: Double? = null,
    @SerialName("weatherWindDirectionDeg") val weatherWindDirectionDeg: Double? = null,
    @SerialName("weatherRainfallMm") val weatherRainfallMm: Double? = null,
    @SerialName("weatherLightLux") val weatherLightLux: Double? = null,
    @SerialName("weatherLastError") val weatherLastError: String? = null,

    @SerialName("weatherSummarySampleCount") val weatherSummarySampleCount: Int? = null,
    @SerialName("weatherSummaryStartUtc") val weatherSummaryStartUtc: String? = null,
    @SerialName("weatherSummaryEndUtc") val weatherSummaryEndUtc: String? = null,
    @SerialName("weatherAirTemperatureCAvg") val weatherAirTemperatureCAvg: Double? = null,
    @SerialName("weatherAirTemperatureCMin") val weatherAirTemperatureCMin: Double? = null,
    @SerialName("weatherAirTemperatureCMax") val weatherAirTemperatureCMax: Double? = null,
    @SerialName("weatherRelativeHumidityPctAvg") val weatherRelativeHumidityPctAvg: Double? = null,
    @SerialName("weatherRelativeHumidityPctMin") val weatherRelativeHumidityPctMin: Double? = null,
    @SerialName("weatherRelativeHumidityPctMax") val weatherRelativeHumidityPctMax: Double? = null,
    @SerialName("weatherBarometricPressureHpaAvg") val weatherBarometricPressureHpaAvg: Double? = null,
    @SerialName("weatherBarometricPressureHpaMin") val weatherBarometricPressureHpaMin: Double? = null,
    @SerialName("weatherBarometricPressureHpaMax") val weatherBarometricPressureHpaMax: Double? = null,
    @SerialName("weatherWindSpeedMSAvg") val weatherWindSpeedMSAvg: Double? = null,
    @SerialName("weatherWindSpeedMSMin") val weatherWindSpeedMSMin: Double? = null,
    @SerialName("weatherWindSpeedMSMax") val weatherWindSpeedMSMax: Double? = null,
    @SerialName("weatherWindDirectionDegAvg") val weatherWindDirectionDegAvg: Double? = null,
    @SerialName("weatherLightLuxAvg") val weatherLightLuxAvg: Double? = null,
    @SerialName("weatherLightLuxMin") val weatherLightLuxMin: Double? = null,
    @SerialName("weatherLightLuxMax") val weatherLightLuxMax: Double? = null,
    @SerialName("weatherRainfallIntervalMm") val weatherRainfallIntervalMm: Double? = null,
    @SerialName("weatherRainfallCounterReset") val weatherRainfallCounterReset: Boolean? = null,

    @SerialName("status") val status: String? = null,
    @SerialName("uploadSource") val uploadSource: String? = null,
) {
    val isOk: Boolean get() = status?.uppercase() == "OK"

    val timestampEpochMillis: Long?
        get() = parseIso8601(timestampUtc)

    val weatherReadEpochMillis: Long?
        get() = parseIso8601(weatherReadUtc)

    companion object {
        /**
         * Parses ISO 8601 UTC timestamps in either fractional-second
         * (`...SSSZ`) or whole-second (`...Z`) form, matching the two shapes
         * the API can return, without pulling in a heavier date library.
         */
        fun parseIso8601(value: String?): Long? {
            if (value.isNullOrEmpty()) return null
            return try {
                java.time.Instant.parse(value).toEpochMilli()
            } catch (e: java.time.format.DateTimeParseException) {
                null
            }
        }
    }
}

@Serializable
data class LakeApiWarning(
    @SerialName("code") val code: String? = null,
    @SerialName("message") val message: String? = null,
    @SerialName("details") val details: List<String>? = null,
)

@Serializable
data class LakeCurrentResponse(
    @SerialName("ok") val ok: Boolean = false,
    @SerialName("reading") val reading: LakeReading? = null,
    @SerialName("count") val count: Int = 0,
    @SerialName("tabsRead") val tabsRead: List<String>? = null,
    @SerialName("tabsMissing") val tabsMissing: List<String>? = null,
    @SerialName("warnings") val warnings: List<LakeApiWarning>? = null,
)

@Serializable
data class LakeHistoryResponse(
    @SerialName("ok") val ok: Boolean = false,
    @SerialName("readings") val readings: List<LakeReading> = emptyList(),
    @SerialName("count") val count: Int = 0,
    @SerialName("tabsRead") val tabsRead: List<String>? = null,
    @SerialName("tabsMissing") val tabsMissing: List<String>? = null,
    @SerialName("warnings") val warnings: List<LakeApiWarning>? = null,
)
