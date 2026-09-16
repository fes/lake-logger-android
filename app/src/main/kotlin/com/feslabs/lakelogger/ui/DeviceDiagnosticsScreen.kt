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
import com.feslabs.lakelogger.data.DeviceProbeReading
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
                TextButton(onClick = { showResetConfirmation = true }) {
                    Text("Reboot device", color = MaterialTheme.colorScheme.error)
                }
            }

            if (uiState.isLoading || uiState.isResetting) {
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
