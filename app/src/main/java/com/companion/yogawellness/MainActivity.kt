package com.companion.yogawellness

import android.os.Bundle
import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.companion.yogawellness.ui.AppNavHost
import com.companion.yogawellness.ui.theme.YogaWellnessTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Every screen opens on the navy/teal brand header, so status-bar icons are always light.
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT))
        setContent {
            YogaWellnessTheme {
                AppNavHost()
            }
        }
    }
}
