package com.tinkrmux.devswitch

import com.android.ddmlib.IDevice
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.intellij.ide.util.PropertiesComponent
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// ============================================================================
// Utility Functions
// ============================================================================

/**
 * Generate a timestamped filename for screenshots.
 */
fun getScreenshotFilename(prefix: String = "screenshot"): String {
    val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))
    return "${prefix}_$timestamp.png"
}

/**
 * Generate a timestamped filename for recordings.
 */
fun getRecordingFilename(prefix: String = "recording"): String {
    val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))
    return "${prefix}_$timestamp.mp4"
}

// ============================================================================
// Device Extension Functions
// ============================================================================

/**
 * Take a screenshot and save to local path.
 */
suspend fun IDevice.takeScreenshot(outputPath: String): Boolean {
    return try {
        val devicePath = "/sdcard/devswitch_screenshot.png"
        executeShellCommand("screencap -p $devicePath")
        pullFile(devicePath, outputPath)
        executeShellCommand("rm $devicePath")
        File(outputPath).exists()
    } catch (e: Exception) {
        false
    }
}

/**
 * Take a clean screenshot with demo mode enabled.
 */
suspend fun IDevice.takeCleanScreenshot(outputPath: String): Boolean {
    return try {
        // Enable demo mode temporarily
        executeShellCommand("settings put global sysui_demo_allowed 1")
        executeShellCommand("am broadcast -a com.android.systemui.demo -e command enter")
        executeShellCommand("am broadcast -a com.android.systemui.demo -e command clock -e hhmm 1200")
        executeShellCommand("am broadcast -a com.android.systemui.demo -e command battery -e level 100 -e plugged false")
        executeShellCommand("am broadcast -a com.android.systemui.demo -e command network -e wifi show -e level 4")
        executeShellCommand("am broadcast -a com.android.systemui.demo -e command notifications -e visible false")
        
        // Wait a moment for UI to update
        kotlinx.coroutines.delay(200)
        
        // Take screenshot
        val result = takeScreenshot(outputPath)
        
        // Disable demo mode
        executeShellCommand("am broadcast -a com.android.systemui.demo -e command exit")
        
        result
    } catch (e: Exception) {
        // Make sure to exit demo mode even on error
        executeShellCommand("am broadcast -a com.android.systemui.demo -e command exit")
        false
    }
}

/**
 * Get installed packages from the device.
 */
suspend fun IDevice.getInstalledPackages(includeSystem: Boolean = false): List<String> {
    val flag = if (includeSystem) "" else "-3"
    val result = executeShellCommand("pm list packages $flag")
    return result.lines()
        .filter { it.startsWith("package:") }
        .map { it.removePrefix("package:").trim() }
        .sorted()
}

/**
 * Get the foreground app package name.
 */
suspend fun IDevice.getForegroundApp(): String? {
    val result = executeShellCommand("dumpsys activity activities | grep mResumedActivity")
    val match = Regex("u0\\s+([\\w.]+)/").find(result)
    return match?.groupValues?.get(1)
}

/**
 * Force stop an app.
 */
suspend fun IDevice.forceStopApp(packageName: String) {
    executeShellCommand("am force-stop $packageName")
}

/**
 * Clear app data.
 */
suspend fun IDevice.clearAppData(packageName: String) {
    executeShellCommand("pm clear $packageName")
}

/**
 * Launch an app.
 */
suspend fun IDevice.launchApp(packageName: String) {
    executeShellCommand("monkey -p $packageName -c android.intent.category.LAUNCHER 1")
}

/**
 * Launch a deep link URL.
 */
suspend fun IDevice.launchDeepLink(url: String, packageName: String? = null) {
    val pkgArg = packageName?.let { "-p $it" } ?: ""
    executeShellCommand("am start -a android.intent.action.VIEW -d \"$url\" $pkgArg")
}

/**
 * Launch a deep link with specific flags.
 */
suspend fun IDevice.launchDeepLinkWithFlags(
    url: String,
    packageName: String? = null,
    clearTask: Boolean = false,
    newTask: Boolean = true
) {
    val flags = buildList {
        if (newTask) add("--activity-brought-to-front")
        if (clearTask) add("--activity-clear-task")
    }.joinToString(" ")
    
    val pkgArg = packageName?.let { "-p $it" } ?: ""
    executeShellCommand("am start -a android.intent.action.VIEW -d \"$url\" $pkgArg $flags")
}

/**
 * Send a common broadcast.
 */
suspend fun IDevice.sendCommonBroadcast(broadcastName: String) {
    val broadcast = COMMON_BROADCASTS.find { it.name == broadcastName }
    if (broadcast != null) {
        executeShellCommand("am broadcast -a ${broadcast.action}")
    }
}

// ============================================================================
// Common Broadcasts Data
// ============================================================================

data class BroadcastInfo(
    val name: String,
    val action: String,
    val description: String
)

val COMMON_BROADCASTS = listOf(
    BroadcastInfo("Boot Completed", "android.intent.action.BOOT_COMPLETED", "Simulate device boot"),
    BroadcastInfo("Screen On", "android.intent.action.SCREEN_ON", "Screen turned on"),
    BroadcastInfo("Screen Off", "android.intent.action.SCREEN_OFF", "Screen turned off"),
    BroadcastInfo("Battery Low", "android.intent.action.BATTERY_LOW", "Battery level low"),
    BroadcastInfo("Battery OK", "android.intent.action.BATTERY_OKAY", "Battery level OK"),
    BroadcastInfo("Power Connected", "android.intent.action.ACTION_POWER_CONNECTED", "Charger connected"),
    BroadcastInfo("Power Disconnected", "android.intent.action.ACTION_POWER_DISCONNECTED", "Charger disconnected"),
    BroadcastInfo("Connectivity Change", "android.net.conn.CONNECTIVITY_CHANGE", "Network connectivity changed"),
    BroadcastInfo("Airplane Mode", "android.intent.action.AIRPLANE_MODE", "Airplane mode changed"),
    BroadcastInfo("Locale Changed", "android.intent.action.LOCALE_CHANGED", "Locale/language changed"),
    BroadcastInfo("Timezone Changed", "android.intent.action.TIMEZONE_CHANGED", "Timezone changed"),
    BroadcastInfo("Time Changed", "android.intent.action.TIME_SET", "System time changed"),
    BroadcastInfo("Date Changed", "android.intent.action.DATE_CHANGED", "System date changed"),
    BroadcastInfo("Package Added", "android.intent.action.PACKAGE_ADDED", "App installed"),
    BroadcastInfo("Package Removed", "android.intent.action.PACKAGE_REMOVED", "App uninstalled")
)

// ============================================================================
// Device Info
// ============================================================================

data class DeviceInfo(
    val model: String,
    val manufacturer: String,
    val androidVersion: String,
    val apiLevel: String,
    val screenResolution: String,
    val screenDensity: String
)

suspend fun IDevice.getDeviceInfo(): DeviceInfo {
    val model = executeShellCommand("getprop ro.product.model").trim()
    val manufacturer = executeShellCommand("getprop ro.product.manufacturer").trim()
    val androidVersion = executeShellCommand("getprop ro.build.version.release").trim()
    val apiLevel = executeShellCommand("getprop ro.build.version.sdk").trim()
    
    val wmSize = executeShellCommand("wm size")
    val resolution = Regex("(\\d+x\\d+)").find(wmSize)?.value ?: "Unknown"
    
    val wmDensity = executeShellCommand("wm density")
    val density = Regex("(\\d+)").find(wmDensity)?.value ?: "Unknown"
    
    return DeviceInfo(
        model = model,
        manufacturer = manufacturer,
        androidVersion = androidVersion,
        apiLevel = apiLevel,
        screenResolution = resolution,
        screenDensity = density
    )
}

// ============================================================================
// Recording Manager
// ============================================================================

class RecordingManager {
    private var isRecording = false
    private val deviceRecordingPath = "/sdcard/devswitch_recording.mp4"
    
    suspend fun startRecording(device: IDevice, maxDuration: Int = 180): Boolean {
        return try {
            isRecording = true
            // Start recording in background (will auto-stop after maxDuration)
            device.executeShellCommand("screenrecord --time-limit $maxDuration $deviceRecordingPath &")
            true
        } catch (e: Exception) {
            isRecording = false
            false
        }
    }
    
    suspend fun stopRecording(device: IDevice, outputPath: String): Boolean {
        return try {
            // Stop recording
            device.executeShellCommand("pkill -SIGINT screenrecord")
            kotlinx.coroutines.delay(500) // Wait for file to be written
            
            // Pull the file
            device.pullFile(deviceRecordingPath, outputPath)
            
            // Clean up device file
            device.executeShellCommand("rm $deviceRecordingPath")
            
            isRecording = false
            File(outputPath).exists()
        } catch (e: Exception) {
            isRecording = false
            false
        }
    }
    
    suspend fun cancelRecording(device: IDevice) {
        try {
            device.executeShellCommand("pkill -SIGINT screenrecord")
            device.executeShellCommand("rm $deviceRecordingPath")
        } catch (e: Exception) {
            // Ignore
        }
        isRecording = false
    }
}

// ============================================================================
// Presets Manager
// ============================================================================

data class SettingsPreset(
    val name: String,
    val description: String = "",
    val settings: Map<String, Any>,
    val createdAt: Long = System.currentTimeMillis()
)

class PresetsManager {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private val properties = PropertiesComponent.getInstance()
    private val presetsKey = "devswitch.presets"

    fun getAllPresets(): List<SettingsPreset> {
        val json = properties.getValue(presetsKey)
        if (json.isNullOrBlank()) {
            return getDefaultPresets()
        }
        return try {
            val type = object : TypeToken<List<SettingsPreset>>() {}.type
            val userPresets: List<SettingsPreset>? = gson.fromJson(json, type)
            (userPresets ?: emptyList()) + getDefaultPresets()
        } catch (e: Exception) {
            getDefaultPresets()
        }
    }

    fun savePreset(preset: SettingsPreset) {
        val presets = getUserPresets().toMutableList()
        presets.removeAll { it.name == preset.name }
        presets.add(0, preset)
        val json = gson.toJson(presets)
        properties.setValue(presetsKey, json)
    }

    fun deletePreset(name: String) {
        val presets = getUserPresets().filter { it.name != name }
        val json = gson.toJson(presets)
        properties.setValue(presetsKey, json)
    }
    
    private fun getUserPresets(): List<SettingsPreset> {
        val json = properties.getValue(presetsKey) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<SettingsPreset>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun isDefaultPreset(name: String): Boolean {
        return getDefaultPresets().any { it.name == name }
    }

    private fun getDefaultPresets(): List<SettingsPreset> = listOf(
        SettingsPreset(
            name = "UI Testing",
            description = "Optimal settings for UI/screenshot testing",
            settings = mapOf(
                "demo_mode" to true,
                "animations" to "off",
                "show_touches" to false,
                "stay_awake" to true
            )
        ),
        SettingsPreset(
            name = "Accessibility Testing",
            description = "Settings for accessibility testing",
            settings = mapOf(
                "font_scale" to "1.3x",
                "high_contrast" to true,
                "talkback" to false
            )
        ),
        SettingsPreset(
            name = "Performance Debug",
            description = "Debug overlays for performance analysis",
            settings = mapOf(
                "show_fps" to true,
                "gpu_overdraw" to true,
                "hwui" to true,
                "strict_mode" to true
            )
        ),
        SettingsPreset(
            name = "Layout Debug",
            description = "Visual aids for layout debugging",
            settings = mapOf(
                "layout_bounds" to true,
                "show_touches" to true,
                "pointer_location" to true
            )
        ),
        SettingsPreset(
            name = "RTL Testing",
            description = "Test right-to-left layouts",
            settings = mapOf(
                "force_rtl" to true
            )
        ),
        SettingsPreset(
            name = "Default/Reset",
            description = "Reset to default developer settings",
            settings = mapOf(
                "demo_mode" to false,
                "animations" to "1x",
                "show_touches" to false,
                "layout_bounds" to false,
                "show_fps" to false,
                "gpu_overdraw" to false,
                "hwui" to false,
                "strict_mode" to false,
                "force_rtl" to false,
                "pointer_location" to false
            )
        )
    )
}

// ============================================================================
// Controller Extensions for Presets
// ============================================================================

suspend fun Controller.captureCurrentSettings(
    device: IDevice,
    name: String,
    description: String
): SettingsPreset {
    val settings = mutableMapOf<String, Any>()
    
    // Capture toggle states
    settings["layout_bounds"] = layoutBoundsState.value
    settings["show_taps"] = showTapsState.value
    settings["stay_awake"] = stayAwakeState.value
    settings["hwui"] = hwuiState.value
    settings["force_rtl"] = forceRtlState.value
    settings["strict_mode"] = strictModeState.value
    settings["pointer_location"] = pointerLocationState.value
    settings["demo_mode"] = demoModeState.value
    settings["dark_mode"] = darkModeState.value
    settings["high_contrast"] = highContrastState.value
    settings["color_inversion"] = colorInversionState.value
    settings["talkback"] = talkBackState.value
    
    // Capture range states
    settings["animations"] = animationScaleState.value
    settings["font_scale"] = fontScaleState.value
    
    return SettingsPreset(
        name = name,
        description = description,
        settings = settings
    )
}

suspend fun Controller.applyPreset(device: IDevice, preset: SettingsPreset) {
    preset.settings.forEach { (key, value) ->
        when (key) {
            // Toggles
            "layout_bounds" -> if (value is Boolean) changeLayoutBounds(value)
            "show_taps", "show_touches" -> if (value is Boolean) changeShowTaps(value)
            "stay_awake" -> if (value is Boolean) changeStayAwake(value)
            "hwui" -> if (value is Boolean) changeHwui(value)
            "force_rtl" -> if (value is Boolean) changeForceRtl(value)
            "strict_mode" -> if (value is Boolean) changeStrictMode(value)
            "pointer_location" -> if (value is Boolean) changePointerLocation(value)
            "demo_mode" -> if (value is Boolean) changeDemoMode(value)
            "dark_mode" -> if (value is Boolean) changeDarkMode(value)
            "high_contrast" -> if (value is Boolean) changeHighContrast(value)
            "color_inversion" -> if (value is Boolean) changeColorInversion(value)
            "talkback" -> if (value is Boolean) changeTalkBack(value)
            "show_fps" -> if (value is Boolean) changeShowFps(value)
            "gpu_overdraw" -> if (value is Boolean) changeGpuOverdraw(value)
            
            // Range values
            "animations" -> if (value is String) changeAnimationScale(value)
            "font_scale" -> if (value is String) changeFontScale(value)
        }
    }
}
