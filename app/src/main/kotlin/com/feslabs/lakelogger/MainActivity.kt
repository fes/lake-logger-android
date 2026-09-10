package com.feslabs.lakelogger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.feslabs.lakelogger.ui.LakeLoggerApp
import com.feslabs.lakelogger.widget.LakeRefreshWorker

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LakeRefreshWorker.schedulePeriodic(applicationContext)
        setContent {
            LakeLoggerApp()
        }
    }
}
