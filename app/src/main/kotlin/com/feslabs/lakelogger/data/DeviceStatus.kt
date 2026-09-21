package com.feslabs.lakelogger.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Mirrors a useful subset of the fields returned by the logger device's own
 * `/status` endpoint, reachable only when the phone is on the same local
 * network as the device (it is not exposed to the internet). All fields
 * are nullable so firmware version drift degrades gracefully to "unknown"
 * instead of failing to parse the whole response.
 */
@Serializable
data class DeviceStatus(
    @SerialName("device_id") val deviceId: String? = null,
    @SerialName("site_health") val siteHealth: String? = null,
    @SerialName("ip") val ip: String? = null,
    @SerialName("wifi_connected") val wifiConnected: Boolean? = null,
    @SerialName("wifi_rssi_dbm") val wifiRssiDbm: Int? = null,
    @SerialName("uptime_s") val uptimeS: Long? = null,
    @SerialName("last_system_reset_reason") val lastSystemResetReason: String? = null,

    @SerialName("clock_valid") val clockValid: Boolean? = null,
    @SerialName("clock_now_utc") val clockNowUtc: String? = null,
    @SerialName("last_ntp_sync_age") val lastNtpSyncAge: String? = null,
    @SerialName("consecutive_ntp_failures") val consecutiveNtpFailures: Int? = null,
    @SerialName("ntp_sync_delta_seconds") val ntpSyncDeltaSeconds: Int? = null,
    @SerialName("ntp_largest_observed_skew_seconds") val ntpLargestObservedSkewSeconds: Int? = null,
    @SerialName("ntp_skew_sample_count") val ntpSkewSampleCount: Int? = null,

    @SerialName("sensor_found") val sensorFound: Boolean? = null,
    @SerialName("last_sensor_discovery_attempt_succeeded") val lastSensorDiscoveryAttemptSucceeded: Boolean? = null,
    @SerialName("consecutive_sensor_discovery_failures") val consecutiveSensorDiscoveryFailures: Int? = null,

    @SerialName("upload_endpoint_host") val uploadEndpointHost: String? = null,
    @SerialName("last_successful_upload_utc") val lastSuccessfulUploadUtc: String? = null,
    @SerialName("last_successful_upload_age") val lastSuccessfulUploadAge: String? = null,
    @SerialName("last_upload_error") val lastUploadError: String? = null,
    @SerialName("last_upload_error_utc") val lastUploadErrorUtc: String? = null,
    @SerialName("consecutive_upload_failures") val consecutiveUploadFailures: Int? = null,
    @SerialName("successful_uploads") val successfulUploads: Int? = null,
    @SerialName("failed_uploads") val failedUploads: Int? = null,
    @SerialName("backlog_count") val backlogCount: Int? = null,
    @SerialName("dropped_backlog_entries") val droppedBacklogEntries: Int? = null,

    @SerialName("last_successful_probe_read_utc") val lastSuccessfulProbeReadUtc: String? = null,
    @SerialName("last_successful_probe_read_age") val lastSuccessfulProbeReadAge: String? = null,
    @SerialName("successful_probe_reads") val successfulProbeReads: Int? = null,
    @SerialName("failed_probe_reads") val failedProbeReads: Int? = null,

    @SerialName("cached_probe_battery_output_voltage_v") val cachedProbeBatteryOutputVoltageV: Double? = null,
    @SerialName("cached_probe_solar_input_voltage_v") val cachedProbeSolarInputVoltageV: Double? = null,
    @SerialName("battery_charge_level_pct_approx") val batteryChargeLevelPctApprox: Double? = null,

    @SerialName("modbus_failure_total") val modbusFailureTotal: Int? = null,
    @SerialName("consecutive_solinst_modbus_failures") val consecutiveSolinstModbusFailures: Int? = null,
    @SerialName("consecutive_weather_modbus_failures") val consecutiveWeatherModbusFailures: Int? = null,
    @SerialName("rs485_bridge_recovery_attempts") val rs485BridgeRecoveryAttempts: Int? = null,
    @SerialName("rs485_bridge_recovery_successes") val rs485BridgeRecoverySuccesses: Int? = null,

    @SerialName("rs485_solinst_bridge_health_supported") val rs485SolinstBridgeHealthSupported: Boolean? = null,
    @SerialName("rs485_solinst_bridge_line_status_register") val rs485SolinstBridgeLineStatusRegister: Int? = null,
    @SerialName("rs485_solinst_bridge_overrun_error") val rs485SolinstBridgeOverrunError: Boolean? = null,
    @SerialName("rs485_solinst_bridge_parity_error") val rs485SolinstBridgeParityError: Boolean? = null,
    @SerialName("rs485_solinst_bridge_framing_error") val rs485SolinstBridgeFramingError: Boolean? = null,
    @SerialName("rs485_solinst_bridge_break_detected") val rs485SolinstBridgeBreakDetected: Boolean? = null,

    @SerialName("rs485_weather_bridge_health_supported") val rs485WeatherBridgeHealthSupported: Boolean? = null,
    @SerialName("rs485_weather_bridge_line_status_register") val rs485WeatherBridgeLineStatusRegister: Int? = null,
    @SerialName("rs485_weather_bridge_overrun_error") val rs485WeatherBridgeOverrunError: Boolean? = null,
    @SerialName("rs485_weather_bridge_parity_error") val rs485WeatherBridgeParityError: Boolean? = null,
    @SerialName("rs485_weather_bridge_framing_error") val rs485WeatherBridgeFramingError: Boolean? = null,
    @SerialName("rs485_weather_bridge_break_detected") val rs485WeatherBridgeBreakDetected: Boolean? = null,

    @SerialName("display_behavior") val displayBehavior: String? = null,
    @SerialName("display_backend") val displayBackend: String? = null,
    @SerialName("display_present") val displayPresent: Boolean? = null,
    @SerialName("display_awake") val displayAwake: Boolean? = null,
    @SerialName("display_wake_remaining_ms") val displayWakeRemainingMs: Long? = null,
    @SerialName("last_display_wake_request_utc") val lastDisplayWakeRequestUtc: String? = null,
    @SerialName("last_display_wake_request_age") val lastDisplayWakeRequestAge: String? = null,
    @SerialName("last_display_refresh_utc") val lastDisplayRefreshUtc: String? = null,
    @SerialName("last_display_refresh_age") val lastDisplayRefreshAge: String? = null,
    @SerialName("display_refresh_count") val displayRefreshCount: Int? = null,
    @SerialName("display_i2c_recovery_count") val displayI2cRecoveryCount: Int? = null,
    @SerialName("display_link_failures") val displayLinkFailures: Int? = null,
    @SerialName("display_last_error") val displayLastError: String? = null,
)

/**
 * Mirrors the response shape shared by every `/display/<command>` endpoint
 * (`status`, `refresh`, `clear`, `pause`, `resume`, `reboot`, `sleep`): the
 * firmware runs the requested display command and reports whether it
 * succeeded, which backend is attached, and any human-readable detail.
 * `GET /display/status` queries without side effects; the rest are
 * POST-only since they change display state.
 */
@Serializable
data class DeviceDisplayCommandResult(
    @SerialName("ok") val ok: Boolean? = null,
    @SerialName("display_backend") val displayBackend: String? = null,
    @SerialName("response") val response: String? = null,
)

/**
 * Mirrors the on-demand `POST /rs485/selftest` endpoint: an internal
 * loopback test of each RS485 bridge's SC16IS752 UART core, independent of
 * the physical bus wiring or the downstream sensor. Deliberately POST-only
 * and manually triggered since it is disruptive to any in-flight
 * transaction on these channels.
 */
@Serializable
data class DeviceRs485SelfTestResult(
    @SerialName("solinst_selftest_supported") val solinstSelftestSupported: Boolean? = null,
    @SerialName("solinst_selftest_passed") val solinstSelftestPassed: Boolean? = null,
    @SerialName("weather_selftest_supported") val weatherSelftestSupported: Boolean? = null,
    @SerialName("weather_selftest_passed") val weatherSelftestPassed: Boolean? = null,
)

/**
 * Mirrors the on-demand `/probe` endpoint: forces a fresh sensor read and
 * returns it immediately, bypassing both the device's own upload schedule
 * and the feslabs.com cloud cache entirely. Useful for confirming the
 * physical sensors/RS485 bus are healthy even when cloud uploads are
 * stalled (as happened with the NTP-skew scheduler bug).
 */
@Serializable
data class DeviceProbeReading(
    @SerialName("ok") val ok: Boolean? = null,
    @SerialName("timestamp_utc") val timestampUtc: String? = null,
    @SerialName("water_level_m") val waterLevelM: Double? = null,
    @SerialName("temperature_c") val temperatureC: Double? = null,
    @SerialName("battery_output_voltage_v") val batteryOutputVoltageV: Double? = null,
    @SerialName("solar_input_voltage_v") val solarInputVoltageV: Double? = null,
    @SerialName("battery_charge_level_pct_approx") val batteryChargeLevelPctApprox: Double? = null,
    @SerialName("weather_air_temperature_c") val weatherAirTemperatureC: Double? = null,
    @SerialName("weather_relative_humidity_pct") val weatherRelativeHumidityPct: Double? = null,
)
