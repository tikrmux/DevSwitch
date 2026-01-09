package com.tinkrmux.devswitch.actions

import com.android.ddmlib.IDevice
import com.tinkrmux.devswitch.executeShellCommand
import com.tinkrmux.devswitch.actions.ToggleAction.Companion.asFlag
import com.tinkrmux.devswitch.actions.ToggleAction.Companion.asNumber

// ============================================================================
// Display Settings Actions
// ============================================================================

/**
 * Toggle Dark Mode / Dark Theme.
 */
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

/**
 * Set Font Scale for accessibility testing.
 */
class SetFontScaleAction : NumericRangeAction(85, 200) {
    override suspend fun setValue(device: IDevice, value: Int) {
        val scale = value / 100f
        device.executeShellCommand("settings put system font_scale $scale")
    }

    override suspend fun getValue(device: IDevice): Int {
        val result = device.executeShellCommand("settings get system font_scale")
        return ((result.toFloatOrNull() ?: 1f) * 100).toInt()
    }
}

/**
 * Set Display Density (DPI).
 */
class SetDisplayDensityAction : NumericRangeAction(120, 640) {
    override suspend fun setValue(device: IDevice, value: Int) {
        device.executeShellCommand("wm density $value")
    }

    override suspend fun getValue(device: IDevice): Int {
        val result = device.executeShellCommand("wm density")
        // Parse "Physical density: 420" or "Override density: 320"
        val match = Regex("(Override|Physical) density: (\\d+)").find(result)
        return match?.groupValues?.get(2)?.toIntOrNull() ?: 420
    }
}

/**
 * Reset display density to default.
 */
suspend fun IDevice.resetDisplayDensity() {
    executeShellCommand("wm density reset")
}

/**
 * Set display resolution.
 */
suspend fun IDevice.setDisplayResolution(width: Int, height: Int) {
    executeShellCommand("wm size ${width}x${height}")
}

/**
 * Reset display resolution to default.
 */
suspend fun IDevice.resetDisplayResolution() {
    executeShellCommand("wm size reset")
}

// ============================================================================
// Accessibility Actions
// ============================================================================

/**
 * Toggle TalkBack accessibility service.
 */
class ToggleTalkBackAction : ToggleAction() {
    private val talkBackService = "com.google.android.marvin.talkback/com.google.android.marvin.talkback.TalkBackService"

    override suspend fun setValue(device: IDevice, value: Boolean) {
        if (value) {
            device.executeShellCommand("settings put secure enabled_accessibility_services $talkBackService")
            device.executeShellCommand("settings put secure accessibility_enabled 1")
        } else {
            device.executeShellCommand("settings put secure enabled_accessibility_services \"\"")
            device.executeShellCommand("settings put secure accessibility_enabled 0")
        }
    }

    override suspend fun getValue(device: IDevice): Boolean {
        val result = device.executeShellCommand("settings get secure enabled_accessibility_services")
        return result.contains("talkback", ignoreCase = true)
    }
}

/**
 * Toggle Color Inversion accessibility feature.
 */
class ToggleColorInversionAction : ToggleAction() {
    override suspend fun setValue(device: IDevice, value: Boolean) {
        device.executeShellCommand("settings put secure accessibility_display_inversion_enabled ${value.asNumber}")
    }

    override suspend fun getValue(device: IDevice): Boolean {
        return device.executeShellCommand("settings get secure accessibility_display_inversion_enabled").asFlag
    }
}

/**
 * Toggle High Contrast Text.
 */
class ToggleHighContrastTextAction : ToggleAction() {
    override suspend fun setValue(device: IDevice, value: Boolean) {
        device.executeShellCommand("settings put secure high_text_contrast_enabled ${value.asNumber}")
    }

    override suspend fun getValue(device: IDevice): Boolean {
        return device.executeShellCommand("settings get secure high_text_contrast_enabled").asFlag
    }
}

/**
 * Toggle Color Correction (for color blindness).
 */
class ToggleColorCorrectionAction : ToggleAction() {
    override suspend fun setValue(device: IDevice, value: Boolean) {
        device.executeShellCommand("settings put secure accessibility_display_daltonizer_enabled ${value.asNumber}")
        if (value) {
            // Default to deuteranomaly (most common)
            device.executeShellCommand("settings put secure accessibility_display_daltonizer 12")
        }
    }

    override suspend fun getValue(device: IDevice): Boolean {
        return device.executeShellCommand("settings get secure accessibility_display_daltonizer_enabled").asFlag
    }
}

// ============================================================================
// Color Correction Modes
// ============================================================================

enum class ColorCorrectionMode(val value: Int, val displayName: String) {
    DISABLED(0, "Disabled"),
    DEUTERANOMALY(12, "Deuteranomaly (Red-Green)"),
    PROTANOMALY(11, "Protanomaly (Red-Green)"),
    TRITANOMALY(13, "Tritanomaly (Blue-Yellow)"),
    GRAYSCALE(0, "Grayscale")
}

/**
 * Set specific color correction mode.
 */
suspend fun IDevice.setColorCorrectionMode(mode: ColorCorrectionMode) {
    if (mode == ColorCorrectionMode.DISABLED) {
        executeShellCommand("settings put secure accessibility_display_daltonizer_enabled 0")
    } else if (mode == ColorCorrectionMode.GRAYSCALE) {
        executeShellCommand("settings put secure accessibility_display_daltonizer_enabled 1")
        executeShellCommand("settings put secure accessibility_display_daltonizer 0")
    } else {
        executeShellCommand("settings put secure accessibility_display_daltonizer_enabled 1")
        executeShellCommand("settings put secure accessibility_display_daltonizer ${mode.value}")
    }
}

// ============================================================================
// Touch & Haptic Feedback
// ============================================================================

/**
 * Toggle Haptic Feedback.
 */
class ToggleHapticFeedbackAction : ToggleAction() {
    override suspend fun setValue(device: IDevice, value: Boolean) {
        device.executeShellCommand("settings put system haptic_feedback_enabled ${value.asNumber}")
    }

    override suspend fun getValue(device: IDevice): Boolean {
        return device.executeShellCommand("settings get system haptic_feedback_enabled").asFlag
    }
}

// ============================================================================
// Screen Timeout
// ============================================================================

/**
 * Set Screen Timeout in milliseconds.
 */
class SetScreenTimeoutAction : NumericRangeAction(15000, 1800000) {
    override suspend fun setValue(device: IDevice, value: Int) {
        device.executeShellCommand("settings put system screen_off_timeout $value")
    }

    override suspend fun getValue(device: IDevice): Int {
        val result = device.executeShellCommand("settings get system screen_off_timeout")
        return result.trim().toIntOrNull() ?: 60000
    }
}

/**
 * Predefined screen timeout values.
 */
val SCREEN_TIMEOUT_OPTIONS = listOf(
    15000 to "15 seconds",
    30000 to "30 seconds",
    60000 to "1 minute",
    120000 to "2 minutes",
    300000 to "5 minutes",
    600000 to "10 minutes",
    1800000 to "30 minutes"
)

// ============================================================================
// Screen Brightness
// ============================================================================

/**
 * Set Screen Brightness (0-255).
 */
class SetScreenBrightnessAction : NumericRangeAction(0, 255) {
    override suspend fun setValue(device: IDevice, value: Int) {
        device.executeShellCommand("settings put system screen_brightness $value")
    }

    override suspend fun getValue(device: IDevice): Int {
        val result = device.executeShellCommand("settings get system screen_brightness")
        return result.trim().toIntOrNull() ?: 128
    }
}

/**
 * Toggle Auto Brightness.
 */
class ToggleAutoBrightnessAction : ToggleAction() {
    override suspend fun setValue(device: IDevice, value: Boolean) {
        device.executeShellCommand("settings put system screen_brightness_mode ${value.asNumber}")
    }

    override suspend fun getValue(device: IDevice): Boolean {
        return device.executeShellCommand("settings get system screen_brightness_mode").asFlag
    }
}
