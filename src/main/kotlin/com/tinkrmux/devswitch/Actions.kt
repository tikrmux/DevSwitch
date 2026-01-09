package com.tinkrmux.devswitch

import com.android.ddmlib.IDevice
import com.tinkrmux.devswitch.ToggleAction.Companion.asFlag
import kotlin.math.round

interface SettingsAction<T> {
    suspend fun setValue(device: IDevice, value: T)
    suspend fun getValue(device: IDevice): T
}

abstract class ToggleAction : SettingsAction<Boolean> {
    companion object {
        val Boolean.asNumber: String get() = if (this) "1" else "0"
        val String.asFlag: Boolean get() = trim() == "1"
        suspend fun IDevice.restartSystemUI() = executeShellCommand("service call activity 1599295570")
    }
}

class ToggleWiFiAction : ToggleAction() {

    companion object {
        private val Boolean.enableOrDisable: String get() = if (this) "enable" else "disable"
    }

    override suspend fun setValue(device: IDevice, value: Boolean) {
        device.executeShellCommand("svc wifi ${value.enableOrDisable}")
    }

    override suspend fun getValue(device: IDevice): Boolean {
        return device.executeShellCommand("settings get global wifi_on").asFlag
    }
}

class ToggleDataAction : ToggleAction() {

    companion object {
        private val Boolean.enableOrDisable: String get() = if (this) "enable" else "disable"
    }

    override suspend fun setValue(device: IDevice, value: Boolean) {
        device.executeShellCommand("svc data ${value.enableOrDisable}")
    }

    override suspend fun getValue(device: IDevice): Boolean {
        return device.executeShellCommand("settings get global mobile_data").asFlag
    }
}

class ToggleLayoutBoundsAction : ToggleAction() {

    override suspend fun setValue(device: IDevice, value: Boolean) {
        device.executeShellCommand("setprop debug.layout $value")
        device.restartSystemUI()
    }

    override suspend fun getValue(device: IDevice): Boolean {
        return device.executeShellCommand("getprop debug.layout").trim() == "true"
    }
}

class ToggleHwuiRenderingAction : ToggleAction() {

    companion object {
        private const val VISUAL_BARS = "visual_bars"
        private val String.flag: Boolean get() = trim() == VISUAL_BARS
        private val Boolean.string: String get() = if (this) VISUAL_BARS else "off"
    }

    override suspend fun setValue(device: IDevice, value: Boolean) {
        device.executeShellCommand("setprop debug.hwui.profile ${value.string}")
        device.restartSystemUI()
    }

    override suspend fun getValue(device: IDevice): Boolean {
        return device.executeShellCommand("getprop debug.hwui.profile").flag
    }
}

class ToggleShowTapsAction : ToggleAction() {

    override suspend fun setValue(device: IDevice, value: Boolean) {
        device.executeShellCommand("settings put system show_touches ${value.asNumber}")
        device.executeShellCommand("am broadcast -a android.intent.action.USER_PRESENT") // Update UI
        device.restartSystemUI()
    }

    override suspend fun getValue(device: IDevice): Boolean {
        return device.executeShellCommand("settings get system show_touches").asFlag
    }
}

class ToggleStayAwakeAction : ToggleAction() {

    companion object {
        private const val ON_VALUE = "3"
        private const val OFF_VALUE = "0"
    }

    override suspend fun setValue(device: IDevice, value: Boolean) {
        val flag = if (value) ON_VALUE else OFF_VALUE
        device.executeShellCommand("settings put global stay_on_while_plugged_in $flag")
    }

    override suspend fun getValue(device: IDevice): Boolean {
        return device.executeShellCommand("settings get global stay_on_while_plugged_in").trim() == ON_VALUE
    }
}

class ToggleShowFPSAction : ToggleAction() {

    override suspend fun setValue(device: IDevice, value: Boolean) {
        device.executeShellCommand("setprop debug.hwui.show_fps $value")
    }

    override suspend fun getValue(device: IDevice): Boolean {
        return device.executeShellCommand("getprop debug.hwui.show_fps").trim().toBoolean()
    }
}

abstract class RangeAction : SettingsAction<String> {
    abstract suspend fun getRange(device: IDevice): List<String>
}

class AnimationScaleAction : RangeAction() {

    companion object {
        // Android animation scale values: Off, 0.5x, 1x, 1.5x, 2x, 5x, 10x
        private val SCALE_OPTIONS = listOf("Off", "0.5x", "1x", "1.5x", "2x", "5x", "10x")
        private val SCALE_VALUES = mapOf(
            "off" to 0f,
            "0.5x" to 0.5f,
            "1x" to 1f,
            "1.5x" to 1.5f,
            "2x" to 2f,
            "5x" to 5f,
            "10x" to 10f
        )
    }

    override suspend fun getRange(device: IDevice): List<String> = SCALE_OPTIONS

    override suspend fun setValue(device: IDevice, value: String) {
        // Handle case-insensitive lookup and various formats
        val normalizedValue = value.lowercase().trim()
        val scale = SCALE_VALUES[normalizedValue] 
            ?: value.replace("x", "").replace("X", "").toFloatOrNull()
            ?: 1f
        device.executeShellCommand("settings put global window_animation_scale $scale")
        device.executeShellCommand("settings put global transition_animation_scale $scale")
        device.executeShellCommand("settings put global animator_duration_scale $scale")
    }

    override suspend fun getValue(device: IDevice): String {
        val rawValue = device.executeShellCommand("settings get global window_animation_scale")
            .trim()
            .replace("\n", "")
        
        val floatValue = rawValue.toFloatOrNull() ?: 1f
        
        // Map back to display string
        return when {
            floatValue == 0f -> "Off"
            floatValue == 0.5f -> "0.5x"
            floatValue == 1f -> "1x"
            floatValue == 1.5f -> "1.5x"
            floatValue == 2f -> "2x"
            floatValue == 5f -> "5x"
            floatValue == 10f -> "10x"
            else -> "${floatValue}x"
        }
    }
}

class FpsScaleAction : RangeAction() {

    private companion object {
        const val DUMPSYS_DISPLAY = "dumpsys display"
        val displayModeRegex = """DisplayMode\{.*? vsyncRate=([\d.]+)""".toRegex()
    }

    private var cachedRanges: List<String>? = null
    private var lastDevice: IDevice? = null

    override suspend fun getRange(device: IDevice): List<String> {
        if (device == lastDevice && cachedRanges != null) {
            return cachedRanges!!
        }
        
        val dumpsysOutput = device.executeShellCommand(DUMPSYS_DISPLAY)
        cachedRanges = displayModeRegex.findAll(dumpsysOutput)
            .mapNotNull { match -> match.groupValues.getOrNull(1)?.toDoubleOrNull() }
            .map { round(it).toString() }
            .toSet()
            .toList()
        lastDevice = device
        return cachedRanges!!
    }

    override suspend fun setValue(device: IDevice, value: String) {
        device.executeShellCommand("settings put system min_refresh_rate $value")
        device.executeShellCommand("settings put system peak_refresh_rate $value")
    }

    override suspend fun getValue(device: IDevice): String {
        val peakRate = device.executeShellCommand("settings get system peak_refresh_rate").trim()
        if (peakRate == "null" || peakRate.isEmpty()) {
            return "Default"
        }
        return peakRate.toDoubleOrNull()?.run { round(this).toString() } ?: "Default"
    }
}

// ============================================================================
// Additional Network Toggle Actions
// ============================================================================

class ToggleAirplaneModeAction : ToggleAction() {
    override suspend fun setValue(device: IDevice, value: Boolean) {
        device.executeShellCommand("settings put global airplane_mode_on ${value.asNumber}")
        device.executeShellCommand("am broadcast -a android.intent.action.AIRPLANE_MODE --ez state $value")
    }

    override suspend fun getValue(device: IDevice): Boolean {
        return device.executeShellCommand("settings get global airplane_mode_on").asFlag
    }
}

class ToggleBluetoothAction : ToggleAction() {
    override suspend fun setValue(device: IDevice, value: Boolean) {
        val command = if (value) "enable" else "disable"
        device.executeShellCommand("svc bluetooth $command")
    }

    override suspend fun getValue(device: IDevice): Boolean {
        return device.executeShellCommand("settings get global bluetooth_on").asFlag
    }
}

// ============================================================================
// Debug Overlay Toggle Actions
// ============================================================================

class TogglePointerLocationAction : ToggleAction() {
    override suspend fun setValue(device: IDevice, value: Boolean) {
        device.executeShellCommand("settings put system pointer_location ${value.asNumber}")
    }

    override suspend fun getValue(device: IDevice): Boolean {
        return device.executeShellCommand("settings get system pointer_location").asFlag
    }
}

class ToggleGPUOverdrawAction : ToggleAction() {
    override suspend fun setValue(device: IDevice, value: Boolean) {
        val mode = if (value) "show" else "false"
        // Primary method using setprop
        device.executeShellCommand("setprop debug.hwui.overdraw $mode")
        // Also set the settings secure option for newer Android versions
        device.executeShellCommand("settings put secure debug_hw_overdraw ${if (value) "show" else "off"}")
        // Restart SystemUI to apply changes
        device.restartSystemUI()
    }

    override suspend fun getValue(device: IDevice): Boolean {
        val prop = device.executeShellCommand("getprop debug.hwui.overdraw").trim()
        return prop == "show"
    }
}

class ToggleSurfaceUpdatesAction : ToggleAction() {
    override suspend fun setValue(device: IDevice, value: Boolean) {
        // Use setprop for debug.hwui.show_dirty_regions which works on more devices
        device.executeShellCommand("setprop debug.hwui.show_dirty_regions ${value.asNumber}")
        // Also try SurfaceFlinger for older devices
        device.executeShellCommand("service call SurfaceFlinger 1002 i32 ${if (value) 1 else 0}")
        // Restart UI to apply
        device.restartSystemUI()
    }

    override suspend fun getValue(device: IDevice): Boolean {
        val prop = device.executeShellCommand("getprop debug.hwui.show_dirty_regions").trim()
        return prop == "1" || prop == "true"
    }
}

class ToggleStrictModeAction : ToggleAction() {
    override suspend fun setValue(device: IDevice, value: Boolean) {
        device.executeShellCommand("settings put global strict_mode_visual_indicator ${value.asNumber}")
    }

    override suspend fun getValue(device: IDevice): Boolean {
        return device.executeShellCommand("settings get global strict_mode_visual_indicator").asFlag
    }
}

// ============================================================================
// Developer Options Toggle Actions
// ============================================================================

class ToggleDontKeepActivitiesAction : ToggleAction() {
    override suspend fun setValue(device: IDevice, value: Boolean) {
        device.executeShellCommand("settings put global always_finish_activities ${value.asNumber}")
    }

    override suspend fun getValue(device: IDevice): Boolean {
        return device.executeShellCommand("settings get global always_finish_activities").asFlag
    }
}

class ToggleForceRTLAction : ToggleAction() {
    override suspend fun setValue(device: IDevice, value: Boolean) {
        device.executeShellCommand("settings put global debug.force_rtl ${value.asNumber}")
        device.restartSystemUI()
    }

    override suspend fun getValue(device: IDevice): Boolean {
        return device.executeShellCommand("settings get global debug.force_rtl").asFlag
    }
}

class ToggleUSBDebuggingAction : ToggleAction() {
    override suspend fun setValue(device: IDevice, value: Boolean) {
        device.executeShellCommand("settings put global adb_enabled ${value.asNumber}")
    }

    override suspend fun getValue(device: IDevice): Boolean {
        return device.executeShellCommand("settings get global adb_enabled").asFlag
    }
}

class ToggleDemoModeAction : ToggleAction() {
    override suspend fun setValue(device: IDevice, value: Boolean) {
        if (value) {
            device.executeShellCommand("settings put global sysui_demo_allowed 1")
            device.executeShellCommand("am broadcast -a com.android.systemui.demo -e command enter")
            device.executeShellCommand("am broadcast -a com.android.systemui.demo -e command clock -e hhmm 1200")
            device.executeShellCommand("am broadcast -a com.android.systemui.demo -e command battery -e level 100 -e plugged false")
            device.executeShellCommand("am broadcast -a com.android.systemui.demo -e command network -e wifi show -e level 4")
            device.executeShellCommand("am broadcast -a com.android.systemui.demo -e command notifications -e visible false")
        } else {
            device.executeShellCommand("am broadcast -a com.android.systemui.demo -e command exit")
            device.executeShellCommand("settings put global sysui_demo_allowed 0")
        }
    }

    override suspend fun getValue(device: IDevice): Boolean {
        return device.executeShellCommand("settings get global sysui_demo_allowed").asFlag
    }
}

// ============================================================================
// Display & Accessibility Toggle Actions
// ============================================================================

class ToggleDarkModeAction : ToggleAction() {
    override suspend fun setValue(device: IDevice, value: Boolean) {
        val mode = if (value) "yes" else "no"
        device.executeShellCommand("cmd uimode night $mode")
    }

    override suspend fun getValue(device: IDevice): Boolean {
        val result = device.executeShellCommand("cmd uimode night")
        return result.contains("yes", ignoreCase = true)
    }
}

class ToggleHighContrastTextAction : ToggleAction() {
    override suspend fun setValue(device: IDevice, value: Boolean) {
        device.executeShellCommand("settings put secure high_text_contrast_enabled ${value.asNumber}")
    }

    override suspend fun getValue(device: IDevice): Boolean {
        return device.executeShellCommand("settings get secure high_text_contrast_enabled").asFlag
    }
}

class ToggleColorInversionAction : ToggleAction() {
    override suspend fun setValue(device: IDevice, value: Boolean) {
        device.executeShellCommand("settings put secure accessibility_display_inversion_enabled ${value.asNumber}")
    }

    override suspend fun getValue(device: IDevice): Boolean {
        return device.executeShellCommand("settings get secure accessibility_display_inversion_enabled").asFlag
    }
}

class ToggleMagnificationAction : ToggleAction() {
    override suspend fun setValue(device: IDevice, value: Boolean) {
        device.executeShellCommand("settings put secure accessibility_display_magnification_enabled ${value.asNumber}")
    }

    override suspend fun getValue(device: IDevice): Boolean {
        return device.executeShellCommand("settings get secure accessibility_display_magnification_enabled").asFlag
    }
}

class ToggleBoldTextAction : ToggleAction() {
    override suspend fun setValue(device: IDevice, value: Boolean) {
        device.executeShellCommand("settings put secure font_weight_adjustment ${if (value) 300 else 0}")
    }

    override suspend fun getValue(device: IDevice): Boolean {
        val result = device.executeShellCommand("settings get secure font_weight_adjustment")
        return result.trim().toIntOrNull()?.let { it > 0 } ?: false
    }
}

class ToggleAutoBrightnessAction : ToggleAction() {
    override suspend fun setValue(device: IDevice, value: Boolean) {
        device.executeShellCommand("settings put system screen_brightness_mode ${value.asNumber}")
    }

    override suspend fun getValue(device: IDevice): Boolean {
        return device.executeShellCommand("settings get system screen_brightness_mode").asFlag
    }
}

class ToggleTalkBackAction : ToggleAction() {
    // Try multiple possible TalkBack service names
    private val talkBackServices = listOf(
        "com.google.android.marvin.talkback/com.google.android.marvin.talkback.TalkBackService",
        "com.google.android.marvin.talkback/.TalkBackService",
        "com.samsung.android.accessibility.talkback/com.samsung.android.marvin.talkback.TalkBackService"
    )

    override suspend fun setValue(device: IDevice, value: Boolean) {
        if (value) {
            // Get current services and add TalkBack
            val current = device.executeShellCommand("settings get secure enabled_accessibility_services").trim()
            val talkBackService = talkBackServices[0] // Use Google's service
            val newServices = if (current.isEmpty() || current == "null") {
                talkBackService
            } else if (!current.contains("talkback", ignoreCase = true)) {
                "$current:$talkBackService"
            } else {
                current
            }
            device.executeShellCommand("settings put secure enabled_accessibility_services '$newServices'")
            device.executeShellCommand("settings put secure accessibility_enabled 1")
        } else {
            // Remove TalkBack from services
            val current = device.executeShellCommand("settings get secure enabled_accessibility_services").trim()
            val filtered = current.split(":")
                .filter { !it.contains("talkback", ignoreCase = true) }
                .joinToString(":")
            device.executeShellCommand("settings put secure enabled_accessibility_services '$filtered'")
            if (filtered.isEmpty()) {
                device.executeShellCommand("settings put secure accessibility_enabled 0")
            }
        }
    }

    override suspend fun getValue(device: IDevice): Boolean {
        val result = device.executeShellCommand("settings get secure enabled_accessibility_services")
        return result.contains("talkback", ignoreCase = true)
    }
}

// ============================================================================
// Battery Toggle Actions
// ============================================================================

class ToggleBatterySaverAction : ToggleAction() {
    override suspend fun setValue(device: IDevice, value: Boolean) {
        device.executeShellCommand("settings put global low_power ${value.asNumber}")
    }

    override suspend fun getValue(device: IDevice): Boolean {
        return device.executeShellCommand("settings get global low_power").asFlag
    }
}

// ============================================================================
// Additional Range Actions
// ============================================================================

class FontScaleAction : RangeAction() {
    override suspend fun getRange(device: IDevice): List<String> =
        listOf("0.85x", "1.0x", "1.15x", "1.3x", "1.5x", "1.8x", "2.0x")

    override suspend fun setValue(device: IDevice, value: String) {
        val scale = value.removeSuffix("x").toFloatOrNull() ?: 1f
        device.executeShellCommand("settings put system font_scale $scale")
    }

    override suspend fun getValue(device: IDevice): String {
        val result = device.executeShellCommand("settings get system font_scale")
        val scale = result.trim().toFloatOrNull() ?: 1f
        return "${scale}x"
    }
}

class DisplayDensityAction : RangeAction() {
    override suspend fun getRange(device: IDevice): List<String> =
        listOf("Default", "120", "160", "240", "320", "400", "480", "560", "640")

    override suspend fun setValue(device: IDevice, value: String) {
        if (value == "Default") {
            device.executeShellCommand("wm density reset")
        } else {
            device.executeShellCommand("wm density $value")
        }
    }

    override suspend fun getValue(device: IDevice): String {
        val result = device.executeShellCommand("wm density")
        val match = Regex("Override density: (\\d+)").find(result)
        return match?.groupValues?.get(1) ?: "Default"
    }
}

class ColorCorrectionAction : RangeAction() {
    override suspend fun getRange(device: IDevice): List<String> =
        listOf("Disabled", "Deuteranomaly", "Protanomaly", "Tritanomaly", "Grayscale")

    override suspend fun setValue(device: IDevice, value: String) {
        // Daltonizer modes: 0=none, 11=protanomaly, 12=deuteranomaly, 13=tritanomaly, -1=grayscale
        val mode = when (value) {
            "Disabled" -> 0
            "Deuteranomaly" -> 12
            "Protanomaly" -> 11
            "Tritanomaly" -> 13
            "Grayscale" -> -1
            else -> 0
        }
        
        if (value == "Disabled") {
            // Disable color correction
            device.executeShellCommand("settings put secure accessibility_display_daltonizer_enabled 0")
            device.executeShellCommand("settings put secure accessibility_display_daltonizer 0")
        } else {
            // First set the mode, then enable
            device.executeShellCommand("settings put secure accessibility_display_daltonizer $mode")
            device.executeShellCommand("settings put secure accessibility_display_daltonizer_enabled 1")
        }
        
        // Force display refresh by toggling a display setting
        device.executeShellCommand("service call SurfaceFlinger 1015")
    }

    override suspend fun getValue(device: IDevice): String {
        val enabled = device.executeShellCommand("settings get secure accessibility_display_daltonizer_enabled").asFlag
        if (!enabled) return "Disabled"
        val mode = device.executeShellCommand("settings get secure accessibility_display_daltonizer").trim()
        return when (mode) {
            "12" -> "Deuteranomaly"
            "11" -> "Protanomaly"
            "13" -> "Tritanomaly"
            "-1" -> "Grayscale"
            else -> "Disabled"
        }
    }
}

class BackgroundProcessLimitAction : RangeAction() {
    override suspend fun getRange(device: IDevice): List<String> =
        listOf("Standard limit", "No background", "1 process", "2 processes", "3 processes", "4 processes")

    override suspend fun setValue(device: IDevice, value: String) {
        when (value) {
            "Standard limit" -> {
                device.executeShellCommand("settings put global always_finish_activities 0")
                device.executeShellCommand("settings delete global background_process_limit")
            }
            "No background" -> {
                device.executeShellCommand("settings put global always_finish_activities 1")
                device.executeShellCommand("settings put global background_process_limit 0")
            }
            "1 process" -> {
                device.executeShellCommand("settings put global always_finish_activities 0")
                device.executeShellCommand("settings put global background_process_limit 1")
            }
            "2 processes" -> {
                device.executeShellCommand("settings put global always_finish_activities 0")
                device.executeShellCommand("settings put global background_process_limit 2")
            }
            "3 processes" -> {
                device.executeShellCommand("settings put global always_finish_activities 0")
                device.executeShellCommand("settings put global background_process_limit 3")
            }
            "4 processes" -> {
                device.executeShellCommand("settings put global always_finish_activities 0")
                device.executeShellCommand("settings put global background_process_limit 4")
            }
        }
    }

    override suspend fun getValue(device: IDevice): String {
        val alwaysFinish = device.executeShellCommand("settings get global always_finish_activities").trim()
        if (alwaysFinish == "1") return "No background"
        
        val limitStr = device.executeShellCommand("settings get global background_process_limit").trim()
        
        // "null" means the setting is not set (standard limit)
        if (limitStr == "null" || limitStr.isEmpty()) return "Standard limit"
        
        val limit = limitStr.toIntOrNull()
        return when (limit) {
            null -> "Standard limit"
            0 -> "No background"
            1 -> "1 process"
            2 -> "2 processes"
            3 -> "3 processes"
            4 -> "4 processes"
            else -> "Standard limit"
        }
    }
}

class LocaleAction : RangeAction() {
    private val locales = listOf(
        "English (US)" to "en-US",
        "English (UK)" to "en-GB",
        "Spanish" to "es-ES",
        "French" to "fr-FR",
        "German" to "de-DE",
        "Italian" to "it-IT",
        "Japanese" to "ja-JP",
        "Korean" to "ko-KR",
        "Chinese (Simplified)" to "zh-CN",
        "Chinese (Traditional)" to "zh-TW",
        "Arabic" to "ar-SA",
        "Hebrew" to "he-IL",
        "Russian" to "ru-RU",
        "Portuguese (Brazil)" to "pt-BR",
        "Hindi" to "hi-IN",
        "Pseudo-Accented" to "en-XA",
        "Pseudo-Bidi" to "ar-XB"
    )

    override suspend fun getRange(device: IDevice): List<String> =
        locales.map { "${it.first} (${it.second})" }

    override suspend fun setValue(device: IDevice, value: String) {
        val code = Regex("\\(([a-z]{2}-[A-Z]{2,3})\\)").find(value)?.groupValues?.get(1) ?: return
        val parts = code.split("-")
        if (parts.size == 2) {
            val locale = "${parts[0]}_${parts[1]}"
            // Method 1: Use settings (Android 7+)
            device.executeShellCommand("settings put system system_locales $locale")
            // Method 2: Use setprop for persist
            device.executeShellCommand("setprop persist.sys.locale $locale")
            device.executeShellCommand("setprop persist.sys.language ${parts[0]}")
            device.executeShellCommand("setprop persist.sys.country ${parts[1]}")
            // Method 3: Use am command to trigger locale change (most reliable)
            device.executeShellCommand("am start -a android.settings.LOCALE_SETTINGS")
            // Broadcast locale change
            device.executeShellCommand("am broadcast -a android.intent.action.LOCALE_CHANGED")
        }
    }

    override suspend fun getValue(device: IDevice): String {
        // Try system locales first
        val sysLocales = device.executeShellCommand("settings get system system_locales").trim()
        if (sysLocales.isNotEmpty() && sysLocales != "null") {
            val code = sysLocales.replace("_", "-")
            return locales.find { it.second.equals(code, ignoreCase = true) }?.let { "${it.first} ($code)" } ?: code
        }
        // Fallback to persist.sys.locale
        val locale = device.executeShellCommand("getprop persist.sys.locale").trim()
        if (locale.isNotEmpty() && locale != "null") {
            val code = locale.replace("_", "-")
            return locales.find { it.second.equals(code, ignoreCase = true) }?.let { "${it.first} ($code)" } ?: code
        }
        return "English (US) (en-US)"
    }
}

class ScreenTimeoutAction : RangeAction() {
    private val timeouts = listOf(
        "15 sec" to 15000,
        "30 sec" to 30000,
        "1 min" to 60000,
        "2 min" to 120000,
        "5 min" to 300000,
        "10 min" to 600000,
        "30 min" to 1800000
    )

    override suspend fun getRange(device: IDevice): List<String> = timeouts.map { it.first }

    override suspend fun setValue(device: IDevice, value: String) {
        val ms = timeouts.find { it.first == value }?.second ?: 60000
        device.executeShellCommand("settings put system screen_off_timeout $ms")
    }

    override suspend fun getValue(device: IDevice): String {
        val result = device.executeShellCommand("settings get system screen_off_timeout")
        val ms = result.trim().toIntOrNull() ?: 60000
        return timeouts.find { it.second == ms }?.first ?: "1 min"
    }
}

class ScreenBrightnessAction : RangeAction() {
    override suspend fun getRange(device: IDevice): List<String> =
        listOf("10%", "25%", "50%", "75%", "100%")

    override suspend fun setValue(device: IDevice, value: String) {
        val percent = value.removeSuffix("%").toIntOrNull() ?: 50
        // Use proper rounding: (percent * 255 + 50) / 100 for correct values
        // 10% -> 26, 25% -> 64, 50% -> 128, 75% -> 191, 100% -> 255
        val brightness = (percent * 255 + 50) / 100
        // First disable auto-brightness
        device.executeShellCommand("settings put system screen_brightness_mode 0")
        // Then set the brightness value
        device.executeShellCommand("settings put system screen_brightness $brightness")
    }

    override suspend fun getValue(device: IDevice): String {
        val result = device.executeShellCommand("settings get system screen_brightness")
        val brightness = result.trim().toIntOrNull() ?: 128
        // Use proper rounding for percentage calculation
        val percent = (brightness * 100 + 127) / 255
        // Find the closest predefined value
        val options = listOf(10, 25, 50, 75, 100)
        val closest = options.minByOrNull { kotlin.math.abs(it - percent) } ?: percent
        return "${closest}%"
    }
}
