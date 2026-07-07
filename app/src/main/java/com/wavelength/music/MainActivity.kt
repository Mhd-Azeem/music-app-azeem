package com.wavelength.music

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wavelength.music.ui.components.OfflineBanner
import com.wavelength.music.ui.navigation.WavelengthNavHost
import com.wavelength.music.ui.settings.AppSettingsViewModel
import com.wavelength.music.ui.theme.WavelengthTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            val settingsViewModel: AppSettingsViewModel = hiltViewModel()
            val settings by settingsViewModel.state.collectAsStateWithLifecycle()

            WavelengthTheme(theme = settings.theme) {
                Column(modifier = Modifier.fillMaxSize()) {
                    OfflineBanner()
                    Box(modifier = Modifier.fillMaxWidth().weight(1f).background(Color.Black)) {
                        WavelengthNavHost()
                    }
                }
            }
        }
    }
}
