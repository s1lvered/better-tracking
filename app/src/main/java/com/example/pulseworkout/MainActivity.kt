package com.example.pulseworkout

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.pulseworkout.ui.AppViewModel
import com.example.pulseworkout.ui.BetterTrackingApp
import com.example.pulseworkout.ui.theme.BetterTrackingTheme

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    @androidx.annotation.VisibleForTesting
    fun viewModelForTest(): AppViewModel = vm

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setContent {
            BetterTrackingTheme { BetterTrackingApp(vm) }
        }
    }
}
