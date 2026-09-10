package com.feslabs.lakelogger.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.feslabs.lakelogger.data.LakeFormat
import com.feslabs.lakelogger.data.LakeReading

@Composable
fun StatusHeaderCard(reading: LakeReading, lastFetchedAtEpochMillis: Long?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    text = if (reading.isOk) "Online" else (reading.status ?: "Unknown"),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "Reading: ${LakeFormat.relativeTime(reading.timestampEpochMillis)}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Text(
                text = "Updated ${LakeFormat.relativeTime(lastFetchedAtEpochMillis)}",
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
fun WaterLevelCard(reading: LakeReading) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Water Level", style = MaterialTheme.typography.labelMedium)
                Text(
                    LakeFormat.meters(reading.waterLevelM),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Water Temp", style = MaterialTheme.typography.labelMedium)
                Text(
                    LakeFormat.celsius(reading.temperatureC),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
fun BatterySolarCard(reading: LakeReading) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text("Power", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.padding(4.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                PowerColumn(
                    modifier = Modifier.weight(1f),
                    title = "Battery",
                    voltage = reading.batteryOutputVoltageV,
                    current = reading.batteryOutputCurrentA,
                    present = reading.batteryOutputMonitorPresent,
                    valid = reading.batteryOutputMonitorValid,
                )
                PowerColumn(
                    modifier = Modifier.weight(1f),
                    title = "Solar",
                    voltage = reading.solarInputVoltageV,
                    current = reading.solarInputCurrentA,
                    present = reading.solarInputMonitorPresent,
                    valid = reading.solarInputMonitorValid,
                )
            }
            reading.batteryChargeLevelPctApprox?.let { pct ->
                Spacer(modifier = Modifier.padding(4.dp))
                Text("Charge: ${LakeFormat.percent(pct)}", style = MaterialTheme.typography.bodySmall)
                LinearProgressIndicator(
                    progress = { (pct / 100).toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun PowerColumn(
    modifier: Modifier = Modifier,
    title: String,
    voltage: Double?,
    current: Double?,
    present: Boolean?,
    valid: Boolean?,
) {
    Column(modifier = modifier) {
        Text(title, style = MaterialTheme.typography.labelMedium)
        when {
            present == false -> Text("Not present", style = MaterialTheme.typography.bodySmall)
            valid == false -> Text(
                "Invalid reading",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
            else -> {
                Text(LakeFormat.volts(voltage), fontWeight = FontWeight.Bold)
                Text(LakeFormat.amps(current), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun WeatherCard(reading: LakeReading) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text("Weather", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.padding(4.dp))
            when {
                reading.weatherEnabled == false ->
                    Text("Weather station not configured", style = MaterialTheme.typography.bodySmall)
                reading.weatherPresent == false ->
                    Text(
                        "Weather station not responding",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                reading.weatherValid == false ->
                    Text(
                        reading.weatherLastError ?: "Weather reading invalid",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                else -> {
                    val stats = buildList {
                        add("Air Temp" to LakeFormat.celsius(reading.weatherAirTemperatureC))
                        add("Humidity" to LakeFormat.humidity(reading.weatherRelativeHumidityPct))
                        add("Pressure" to LakeFormat.pressure(reading.weatherBarometricPressureHpa))
                        add(
                            "Wind" to "${LakeFormat.windSpeed(reading.weatherWindSpeedMS)} " +
                                LakeFormat.compassDirection(reading.weatherWindDirectionDeg)
                        )
                        add("Rainfall" to LakeFormat.rainfall(reading.weatherRainfallMm))
                        reading.weatherLightLux?.let { lux ->
                            add("Light" to "${lux.toInt()} lux")
                        }
                    }
                    // A plain non-lazy 2-column grid: this card already lives
                    // inside the dashboard's outer LazyColumn, and nesting a
                    // LazyVerticalGrid there crashes with "infinity maximum
                    // height constraints" since two lazy scrollables can't be
                    // nested vertically.
                    Column(modifier = Modifier.fillMaxWidth()) {
                        stats.chunked(2).forEach { rowStats ->
                            Row(modifier = Modifier.fillMaxWidth()) {
                                rowStats.forEach { (title, value) ->
                                    Column(modifier = Modifier.weight(1f).padding(vertical = 4.dp)) {
                                        Text(title, style = MaterialTheme.typography.labelSmall)
                                        Text(value, fontWeight = FontWeight.Bold)
                                    }
                                }
                                if (rowStats.size < 2) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WaterLevelHistoryCard(readings: List<LakeReading>) {
    val points = readings.mapNotNull { r ->
        val t = r.timestampEpochMillis
        val level = r.waterLevelM
        if (t != null && level != null) t to level else null
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text("Water Level History", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.padding(4.dp))
            if (points.isEmpty()) {
                Text("Not enough history data yet.", style = MaterialTheme.typography.bodySmall)
            } else {
                WaterLevelSparkline(points = points, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
