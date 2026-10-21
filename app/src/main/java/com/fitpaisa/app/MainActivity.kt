package com.fitpaisa.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import com.fitpaisa.app.core.sensor.StepTrackingService
import com.fitpaisa.app.navigation.AppNav
import com.fitpaisa.app.ui.theme.FitPaisaTheme
import com.fitpaisa.app.ui.theme.ForestGreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var notificationScheduler: com.fitpaisa.app.core.notifications.NotificationScheduler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // The app is always dark: dark status bar with light icons.
        window.statusBarColor = ForestGreen.toArgb()
        WindowCompat.getInsetsController(window, window.decorView)
            .isAppearanceLightStatusBars = false

        // Schedule daily walk/convert reminders.
        notificationScheduler.scheduleDaily()
        StepTrackingService.start(this)
        requestNotificationPermission()

        setContent {
            FitPaisaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNav()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Also covers the moment after the user grants activity permission.
        StepTrackingService.start(this)
    }

    /** Asks for notification permission on Android 13+. */
    private fun requestNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
                android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1001)
            }
        }
    }
}
