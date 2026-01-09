package com.tinkrmux.devswitch.actions

import com.android.ddmlib.IDevice
import com.tinkrmux.devswitch.executeShellCommand
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.intellij.ide.util.PropertiesComponent

// ============================================================================
// Preset Data Models
// ============================================================================

/**
 * Represents a saved settings preset.
 */
data class SettingsPreset(
    val name: String,
    val description: String = "",
    val settings: Map<String, Any>,
    val createdAt: Long = System.currentTimeMillis()
)

// ============================================================================
// Presets Manager (with Preferences Persistence)
// ============================================================================

/**
 * Manages settings presets with persistence via IntelliJ PropertiesComponent.
 */
class PresetsManager {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private val properties = PropertiesComponent.getInstance()
    private val presetsKey = "devswitch.presets"

    /**
     * Get all saved presets.
     */
    fun getPresets(): List<SettingsPreset> {
        val json = properties.getValue(presetsKey) ?: return getBuiltInPresets()
        return try {
            val type = object : TypeToken<List<SettingsPreset>>() {}.type
            gson.fromJson<List<SettingsPreset>>(json, type) ?: getBuiltInPresets()
        } catch (e: Exception) {
            getBuiltInPresets()
        }
    }

    /**
     * Save a new preset.
     */
    fun savePreset(preset: SettingsPreset) {
        val presets = getPresets().toMutableList()
        // Remove existing with same name
        presets.removeAll { it.name == preset.name }
        presets.add(0, preset)
        savePresets(presets)
    }

    /**
     * Delete a preset by name.
     */
    fun deletePreset(name: String) {
        val presets = getPresets().filter { it.name != name }
        savePresets(presets)
    }

    /**
     * Rename a preset.
     */
    fun renamePreset(oldName: String, newName: String) {
        val presets = getPresets().map {
            if (it.name == oldName) it.copy(name = newName) else it
        }
        savePresets(presets)
    }

    private fun savePresets(presets: List<SettingsPreset>) {
        val json = gson.toJson(presets)
        properties.setValue(presetsKey, json)
    }

    /**
     * Built-in presets that are always available.
     */
    private fun getBuiltInPresets(): List<SettingsPreset> = listOf(
        SettingsPreset(
            name = "UI Testing",
            description = "Optimal settings for UI/screenshot testing",
            settings = mapOf(
                "demo_mode" to true,
                "window_animation_scale" to 0,
                "transition_animation_scale" to 0,
                "animator_duration_scale" to 0,
                "show_touches" to false,
                "stay_awake" to true
            )
        ),
        SettingsPreset(
            name = "Accessibility Testing",
            description = "Settings for accessibility testing",
            settings = mapOf(
                "font_scale" to 1.3f,
                "high_contrast_text" to true,
                "talkback" to false, // User should enable manually
                "color_inversion" to false
            )
        ),
        SettingsPreset(
            name = "Performance Debug",
            description = "Debug overlays for performance analysis",
            settings = mapOf(
                "show_fps" to true,
                "gpu_overdraw" to true,
                "hwui_profile" to true,
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
            name = "Battery Saver Testing",
            description = "Simulate low battery conditions",
            settings = mapOf(
                "battery_level" to 15,
                "battery_unplugged" to true
            )
        ),
        SettingsPreset(
            name = "RTL Testing",
            description = "Test right-to-left layouts",
            settings = mapOf(
                "force_rtl" to true,
                "locale" to "ar-XB"
            )
        ),
        SettingsPreset(
            name = "Default/Reset",
            description = "Reset to default developer settings",
            settings = mapOf(
                "demo_mode" to false,
                "window_animation_scale" to 10,
                "transition_animation_scale" to 10,
                "animator_duration_scale" to 10,
                "show_touches" to false,
                "layout_bounds" to false,
                "show_fps" to false,
                "gpu_overdraw" to false,
                "hwui_profile" to false,
                "strict_mode" to false,
                "force_rtl" to false,
                "pointer_location" to false,
                "font_scale" to 1.0f
            )
        )
    )

    /**
     * Check if a preset is built-in (cannot be deleted).
     */
    fun isBuiltInPreset(name: String): Boolean {
        return getBuiltInPresets().any { it.name == name }
    }
}

// ============================================================================
// Apply Preset to Device
// ============================================================================

/**
 * Apply a settings preset to a device.
 */
suspend fun IDevice.applyPreset(preset: SettingsPreset, onProgress: (String) -> Unit = {}) {
    preset.settings.forEach { (key, value) ->
        onProgress("Applying: $key")
        applySettingValue(key, value)
    }
}

/**
 * Apply a single setting value.
 */
private suspend fun IDevice.applySettingValue(key: String, value: Any) {
    when (key) {
        // Animation scales
        "window_animation_scale" -> {
            val scale = (value as? Number)?.toFloat()?.div(10) ?: 1f
            executeShellCommand("settings put global window_animation_scale $scale")
        }
        "transition_animation_scale" -> {
            val scale = (value as? Number)?.toFloat()?.div(10) ?: 1f
            executeShellCommand("settings put global transition_animation_scale $scale")
        }
        "animator_duration_scale" -> {
            val scale = (value as? Number)?.toFloat()?.div(10) ?: 1f
            executeShellCommand("settings put global animator_duration_scale $scale")
        }

        // Boolean toggles
        "demo_mode" -> ToggleDemoModeAction().setValue(this, value as Boolean)
        "show_touches" -> ToggleShowTapsAction().setValue(this, value as Boolean)
        "stay_awake" -> ToggleStayAwakeAction().setValue(this, value as Boolean)
        "layout_bounds" -> ToggleLayoutBoundsAction().setValue(this, value as Boolean)
        "hwui_profile" -> ToggleHWUIAction().setValue(this, value as Boolean)
        "show_fps" -> ToggleShowFPSAction().setValue(this, value as Boolean)
        "gpu_overdraw" -> ToggleGPUOverdrawAction().setValue(this, value as Boolean)
        "force_rtl" -> ToggleForceRTLAction().setValue(this, value as Boolean)
        "strict_mode" -> ToggleStrictModeAction().setValue(this, value as Boolean)
        "pointer_location" -> TogglePointerLocationAction().setValue(this, value as Boolean)
        "talkback" -> ToggleTalkBackAction().setValue(this, value as Boolean)
        "color_inversion" -> ToggleColorInversionAction().setValue(this, value as Boolean)
        "high_contrast_text" -> ToggleHighContrastTextAction().setValue(this, value as Boolean)

        // Font scale
        "font_scale" -> {
            val scale = (value as? Number)?.toFloat() ?: 1f
            executeShellCommand("settings put system font_scale $scale")
        }

        // Battery simulation
        "battery_level" -> {
            val level = (value as? Number)?.toInt() ?: 100
            unplugBattery()
            setBatteryLevel(level)
        }
        "battery_unplugged" -> {
            if (value == true) unplugBattery() else resetBattery()
        }

        // Locale
        "locale" -> setLocale(value as String)
    }
}

// ============================================================================
// Capture Current Settings as Preset
// ============================================================================

/**
 * Capture current device settings as a preset.
 */
suspend fun IDevice.captureCurrentSettingsAsPreset(name: String, description: String = ""): SettingsPreset {
    val settings = mutableMapOf<String, Any>()

    // Capture toggle states
    settings["layout_bounds"] = ToggleLayoutBoundsAction().getValue(this)
    settings["show_touches"] = ToggleShowTapsAction().getValue(this)
    settings["stay_awake"] = ToggleStayAwakeAction().getValue(this)
    settings["hwui_profile"] = ToggleHWUIAction().getValue(this)
    settings["force_rtl"] = ToggleForceRTLAction().getValue(this)
    settings["strict_mode"] = ToggleStrictModeAction().getValue(this)
    settings["pointer_location"] = TogglePointerLocationAction().getValue(this)
    settings["demo_mode"] = ToggleDemoModeAction().getValue(this)
    settings["talkback"] = ToggleTalkBackAction().getValue(this)
    settings["color_inversion"] = ToggleColorInversionAction().getValue(this)
    settings["high_contrast_text"] = ToggleHighContrastTextAction().getValue(this)

    // Capture animation scales
    settings["window_animation_scale"] = SetWindowAnimationScaleAction().getValue(this)
    settings["transition_animation_scale"] = SetTransitionAnimationScaleAction().getValue(this)
    settings["animator_duration_scale"] = SetAnimatorDurationScaleAction().getValue(this)

    // Capture font scale
    settings["font_scale"] = SetFontScaleAction().getValue(this) / 100f

    // Capture locale
    settings["locale"] = getCurrentLocale()

    return SettingsPreset(
        name = name,
        description = description,
        settings = settings
    )
}
