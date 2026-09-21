package com.feslabs.lakelogger.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.feslabs.lakelogger.data.DeviceApiClient
import com.feslabs.lakelogger.data.DeviceDisplayCommandResult
import com.feslabs.lakelogger.data.DeviceProbeReading
import com.feslabs.lakelogger.data.DeviceRs485SelfTestResult
import com.feslabs.lakelogger.data.DeviceSettingsStore
import com.feslabs.lakelogger.data.DeviceStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class DeviceDiagnosticsUiState(
    val ipAddress: String = "",
    val status: DeviceStatus? = null,
    val probe: DeviceProbeReading? = null,
    val selfTest: DeviceRs485SelfTestResult? = null,
    val displayResult: DeviceDisplayCommandResult? = null,
    val lastDisplayCommand: String? = null,
    val isLoading: Boolean = false,
    val isResetting: Boolean = false,
    val isRunningSelfTest: Boolean = false,
    val isRunningDisplayCommand: Boolean = false,
    val errorMessage: String? = null,
    val lastCheckedAtEpochMillis: Long? = null,
)

class DeviceDiagnosticsViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsStore = DeviceSettingsStore(application)
    private val api = DeviceApiClient(settingsStore)

    private val _uiState = MutableStateFlow(DeviceDiagnosticsUiState())
    val uiState: StateFlow<DeviceDiagnosticsUiState> = _uiState.asStateFlow()

    // Raw JSON from the device's own responses, kept alongside the decoded
    // models so an exported report includes every field the firmware
    // returns -- not just the subset this app's UI displays.
    private var lastStatusRawJson: String? = null
    private var lastProbeRawJson: String? = null
    private var lastSelfTestRawJson: String? = null
    private var lastDisplayCommandRawJson: String? = null
    private var lastErrorContext: String? = null

    init {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(ipAddress = settingsStore.loadIpAddress().orEmpty())
        }
    }

    fun onIpAddressChanged(value: String) {
        _uiState.value = _uiState.value.copy(ipAddress = value)
    }

    fun saveAddress() {
        viewModelScope.launch { settingsStore.saveIpAddress(_uiState.value.ipAddress) }
    }

    fun refreshStatus() {
        saveAddress()
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val result = api.fetchStatus()
                lastStatusRawJson = result.rawJson
                _uiState.value = _uiState.value.copy(
                    status = result.value,
                    isLoading = false,
                    lastCheckedAtEpochMillis = System.currentTimeMillis(),
                )
            } catch (e: Exception) {
                lastErrorContext = "GET /status failed: $e"
                _uiState.value = _uiState.value.copy(
                    status = null,
                    isLoading = false,
                    errorMessage = e.message,
                )
            }
        }
    }

    fun triggerProbe() {
        saveAddress()
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val result = api.fetchProbe()
                lastProbeRawJson = result.rawJson
                _uiState.value = _uiState.value.copy(
                    probe = result.value,
                    isLoading = false,
                    lastCheckedAtEpochMillis = System.currentTimeMillis(),
                )
            } catch (e: Exception) {
                lastErrorContext = "GET /probe failed: $e"
                _uiState.value = _uiState.value.copy(
                    probe = null,
                    isLoading = false,
                    errorMessage = e.message,
                )
            }
        }
    }

    fun rebootDevice() {
        saveAddress()
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isResetting = true, errorMessage = null)
            try {
                api.reset()
                lastStatusRawJson = null
                lastProbeRawJson = null
                lastSelfTestRawJson = null
                lastDisplayCommandRawJson = null
                _uiState.value = _uiState.value.copy(
                    status = null,
                    probe = null,
                    selfTest = null,
                    displayResult = null,
                    lastDisplayCommand = null,
                    isResetting = false,
                )
            } catch (e: Exception) {
                lastErrorContext = "GET /reset failed: $e"
                _uiState.value = _uiState.value.copy(isResetting = false, errorMessage = e.message)
            }
        }
    }

    /**
     * Runs the internal RS485 bridge self-test on the device
     * (`POST /rs485/selftest`). Only exercises the SC16IS752 bridge/UART
     * core, not the physical bus or downstream sensor -- so it can pass
     * even when a sensor itself is disconnected or unresponsive.
     */
    fun triggerSelfTest() {
        saveAddress()
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRunningSelfTest = true, errorMessage = null)
            try {
                val result = api.triggerRs485SelfTest()
                lastSelfTestRawJson = result.rawJson
                _uiState.value = _uiState.value.copy(
                    selfTest = result.value,
                    isRunningSelfTest = false,
                    lastCheckedAtEpochMillis = System.currentTimeMillis(),
                )
            } catch (e: Exception) {
                lastErrorContext = "POST /rs485/selftest failed: $e"
                _uiState.value = _uiState.value.copy(
                    selfTest = null,
                    isRunningSelfTest = false,
                    errorMessage = e.message,
                )
            }
        }
    }

    val hasReportContent: Boolean
        get() = lastStatusRawJson != null || lastProbeRawJson != null ||
            lastSelfTestRawJson != null || lastDisplayCommandRawJson != null ||
            _uiState.value.errorMessage != null

    /**
     * Runs a display command (`status`, `refresh`, `clear`, `pause`,
     * `resume`, `reboot`, or `sleep`) via `/display/<command>`, the
     * Inkplate e-paper display's own local control API. `status` is
     * read-only; the rest actively change the attached display's state.
     */
    fun triggerDisplayCommand(command: String) {
        saveAddress()
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRunningDisplayCommand = true, errorMessage = null)
            try {
                val result = api.runDisplayCommand(command)
                lastDisplayCommandRawJson = result.rawJson
                _uiState.value = _uiState.value.copy(
                    displayResult = result.value,
                    lastDisplayCommand = command,
                    isRunningDisplayCommand = false,
                    lastCheckedAtEpochMillis = System.currentTimeMillis(),
                )
            } catch (e: Exception) {
                lastErrorContext = "/display/$command failed: $e"
                _uiState.value = _uiState.value.copy(
                    displayResult = null,
                    isRunningDisplayCommand = false,
                    errorMessage = e.message,
                )
            }
        }
    }

    /**
     * Builds a plain-text diagnostic report -- app/device metadata plus the
     * full raw `/status` and `/probe` JSON last fetched -- formatted so a
     * user can paste it directly into an AI assistant (Copilot, ChatGPT,
     * etc.) or a support ticket without needing to screenshot anything.
     */
    fun diagnosticsReportText(): String {
        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val state = _uiState.value
        val lines = mutableListOf<String>()
        lines += "Lake Logger device diagnostics report"
        lines += "Generated: ${isoFormat.format(Date())}"
        lines += "Device address: ${state.ipAddress.ifEmpty { "(not set)" }}"
        state.lastCheckedAtEpochMillis?.let {
            lines += "Last successful check: ${isoFormat.format(Date(it))}"
        }
        state.errorMessage?.let { lines += "Most recent error: $it" }
        lastErrorContext?.let { lines += "Error context: $it" }

        lines += ""
        lines += "--- /status ---"
        lines += lastStatusRawJson ?: "(not fetched yet)"

        lines += ""
        lines += "--- /probe ---"
        lines += lastProbeRawJson ?: "(not fetched yet)"

        lines += ""
        lines += "--- /rs485/selftest ---"
        lines += lastSelfTestRawJson ?: "(not run yet)"

        lines += ""
        lines += "--- /display/${state.lastDisplayCommand ?: "?"} ---"
        lines += lastDisplayCommandRawJson ?: "(not run yet)"

        return lines.joinToString("\n")
    }
}
