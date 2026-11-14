package com.companion.yogawellness

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.companion.yogawellness.ui.AppNavHost
import com.companion.yogawellness.ui.theme.YogaWellnessTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // System-bar icons follow the theme (dark on light screens); the camera screen overrides it.
        enableEdgeToEdge()
        setContent {
            YogaWellnessTheme {
                AppNavHost()
            }
        }
    }
}
