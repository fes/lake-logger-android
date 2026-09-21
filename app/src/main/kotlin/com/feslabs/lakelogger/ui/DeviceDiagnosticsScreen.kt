package com.feslabs.lakelogger.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.feslabs.lakelogger.data.DeviceDisplayCommandResult
import com.feslabs.lakelogger.data.DeviceProbeReading
import com.feslabs.lakelogger.data.DeviceRs485SelfTestResult
import com.feslabs.lakelogger.data.DeviceStatus
import com.feslabs.lakelogger.data.LakeFormat

/**
 * Lets the user talk directly to the logger device over the local network
 * (bypassing the feslabs.com cloud entirely), for diagnosing issues like a
 * stalled upload schedule even when the phone has no internet connectivity
 * but is on the same Wi-Fi as the device.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceDiagnosticsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val viewModel: DeviceDiagnosticsViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showResetConfirmation by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Device Diagnostics") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                OutlinedTextField(
                    value = uiState.ipAddress,
                    onValueChange = viewModel::onIpAddressChanged,
                    label = { Text("Device local IP address") },
                    placeholder = { Text("e.g. 10.2.12.247") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "Only reachable when your phone is on the same local Wi-Fi network as the logger.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Button(onClick = viewModel::refreshStatus) { Text("Check status") }
                    OutlinedButton(onClick = viewModel::triggerProbe) { Text("Trigger probe") }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    OutlinedButton(
                        onClick = viewModel::triggerSelfTest,
                        enabled = !uiState.isRunningSelfTest,
                    ) { Text("Run RS-485 self-test") }
                }
                DisplayCommandButtons(
                    onCommand = viewModel::triggerDisplayCommand,
                    enabled = !uiState.isRunningDisplayCommand,
                )
                TextButton(onClick = { showResetConfirmation = true }) {
                    Text("Reboot device", color = MaterialTheme.colorScheme.error)
                }
            }

            if (uiState.isLoading || uiState.isResetting || uiState.isRunningSelfTest || uiState.isRunningDisplayCommand) {
                item { CircularProgressIndicator() }
            }

            uiState.errorMessage?.let { message ->
                item {
                    Text(message, color = MaterialTheme.colorScheme.error)
                }
            }

            uiState.status?.let { status ->
                item { StatusSummary(status) }
            }

            uiState.probe?.let { probe ->
                item { ProbeSummary(probe) }
            }

            uiState.selfTest?.let { selfTest ->
                item { SelfTestSummary(selfTest) }
            }

            uiState.displayResult?.let { displayResult ->
                item { DisplayResultSummary(displayResult, command = uiState.lastDisplayCommand) }
            }

            if (viewModel.hasReportContent) {
                item {
                    ExportRow(
                        context = context,
                        reportText = viewModel::diagnosticsReportText,
                    )
                }
            }
        }
    }

    if (showResetConfirmation) {
        AlertDialog(
            onDismissRequest = { showResetConfirmation = false },
            title = { Text("Reboot logger device?") },
            text = {
                Text(
                    "This will power-cycle the physical device. Any unsent readings are " +
                        "kept in its local backlog, so this is safe.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showResetConfirmation = false
                    viewModel.rebootDevice()
                }) { Text("Reboot") }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmation = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun StatusSummary(status: DeviceStatus) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SectionHeader("Connectivity")
        DiagnosticRow("Site health", status.siteHealth)
        DiagnosticRow("Wi-Fi RSSI", status.wifiRssiDbm?.let { "$it dBm" })
        DiagnosticRow("Uptime", LakeFormat.relativeDuration(status.uptimeS))
        DiagnosticRow("Last reset reason", status.lastSystemResetReason)

        SectionHeader("Clock / NTP")
        DiagnosticRow("Clock valid", status.clockValid?.let { if (it) "Yes" else "No" })
        DiagnosticRow("Last NTP sync", status.lastNtpSyncAge)
        DiagnosticRow("Last sync delta", status.ntpSyncDeltaSeconds?.let { "$it s" })
        DiagnosticRow("Largest observed skew", status.ntpLargestObservedSkewSeconds?.let { "$it s" })
        DiagnosticRow("NTP failures (consecutive)", status.consecutiveNtpFailures?.toString())

        SectionHeader("Upload / probe schedule")
        DiagnosticRow("Last successful upload", status.lastSuccessfulUploadAge)
        DiagnosticRow(
            "Last upload error",
            status.lastUploadError?.takeIf { it.isNotEmpty() } ?: "None",
        )
        DiagnosticRow("Upload failures (consecutive)", status.consecutiveUploadFailures?.toString())
        DiagnosticRow("Backlog (unsent readings)", status.backlogCount?.toString())
        DiagnosticRow("Last successful probe", status.lastSuccessfulProbeReadAge)

        SectionHeader("Sensors / power")
        DiagnosticRow("Sensor found", status.sensorFound?.let { if (it) "Yes" else "No" })
        DiagnosticRow("Battery voltage", LakeFormat.volts(status.cachedProbeBatteryOutputVoltageV))
        DiagnosticRow("Solar voltage", LakeFormat.volts(status.cachedProbeSolarInputVoltageV))
        DiagnosticRow("Battery charge", LakeFormat.percent(status.batteryChargeLevelPctApprox))

        SectionHeader("Display")
        DiagnosticRow("Behavior", status.displayBehavior)
        DiagnosticRow("Backend", status.displayBackend)
        DiagnosticRow("Present", status.displayPresent?.let { if (it) "Yes" else "No" })
        DiagnosticRow("Awake", status.displayAwake?.let { if (it) "Yes" else "No" })
        DiagnosticRow("Last refresh", status.lastDisplayRefreshAge)
        DiagnosticRow("Refresh count", status.displayRefreshCount?.toString())
        DiagnosticRow("I2C recovery count", status.displayI2cRecoveryCount?.toString())
        DiagnosticRow("Link failures", status.displayLinkFailures?.toString())
        DiagnosticRow(
            "Last error",
            status.displayLastError?.takeIf { it.isNotEmpty() } ?: "None",
        )

        SectionHeader("RS-485 bridge")
        DiagnosticRow("Modbus failures (total)", status.modbusFailureTotal?.toString())
        DiagnosticRow("Solinst failures (consecutive)", status.consecutiveSolinstModbusFailures?.toString())
        DiagnosticRow("Weather failures (consecutive)", status.consecutiveWeatherModbusFailures?.toString())
        DiagnosticRow("Bridge recovery attempts", status.rs485BridgeRecoveryAttempts?.toString())
        DiagnosticRow("Bridge recovery successes", status.rs485BridgeRecoverySuccesses?.toString())
        BridgeHealthRow(
            label = "Solinst channel",
            supported = status.rs485SolinstBridgeHealthSupported,
            overrun = status.rs485SolinstBridgeOverrunError,
            parity = status.rs485SolinstBridgeParityError,
            framing = status.rs485SolinstBridgeFramingError,
            breakDetected = status.rs485SolinstBridgeBreakDetected,
        )
        BridgeHealthRow(
            label = "Weather channel",
            supported = status.rs485WeatherBridgeHealthSupported,
            overrun = status.rs485WeatherBridgeOverrunError,
            parity = status.rs485WeatherBridgeParityError,
            framing = status.rs485WeatherBridgeFramingError,
            breakDetected = status.rs485WeatherBridgeBreakDetected,
        )
    }
}

@Composable
private fun ProbeSummary(probe: DeviceProbeReading) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SectionHeader("Live probe result")
        DiagnosticRow("Timestamp (device UTC)", probe.timestampUtc)
        DiagnosticRow("Water level", LakeFormat.meters(probe.waterLevelM))
        DiagnosticRow("Water temperature", LakeFormat.celsius(probe.temperatureC))
        DiagnosticRow("Air temperature", LakeFormat.celsius(probe.weatherAirTemperatureC))
        DiagnosticRow("Humidity", LakeFormat.percent(probe.weatherRelativeHumidityPct))
    }
}

@Composable
private fun SelfTestSummary(selfTest: DeviceRs485SelfTestResult) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SectionHeader("RS-485 self-test result")
        SelfTestRow("Solinst channel", selfTest.solinstSelftestSupported, selfTest.solinstSelftestPassed)
        SelfTestRow("Weather channel", selfTest.weatherSelftestSupported, selfTest.weatherSelftestPassed)
        Text(
            "An internal loopback test of the bridge/UART core, not the physical bus or " +
                "sensors -- it can pass even with a sensor disconnected.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * A single "channel: pass/fail" row for the on-demand self-test result,
 * styled to stand out (green/red) since it directly answers "is the shared
 * bridge chip itself OK" independent of sensor wiring or responsiveness.
 */
@Composable
private fun SelfTestRow(label: String, supported: Boolean?, passed: Boolean?) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        val (text, color) = when {
            supported == false -> "Not supported" to MaterialTheme.colorScheme.onSurfaceVariant
            passed == true -> "Passed" to MaterialTheme.colorScheme.primary
            passed == false -> "Failed" to MaterialTheme.colorScheme.error
            else -> "—" to MaterialTheme.colorScheme.onSurfaceVariant
        }
        Text(text, style = MaterialTheme.typography.bodyMedium, color = color)
    }
}

/**
 * A compact "no errors" / "flag list" row for continuous bridge
 * line-status monitoring (distinct from the on-demand self-test above).
 */
@Composable
private fun BridgeHealthRow(
    label: String,
    supported: Boolean?,
    overrun: Boolean?,
    parity: Boolean?,
    framing: Boolean?,
    breakDetected: Boolean?,
) {
    val flags = listOfNotNull(
        "overrun".takeIf { overrun == true },
        "parity".takeIf { parity == true },
        "framing".takeIf { framing == true },
        "break".takeIf { breakDetected == true },
    )
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        val (text, color) = when {
            supported == false -> "Not supported" to MaterialTheme.colorScheme.onSurfaceVariant
            flags.isEmpty() -> "No errors" to MaterialTheme.colorScheme.onSurfaceVariant
            else -> flags.joinToString(", ") to MaterialTheme.colorScheme.tertiary
        }
        Text(text, style = MaterialTheme.typography.bodyMedium, color = color)
    }
}

/**
 * Buttons for the Inkplate e-paper display's own local control API
 * (`/display/<command>`). `status` is read-only; the rest actively change
 * the attached display's state, so they're grouped separately from the
 * status/probe/self-test actions above.
 */
@Composable
private fun DisplayCommandButtons(onCommand: (String) -> Unit, enabled: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SectionHeader("Display")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { onCommand("status") }, enabled = enabled) { Text("Status") }
            OutlinedButton(onClick = { onCommand("refresh") }, enabled = enabled) { Text("Refresh") }
            OutlinedButton(onClick = { onCommand("clear") }, enabled = enabled) { Text("Clear") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { onCommand("pause") }, enabled = enabled) { Text("Pause") }
            OutlinedButton(onClick = { onCommand("resume") }, enabled = enabled) { Text("Resume") }
            OutlinedButton(onClick = { onCommand("sleep") }, enabled = enabled) { Text("Sleep") }
        }
        TextButton(onClick = { onCommand("reboot") }, enabled = enabled) {
            Text("Reboot display", color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun ExportRow(context: Context, reportText: () -> String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SectionHeader("Export")
        Text(
            "Includes the full raw response from the device (not just the fields shown " +
                "above) plus any recent errors -- share this into Copilot, ChatGPT, or a " +
                "support message for help diagnosing an issue.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                val clipboard = context.getSystemService(ClipboardManager::class.java)
                clipboard?.setPrimaryClip(ClipData.newPlainText("Lake Logger diagnostics", reportText()))
            }) { Text("Copy report") }

            OutlinedButton(onClick = {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, reportText())
                    putExtra(Intent.EXTRA_SUBJECT, "Lake Logger device diagnostics")
                }
                context.startActivity(Intent.createChooser(shareIntent, "Share diagnostics report"))
            }) { Text("Share report…") }
        }
    }
}

@Composable
private fun DisplayResultSummary(result: DeviceDisplayCommandResult, command: String?) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SectionHeader("Display command result")
        DiagnosticRow("Command", command)
        DiagnosticRow("Result", result.ok?.let { if (it) "OK" else "Failed" })
        DiagnosticRow("Display backend", result.displayBackend)
        DiagnosticRow(
            "Response detail",
            result.response?.takeIf { it.isNotEmpty() } ?: "None",
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(title, style = MaterialTheme.typography.titleMedium)
}

@Composable
private fun DiagnosticRow(label: String, value: String?) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(
            value ?: "—",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
