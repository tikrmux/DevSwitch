@file:Suppress("UNCHECKED_CAST")

package com.tinkrmux.devswitch

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.launch

// ============================================================================
// Setting Keys
// ============================================================================

// Network
private const val KEY_WIFI = "wifi"
private const val KEY_DATA = "data"
private const val KEY_AIRPLANE = "airplane_mode"
private const val KEY_BLUETOOTH = "bluetooth"

// Debug Overlays
private const val KEY_LAYOUT_BOUNDS = "layout_bounds"
private const val KEY_HWUI = "hwui"
private const val KEY_SHOW_TAPS = "taps"
private const val KEY_SHOW_FPS = "show_fps"
private const val KEY_POINTER_LOCATION = "pointer_location"
private const val KEY_GPU_OVERDRAW = "gpu_overdraw"
private const val KEY_SURFACE_UPDATES = "surface_updates"
private const val KEY_STRICT_MODE = "strict_mode"

// Developer Options
private const val KEY_STAY_AWAKE = "stay_awake"
private const val KEY_DONT_KEEP_ACTIVITIES = "dont_keep_activities"
private const val KEY_FORCE_RTL = "force_rtl"
private const val KEY_USB_DEBUGGING = "usb_debugging"
private const val KEY_DEMO_MODE = "demo_mode"

// Display & Accessibility
private const val KEY_DARK_MODE = "dark_mode"
private const val KEY_HIGH_CONTRAST = "high_contrast"
private const val KEY_COLOR_INVERSION = "color_inversion"
private const val KEY_MAGNIFICATION = "magnification"
private const val KEY_BOLD_TEXT = "bold_text"
private const val KEY_AUTO_BRIGHTNESS = "auto_brightness"
private const val KEY_TALKBACK = "talkback"

// Range Settings
private const val KEY_ANIMATION_SCALE = "animation_scale"
private const val KEY_FPS_SCALE = "fps_scale"
private const val KEY_FONT_SCALE = "font_scale"
private const val KEY_DISPLAY_DENSITY = "display_density"
private const val KEY_COLOR_CORRECTION = "color_correction"
private const val KEY_BACKGROUND_LIMIT = "background_limit"
private const val KEY_LOCALE = "locale"
private const val KEY_SCREEN_TIMEOUT = "screen_timeout"
private const val KEY_BRIGHTNESS = "brightness"

// Battery
private const val KEY_BATTERY_SAVER = "battery_saver"

fun Controller.initExtensions() {
    // Toggle Actions
    mapOf(
        // Network
        KEY_WIFI to ToggleWiFiAction(),
        KEY_DATA to ToggleDataAction(),
        KEY_AIRPLANE to ToggleAirplaneModeAction(),
        KEY_BLUETOOTH to ToggleBluetoothAction(),
        
        // Debug Overlays
        KEY_LAYOUT_BOUNDS to ToggleLayoutBoundsAction(),
        KEY_HWUI to ToggleHwuiRenderingAction(),
        KEY_SHOW_TAPS to ToggleShowTapsAction(),
        KEY_SHOW_FPS to ToggleShowFPSAction(),
        KEY_POINTER_LOCATION to TogglePointerLocationAction(),
        KEY_GPU_OVERDRAW to ToggleGPUOverdrawAction(),
        KEY_SURFACE_UPDATES to ToggleSurfaceUpdatesAction(),
        KEY_STRICT_MODE to ToggleStrictModeAction(),
        
        // Developer Options
        KEY_STAY_AWAKE to ToggleStayAwakeAction(),
        KEY_DONT_KEEP_ACTIVITIES to ToggleDontKeepActivitiesAction(),
        KEY_FORCE_RTL to ToggleForceRTLAction(),
        KEY_USB_DEBUGGING to ToggleUSBDebuggingAction(),
        KEY_DEMO_MODE to ToggleDemoModeAction(),
        
        // Display & Accessibility
        KEY_DARK_MODE to ToggleDarkModeAction(),
        KEY_HIGH_CONTRAST to ToggleHighContrastTextAction(),
        KEY_COLOR_INVERSION to ToggleColorInversionAction(),
        KEY_MAGNIFICATION to ToggleMagnificationAction(),
        KEY_BOLD_TEXT to ToggleBoldTextAction(),
        KEY_AUTO_BRIGHTNESS to ToggleAutoBrightnessAction(),
        KEY_TALKBACK to ToggleTalkBackAction(),
        
        // Battery
        KEY_BATTERY_SAVER to ToggleBatterySaverAction(),
        
        // Range Actions
        KEY_ANIMATION_SCALE to AnimationScaleAction(),
        KEY_FPS_SCALE to FpsScaleAction(),
        KEY_FONT_SCALE to FontScaleAction(),
        KEY_DISPLAY_DENSITY to DisplayDensityAction(),
        KEY_COLOR_CORRECTION to ColorCorrectionAction(),
        KEY_BACKGROUND_LIMIT to BackgroundProcessLimitAction(),
        KEY_LOCALE to LocaleAction(),
        KEY_SCREEN_TIMEOUT to ScreenTimeoutAction(),
        KEY_BRIGHTNESS to ScreenBrightnessAction(),
    ).also { actions.putAll(it) }

    // Toggle States
    mapOf(
        // Network
        KEY_WIFI to mutableStateOf(false),
        KEY_DATA to mutableStateOf(false),
        KEY_AIRPLANE to mutableStateOf(false),
        KEY_BLUETOOTH to mutableStateOf(false),
        
        // Debug Overlays
        KEY_LAYOUT_BOUNDS to mutableStateOf(false),
        KEY_HWUI to mutableStateOf(false),
        KEY_SHOW_TAPS to mutableStateOf(false),
        KEY_SHOW_FPS to mutableStateOf(false),
        KEY_POINTER_LOCATION to mutableStateOf(false),
        KEY_GPU_OVERDRAW to mutableStateOf(false),
        KEY_SURFACE_UPDATES to mutableStateOf(false),
        KEY_STRICT_MODE to mutableStateOf(false),
        
        // Developer Options
        KEY_STAY_AWAKE to mutableStateOf(false),
        KEY_DONT_KEEP_ACTIVITIES to mutableStateOf(false),
        KEY_FORCE_RTL to mutableStateOf(false),
        KEY_USB_DEBUGGING to mutableStateOf(false),
        KEY_DEMO_MODE to mutableStateOf(false),
        
        // Display & Accessibility
        KEY_DARK_MODE to mutableStateOf(false),
        KEY_HIGH_CONTRAST to mutableStateOf(false),
        KEY_COLOR_INVERSION to mutableStateOf(false),
        KEY_MAGNIFICATION to mutableStateOf(false),
        KEY_BOLD_TEXT to mutableStateOf(false),
        KEY_AUTO_BRIGHTNESS to mutableStateOf(false),
        KEY_TALKBACK to mutableStateOf(false),
        
        // Battery
        KEY_BATTERY_SAVER to mutableStateOf(false),
        
        // Range States
        KEY_ANIMATION_SCALE to mutableStateOf(""),
        KEY_FPS_SCALE to mutableStateOf(""),
        KEY_FONT_SCALE to mutableStateOf(""),
        KEY_DISPLAY_DENSITY to mutableStateOf(""),
        KEY_COLOR_CORRECTION to mutableStateOf(""),
        KEY_BACKGROUND_LIMIT to mutableStateOf(""),
        KEY_LOCALE to mutableStateOf(""),
        KEY_SCREEN_TIMEOUT to mutableStateOf(""),
        KEY_BRIGHTNESS to mutableStateOf(""),
    ).also { settingsState.putAll(it) }
}

val Controller.wifiState
    get() = settingsState.getValue(KEY_WIFI) as MutableState<Boolean>

val Controller.dataState
    get() = settingsState.getValue(KEY_DATA) as MutableState<Boolean>

val Controller.airplaneModeState
    get() = settingsState.getValue(KEY_AIRPLANE) as MutableState<Boolean>

val Controller.bluetoothState
    get() = settingsState.getValue(KEY_BLUETOOTH) as MutableState<Boolean>

val Controller.layoutBoundsState
    get() = settingsState.getValue(KEY_LAYOUT_BOUNDS) as MutableState<Boolean>

val Controller.hwuiState
    get() = settingsState.getValue(KEY_HWUI) as MutableState<Boolean>

val Controller.showTapsState
    get() = settingsState.getValue(KEY_SHOW_TAPS) as MutableState<Boolean>

val Controller.stayAwakeState
    get() = settingsState.getValue(KEY_STAY_AWAKE) as MutableState<Boolean>

val Controller.showFpsState
    get() = settingsState.getValue(KEY_SHOW_FPS) as MutableState<Boolean>

val Controller.pointerLocationState
    get() = settingsState.getValue(KEY_POINTER_LOCATION) as MutableState<Boolean>

val Controller.gpuOverdrawState
    get() = settingsState.getValue(KEY_GPU_OVERDRAW) as MutableState<Boolean>

val Controller.surfaceUpdatesState
    get() = settingsState.getValue(KEY_SURFACE_UPDATES) as MutableState<Boolean>

val Controller.strictModeState
    get() = settingsState.getValue(KEY_STRICT_MODE) as MutableState<Boolean>

val Controller.dontKeepActivitiesState
    get() = settingsState.getValue(KEY_DONT_KEEP_ACTIVITIES) as MutableState<Boolean>

val Controller.forceRtlState
    get() = settingsState.getValue(KEY_FORCE_RTL) as MutableState<Boolean>

val Controller.usbDebuggingState
    get() = settingsState.getValue(KEY_USB_DEBUGGING) as MutableState<Boolean>

val Controller.demoModeState
    get() = settingsState.getValue(KEY_DEMO_MODE) as MutableState<Boolean>

val Controller.darkModeState
    get() = settingsState.getValue(KEY_DARK_MODE) as MutableState<Boolean>

val Controller.highContrastState
    get() = settingsState.getValue(KEY_HIGH_CONTRAST) as MutableState<Boolean>

val Controller.colorInversionState
    get() = settingsState.getValue(KEY_COLOR_INVERSION) as MutableState<Boolean>

val Controller.magnificationState
    get() = settingsState.getValue(KEY_MAGNIFICATION) as MutableState<Boolean>

val Controller.boldTextState
    get() = settingsState.getValue(KEY_BOLD_TEXT) as MutableState<Boolean>

val Controller.autoBrightnessState
    get() = settingsState.getValue(KEY_AUTO_BRIGHTNESS) as MutableState<Boolean>

val Controller.talkBackState
    get() = settingsState.getValue(KEY_TALKBACK) as MutableState<Boolean>

val Controller.batterySaverState
    get() = settingsState.getValue(KEY_BATTERY_SAVER) as MutableState<Boolean>

val Controller.animationScaleState
    get() = settingsState.getValue(KEY_ANIMATION_SCALE) as MutableState<String>

val Controller.fpsScaleState
    get() = settingsState.getValue(KEY_FPS_SCALE) as MutableState<String>

val Controller.fontScaleState
    get() = settingsState.getValue(KEY_FONT_SCALE) as MutableState<String>

val Controller.displayDensityState
    get() = settingsState.getValue(KEY_DISPLAY_DENSITY) as MutableState<String>

val Controller.colorCorrectionState
    get() = settingsState.getValue(KEY_COLOR_CORRECTION) as MutableState<String>

val Controller.backgroundLimitState
    get() = settingsState.getValue(KEY_BACKGROUND_LIMIT) as MutableState<String>

val Controller.localeState
    get() = settingsState.getValue(KEY_LOCALE) as MutableState<String>

val Controller.screenTimeoutState
    get() = settingsState.getValue(KEY_SCREEN_TIMEOUT) as MutableState<String>

val Controller.brightnessState
    get() = settingsState.getValue(KEY_BRIGHTNESS) as MutableState<String>

// ============================================================================
// Change Functions - Network
// ============================================================================

fun Controller.changeWifi(value: Boolean) {
    updateState(KEY_WIFI, value, wifiState)
}

fun Controller.changeData(value: Boolean) {
    updateState(KEY_DATA, value, dataState)
}

fun Controller.changeAirplaneMode(value: Boolean) {
    updateState(KEY_AIRPLANE, value, airplaneModeState)
}

fun Controller.changeBluetooth(value: Boolean) {
    updateState(KEY_BLUETOOTH, value, bluetoothState)
}

// ============================================================================
// Change Functions - Debug Overlays
// ============================================================================

fun Controller.changeLayoutBounds(value: Boolean) {
    updateState(KEY_LAYOUT_BOUNDS, value, layoutBoundsState)
}

fun Controller.changeHwui(value: Boolean) {
    updateState(KEY_HWUI, value, hwuiState)
}

fun Controller.changeShowTaps(value: Boolean) {
    updateState(KEY_SHOW_TAPS, value, showTapsState)
}

fun Controller.changeShowFps(value: Boolean) {
    updateState(KEY_SHOW_FPS, value, showFpsState)
}

fun Controller.changePointerLocation(value: Boolean) {
    updateState(KEY_POINTER_LOCATION, value, pointerLocationState)
}

fun Controller.changeGpuOverdraw(value: Boolean) {
    updateState(KEY_GPU_OVERDRAW, value, gpuOverdrawState)
}

fun Controller.changeSurfaceUpdates(value: Boolean) {
    updateState(KEY_SURFACE_UPDATES, value, surfaceUpdatesState)
}

fun Controller.changeStrictMode(value: Boolean) {
    updateState(KEY_STRICT_MODE, value, strictModeState)
}

// ============================================================================
// Change Functions - Developer Options
// ============================================================================

fun Controller.changeStayAwake(value: Boolean) {
    updateState(KEY_STAY_AWAKE, value, stayAwakeState)
}

fun Controller.changeDontKeepActivities(value: Boolean) {
    updateState(KEY_DONT_KEEP_ACTIVITIES, value, dontKeepActivitiesState)
}

fun Controller.changeForceRtl(value: Boolean) {
    updateState(KEY_FORCE_RTL, value, forceRtlState)
}

fun Controller.changeUsbDebugging(value: Boolean) {
    updateState(KEY_USB_DEBUGGING, value, usbDebuggingState)
}

fun Controller.changeDemoMode(value: Boolean) {
    updateState(KEY_DEMO_MODE, value, demoModeState)
}

// ============================================================================
// Change Functions - Display & Accessibility
// ============================================================================

fun Controller.changeDarkMode(value: Boolean) {
    updateState(KEY_DARK_MODE, value, darkModeState)
}

fun Controller.changeHighContrast(value: Boolean) {
    updateState(KEY_HIGH_CONTRAST, value, highContrastState)
}

fun Controller.changeColorInversion(value: Boolean) {
    updateState(KEY_COLOR_INVERSION, value, colorInversionState)
}

fun Controller.changeMagnification(value: Boolean) {
    updateState(KEY_MAGNIFICATION, value, magnificationState)
}

fun Controller.changeBoldText(value: Boolean) {
    updateState(KEY_BOLD_TEXT, value, boldTextState)
}

fun Controller.changeAutoBrightness(value: Boolean) {
    updateState(KEY_AUTO_BRIGHTNESS, value, autoBrightnessState)
}

fun Controller.changeTalkBack(value: Boolean) {
    updateState(KEY_TALKBACK, value, talkBackState)
}

// ============================================================================
// Change Functions - Battery
// ============================================================================

fun Controller.changeBatterySaver(value: Boolean) {
    updateState(KEY_BATTERY_SAVER, value, batterySaverState)
}

// ============================================================================
// Change Functions - Range Settings
// ============================================================================

fun Controller.changeAnimationScale(value: String) {
    updateState(KEY_ANIMATION_SCALE, value, animationScaleState)
}

fun Controller.changeFpsScale(value: String) {
    updateState(KEY_FPS_SCALE, value, fpsScaleState)
}

fun Controller.changeFontScale(value: String) {
    updateState(KEY_FONT_SCALE, value, fontScaleState)
}

fun Controller.changeDisplayDensity(value: String) {
    updateState(KEY_DISPLAY_DENSITY, value, displayDensityState)
}

fun Controller.changeColorCorrection(value: String) {
    updateState(KEY_COLOR_CORRECTION, value, colorCorrectionState)
}

fun Controller.changeBackgroundLimit(value: String) {
    updateState(KEY_BACKGROUND_LIMIT, value, backgroundLimitState)
}

fun Controller.changeLocale(value: String) {
    updateState(KEY_LOCALE, value, localeState)
}

fun Controller.changeScreenTimeout(value: String) {
    updateState(KEY_SCREEN_TIMEOUT, value, screenTimeoutState)
}

fun Controller.changeBrightness(value: String) {
    updateState(KEY_BRIGHTNESS, value, brightnessState)
}

// ============================================================================
// Range Getters
// ============================================================================

suspend fun Controller.getAnimationScaleRange(): List<String> {
    return getRange(KEY_ANIMATION_SCALE)
}

suspend fun Controller.getFpsScaleRange(): List<String> {
    return getRange(KEY_FPS_SCALE)
}

suspend fun Controller.getFontScaleRange(): List<String> {
    return getRange(KEY_FONT_SCALE)
}

suspend fun Controller.getDisplayDensityRange(): List<String> {
    return getRange(KEY_DISPLAY_DENSITY)
}

suspend fun Controller.getColorCorrectionRange(): List<String> {
    return getRange(KEY_COLOR_CORRECTION)
}

suspend fun Controller.getBackgroundLimitRange(): List<String> {
    return getRange(KEY_BACKGROUND_LIMIT)
}

suspend fun Controller.getLocaleRange(): List<String> {
    return getRange(KEY_LOCALE)
}

suspend fun Controller.getScreenTimeoutRange(): List<String> {
    return getRange(KEY_SCREEN_TIMEOUT)
}

suspend fun Controller.getBrightnessRange(): List<String> {
    return getRange(KEY_BRIGHTNESS)
}

private suspend fun Controller.getRange(key: String): List<String> {
    return (actions[key] as? RangeAction)?.let { action ->
        selectedDevice?.let { action.getRange(it) } ?: emptyList()
    } ?: emptyList()
}

private fun Controller.updateState(key: String, value: Boolean, state: MutableState<Boolean>) =
    launch {
        try {
            (actions[key] as? ToggleAction)?.let { action ->
                selectedDevice?.let { device ->
                    if (!device.isOnline) {
                        showError("Device Offline", "Cannot execute command: device is offline")
                        return@launch
                    }
                    action.setValue(device, value)
                    state.value = action.getValue(device)
                    clearError()
                }
            }
        } catch (e: Exception) {
            showError("Command Failed", "Failed to change $key: ${e.message}")
        }
    }

private fun Controller.updateState(key: String, value: String, state: MutableState<String>) =
    launch {
        try {
            (actions[key] as? RangeAction)?.let { action ->
                selectedDevice?.let { device ->
                    if (!device.isOnline) {
                        showError("Device Offline", "Cannot execute command: device is offline")
                        return@launch
                    }
                    action.setValue(device, value)
                    state.value = action.getValue(device)
                    clearError()
                }
            }
        } catch (e: Exception) {
            showError("Command Failed", "Failed to change $key: ${e.message}")
        }
    }
