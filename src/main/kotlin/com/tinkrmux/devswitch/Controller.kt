@file:Suppress("UNCHECKED_CAST")

package com.tinkrmux.devswitch

import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.Snapshot
import com.android.ddmlib.IDevice
import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.ProjectManager
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

val LocalController = compositionLocalOf<Controller> {
    error("No controller provided")
}

private const val AUTO_REFRESH_KEY = "com.tinkrmux.devswitch.autoRefreshEnabled"
private const val REFRESH_INTERVAL_KEY = "com.tinkrmux.devswitch.refreshInterval"
private const val DEFAULT_REFRESH_INTERVAL = 1000L

// Available refresh intervals in milliseconds
val REFRESH_INTERVALS = listOf(
    500L to "0.5s",
    1000L to "1s",
    2000L to "2s",
    3000L to "3s",
    5000L to "5s",
    10000L to "10s",
    30000L to "30s",
    60000L to "1m"
)

class Controller : CoroutineScope {

    private val job = SupervisorJob()
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        println("exception occurred: $throwable")
        showError("ADB Error", throwable.message ?: "Unknown error occurred")
        autoRefreshEnabled = false
    }

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default + job + exceptionHandler

    val actions = mutableMapOf<String, SettingsAction<*>>()
    val settingsState = mutableMapOf<String, MutableState<*>>()

    var selectedDevice by mutableStateOf<IDevice?>(null)
    
    // Error state for displaying in UI
    var lastError by mutableStateOf<String?>(null)
        private set

    // auto refresh - persisted
    private val _autoRefreshEnabled = mutableStateOf(
        PropertiesComponent.getInstance().getBoolean(AUTO_REFRESH_KEY, true)
    )
    var autoRefreshEnabled: Boolean
        get() = _autoRefreshEnabled.value
        set(value) {
            _autoRefreshEnabled.value = value
            PropertiesComponent.getInstance().setValue(AUTO_REFRESH_KEY, value, true)
        }
    
    // refresh interval - persisted
    private val _refreshInterval = mutableStateOf(
        PropertiesComponent.getInstance().getLong(REFRESH_INTERVAL_KEY, DEFAULT_REFRESH_INTERVAL)
    )
    var refreshInterval: Long
        get() = _refreshInterval.value
        set(value) {
            _refreshInterval.value = value
            PropertiesComponent.getInstance().setValue(REFRESH_INTERVAL_KEY, value.toString())
            // Restart auto-refresh with new interval if running
            if (autoRefreshEnabled && selectedDevice != null) {
                startAutoRefresh()
            }
        }
    
    private var autoRefreshJob: Job? = null

    fun startAutoRefresh() {
        autoRefreshJob?.cancel()
        autoRefreshJob = launch {
            while (true) {
                refreshAll()
                delay(refreshInterval)
            }
        }
    }

    fun stopAutoRefresh() {
        autoRefreshJob?.cancel()
    }
    
    fun clearError() {
        lastError = null
    }
    
    fun showError(title: String, message: String) {
        lastError = message
        // Show IDE notification
        try {
            val project = ProjectManager.getInstance().openProjects.firstOrNull()
            NotificationGroupManager.getInstance()
                .getNotificationGroup("DevSwitch Notifications")
                .createNotification(title, message, NotificationType.ERROR)
                .notify(project)
        } catch (e: Exception) {
            // Fallback if notification group not registered
            println("DevSwitch Error: $title - $message")
        }
    }
    
    fun showWarning(title: String, message: String) {
        try {
            val project = ProjectManager.getInstance().openProjects.firstOrNull()
            NotificationGroupManager.getInstance()
                .getNotificationGroup("DevSwitch Notifications")
                .createNotification(title, message, NotificationType.WARNING)
                .notify(project)
        } catch (e: Exception) {
            println("DevSwitch Warning: $title - $message")
        }
    }

    private fun refreshAll() {
        Snapshot.withMutableSnapshot {
            actions.onEach { (key, action) -> refreshState(key, action) }
        }
    }

    private fun refreshState(key: String, action: SettingsAction<*>) =
        launch {
            try {
                when (action) {
                    is ToggleAction -> {
                        (settingsState[key] as? MutableState<Boolean>)?.run {
                            selectedDevice?.let {
                                if (it.isOnline) {
                                    value = action.getValue(it)
                                }
                            }
                        }
                    }

                    is RangeAction -> {
                        (settingsState[key] as? MutableState<String>)?.run {
                            selectedDevice?.let {
                                if (it.isOnline) {
                                    value = action.getValue(it)
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Don't spam errors during refresh, just log
                println("Failed to refresh $key: ${e.message}")
            }
        }
    
    fun dispose() {
        job.cancel()
    }
    
    fun toggleSetting(settingId: String) {
        when (settingId) {
            // Network
            "wifi" -> changeWifi(!wifiState.value)
            "data" -> changeData(!dataState.value)
            "airplane_mode" -> changeAirplaneMode(!airplaneModeState.value)
            "bluetooth" -> changeBluetooth(!bluetoothState.value)
            
            // Debug Overlays
            "layout_bounds" -> changeLayoutBounds(!layoutBoundsState.value)
            "hwui" -> changeHwui(!hwuiState.value)
            "show_taps" -> changeShowTaps(!showTapsState.value)
            "show_fps" -> changeShowFps(!showFpsState.value)
            "pointer_location" -> changePointerLocation(!pointerLocationState.value)
            "gpu_overdraw" -> changeGpuOverdraw(!gpuOverdrawState.value)
            "surface_updates" -> changeSurfaceUpdates(!surfaceUpdatesState.value)
            "strict_mode" -> changeStrictMode(!strictModeState.value)
            
            // Developer Options
            "stay_awake" -> changeStayAwake(!stayAwakeState.value)
            "dont_keep_activities" -> changeDontKeepActivities(!dontKeepActivitiesState.value)
            "force_rtl" -> changeForceRtl(!forceRtlState.value)
            "usb_debugging" -> changeUsbDebugging(!usbDebuggingState.value)
            "demo_mode" -> changeDemoMode(!demoModeState.value)
            
            // Display & Accessibility
            "dark_mode" -> changeDarkMode(!darkModeState.value)
            "high_contrast" -> changeHighContrast(!highContrastState.value)
            "color_inversion" -> changeColorInversion(!colorInversionState.value)
            "magnification" -> changeMagnification(!magnificationState.value)
            "bold_text" -> changeBoldText(!boldTextState.value)
            "talkback" -> changeTalkBack(!talkBackState.value)
            "auto_brightness" -> changeAutoBrightness(!autoBrightnessState.value)
            
            // Battery
            "battery_saver" -> changeBatterySaver(!batterySaverState.value)
        }
    }
}
