@file:Suppress("UNCHECKED_CAST")

package com.tinkrmux.devswitch

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.launch

private const val KEY_WIFI = "wifi"
private const val KEY_DATA = "data"
private const val KEY_LAYOUT_BOUNDS = "layout_bounds"
private const val KEY_HWUI = "hwui"
private const val KEY_SHOW_TAPS = "taps"
private const val KEY_STAY_AWAKE = "stay_awake"
private const val KEY_SHOW_FPS = "show_fps"
private const val KEY_ANIMATION_SCALE = "animation_scale"
private const val KEY_FPS_SCALE = "fps_scale"

fun Controller.initExtensions() {
    mapOf(
        KEY_WIFI to ToggleWiFiAction(),
        KEY_DATA to ToggleDataAction(),
        KEY_LAYOUT_BOUNDS to ToggleLayoutBoundsAction(),
        KEY_HWUI to ToggleHwuiRenderingAction(),
        KEY_SHOW_TAPS to ToggleShowTapsAction(),
        KEY_STAY_AWAKE to ToggleStayAwakeAction(),
        KEY_SHOW_FPS to ToggleShowFPSAction(),
        KEY_ANIMATION_SCALE to AnimationScaleAction(),
        KEY_FPS_SCALE to FpsScaleAction(),
    ).also { actions.putAll(it) }

    mapOf(
        KEY_WIFI to mutableStateOf(false),
        KEY_DATA to mutableStateOf(false),
        KEY_LAYOUT_BOUNDS to mutableStateOf(false),
        KEY_HWUI to mutableStateOf(false),
        KEY_SHOW_TAPS to mutableStateOf(false),
        KEY_STAY_AWAKE to mutableStateOf(false),
        KEY_SHOW_FPS to mutableStateOf(false),
        KEY_ANIMATION_SCALE to mutableStateOf(""),
        KEY_FPS_SCALE to mutableStateOf(""),
    ).also { settingsState.putAll(it) }
}

val Controller.wifiState
    get() = settingsState.getValue(KEY_WIFI) as MutableState<Boolean>

val Controller.dataState
    get() = settingsState.getValue(KEY_DATA) as MutableState<Boolean>

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

val Controller.animationScaleState
    get() = settingsState.getValue(KEY_ANIMATION_SCALE) as MutableState<String>

val Controller.fpsScaleState
    get() = settingsState.getValue(KEY_FPS_SCALE) as MutableState<String>

fun Controller.changeWifi(value: Boolean) {
    updateState(KEY_WIFI, value, wifiState)
}

fun Controller.changeData(value: Boolean) {
    updateState(KEY_DATA, value, dataState)
}

fun Controller.changeLayoutBounds(value: Boolean) {
    updateState(KEY_LAYOUT_BOUNDS, value, layoutBoundsState)
}

fun Controller.changeHwui(value: Boolean) {
    updateState(KEY_HWUI, value, hwuiState)
}

fun Controller.changeShowTaps(value: Boolean) {
    updateState(KEY_SHOW_TAPS, value, showTapsState)
}

fun Controller.changeStayAwake(value: Boolean) {
    updateState(KEY_STAY_AWAKE, value, stayAwakeState)
}

fun Controller.changeShowFps(value: Boolean) {
    updateState(KEY_SHOW_FPS, value, showFpsState)
}

fun Controller.changeAnimationScale(value: String) {
    updateState(KEY_ANIMATION_SCALE, value, animationScaleState)
}

fun Controller.changeFpsScale(value: String) {
    updateState(KEY_FPS_SCALE, value, fpsScaleState)
}

suspend fun Controller.getAnimationScaleRange(): List<String> {
    return getRange(KEY_ANIMATION_SCALE)
}

suspend fun Controller.getFpsScaleRange(): List<String> {
    return getRange(KEY_FPS_SCALE)
}

private suspend fun Controller.getRange(key: String): List<String> {
    return (actions[key] as? RangeAction)?.let { action ->
        selectedDevice?.let { action.getRange(it) } ?: emptyList()
    } ?: emptyList()
}

private fun Controller.updateState(key: String, value: Boolean, state: MutableState<Boolean>) =
    launch {
        (actions[key] as? ToggleAction)?.let { action ->
            selectedDevice?.let {
                action.setValue(it, value)
                state.value = action.getValue(it)
            }
        }
    }

private fun Controller.updateState(key: String, value: String, state: MutableState<String>) =
    launch {
        (actions[key] as? RangeAction)?.let { action ->
            selectedDevice?.let {
                action.setValue(it, value)
                state.value = action.getValue(it)
            }
        }
    }
