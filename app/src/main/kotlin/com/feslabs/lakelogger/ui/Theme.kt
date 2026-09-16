package com.feslabs.lakelogger.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme

private val LakeBlue = Color(0xFF1565C0)
private val LakeAmber = Color(0xFFB08321)

private val LightColors = lightColorScheme(primary = LakeBlue, secondary = LakeAmber)
private val DarkColors = darkColorScheme(primary = LakeBlue, secondary = LakeAmber)

@Composable
fun LakeLoggerTheme(darkTheme: Boolean = androidx.compose.foundation.isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, content = content)
}

@Composable
fun LakeLoggerApp() {
    LakeLoggerTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            var showDeviceDiagnostics by remember { mutableStateOf(false) }
            if (showDeviceDiagnostics) {
                DeviceDiagnosticsScreen(onBack = { showDeviceDiagnostics = false })
            } else {
                DashboardScreen(onOpenDeviceDiagnostics = { showDeviceDiagnostics = true })
            }
        }
    }
}
