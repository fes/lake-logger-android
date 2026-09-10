package com.feslabs.lakelogger.widget

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.feslabs.lakelogger.MainActivity
import com.feslabs.lakelogger.data.LakeFormat

@Composable
fun LakeWidgetContent(state: LakeWidgetState) {
    val context = LocalContext.current

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(Color(0xFFEFF6FF))
            .cornerRadius(16.dp)
            .padding(12.dp)
            .clickable(actionStartActivity(Intent(context, MainActivity::class.java))),
    ) {
        when (state) {
            is LakeWidgetState.Data -> DataContent(state)
            is LakeWidgetState.Unavailable -> UnavailableContent(state.message)
        }
    }
}

@Composable
private fun DataContent(state: LakeWidgetState.Data) {
    val reading = state.reading

    Text(
        "Lake Level",
        style = TextStyle(fontSize = 11.sp, color = ColorProvider(Color.DarkGray)),
    )
    Text(
        LakeFormat.meters(reading.waterLevelM),
        style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold),
    )
    Text(
        LakeFormat.celsius(reading.temperatureC),
        style = TextStyle(fontSize = 14.sp, color = ColorProvider(Color.DarkGray)),
    )
    Row(
        modifier = GlanceModifier.fillMaxWidth().padding(top = 6.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        Text("\uD83D\uDD0B ${LakeFormat.percent(reading.batteryChargeLevelPctApprox)}", style = TextStyle(fontSize = 12.sp))
        Text("  \u2600\uFE0F ${LakeFormat.volts(reading.solarInputVoltageV)}", style = TextStyle(fontSize = 12.sp))
    }
    Text(
        LakeFormat.relativeTime(reading.timestampEpochMillis),
        style = TextStyle(fontSize = 10.sp, color = ColorProvider(Color.Gray)),
    )
}

@Composable
private fun UnavailableContent(message: String) {
    Column(
        modifier = GlanceModifier.fillMaxSize(),
        verticalAlignment = Alignment.Vertical.CenterVertically,
        horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
    ) {
        Text("\u26A0\uFE0F Lake Logger", style = TextStyle(fontWeight = FontWeight.Bold))
        Text(message, style = TextStyle(fontSize = 11.sp))
    }
}
