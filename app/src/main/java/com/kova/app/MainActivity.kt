package com.kova.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.kova.app.domain.model.ThemeMode
import com.kova.app.ui.navigation.KovaAppRoot
import com.kova.app.ui.state.KovaViewModel
import com.kova.app.ui.theme.KovaTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

class MainActivity : ComponentActivity() {

    private var canDrawOverlaysState by mutableStateOf(false)
    private var requestedNotificationPermissionOnce = false

    private val viewModel: KovaViewModel by viewModels {
        val app = application as KovaApplication
        KovaViewModel.factory(
            repository = app.repository,
            restTimerManager = app.restTimerManager,
            cloudSyncProvider = app.cloudSyncProvider
        )
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Ongoing notification works if granted; floating overlay + vibration work regardless */ }

    private val exportBackupLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@registerForActivityResult
        lifecycleScope.launch {
            runCatching {
                val json = viewModel.exportBackupJson()
                withContext(Dispatchers.IO) {
                    contentResolver.openOutputStream(uri)?.use { stream ->
                        stream.write(json.toByteArray(Charsets.UTF_8))
                    } ?: error("No se pudo abrir el archivo destino")
                }
            }.onSuccess {
                viewModel.showMessage("Copia de seguridad exportada correctamente")
            }.onFailure { err ->
                viewModel.showMessage("Error al exportar: ${err.message ?: "desconocido"}")
            }
        }
    }

    private val importBackupLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@registerForActivityResult
        lifecycleScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                        ?: error("No se pudo leer el archivo")
                }
            }.onSuccess { json ->
                viewModel.importBackupJson(json)
            }.onFailure { err ->
                viewModel.showMessage("Error al leer copia: ${err.message ?: "desconocido"}")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        canDrawOverlaysState = Settings.canDrawOverlays(this)

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val restTimerState by viewModel.restTimerState.collectAsStateWithLifecycle()
            val prefs = uiState.preferences
            val isDark = when (prefs.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            LaunchedEffect(restTimerState.isVisible) {
                if (restTimerState.isVisible) {
                    maybeRequestNotificationsPermission()
                }
            }

            LaunchedEffect(isDark) {
                enableEdgeToEdge(
                    statusBarStyle = if (isDark) {
                        SystemBarStyle.dark(Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                    },
                    navigationBarStyle = if (isDark) {
                        SystemBarStyle.dark(Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                    }
                )
            }

            KovaTheme(
                themeMode = prefs.themeMode,
                accentPalette = prefs.accentPalette
            ) {
                KovaAppRoot(
                    viewModel = viewModel,
                    canDrawOverlays = canDrawOverlaysState,
                    onRequestOverlayPermission = { openOverlayPermissionSettings() },
                    onRequestExportJson = {
                        val fileName = "kova-backup-${LocalDate.now()}.json"
                        exportBackupLauncher.launch(fileName)
                    },
                    onRequestImportJson = {
                        importBackupLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        canDrawOverlaysState = Settings.canDrawOverlays(this)
    }

    private fun maybeRequestNotificationsPermission() {
        if (requestedNotificationPermissionOnce) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                requestedNotificationPermissionOnce = true
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun openOverlayPermissionSettings() {
        maybeRequestNotificationsPermission()
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        runCatching { startActivity(intent) }.onFailure {
            runCatching {
                startActivity(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:$packageName")
                    )
                )
            }
        }
    }
}
