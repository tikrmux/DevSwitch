package com.tinkrmux.devswitch.actions

import com.android.ddmlib.IDevice
import com.tinkrmux.devswitch.executeShellCommand
import com.tinkrmux.devswitch.actions.ToggleAction.Companion.asFlag
import com.tinkrmux.devswitch.actions.ToggleAction.Companion.asNumber
import com.tinkrmux.devswitch.actions.ToggleAction.Companion.restartSystemUI

// ============================================================================
// Layout & Debug Overlay Actions
// ============================================================================

/**
 * Toggle Layout Bounds visualization.
 */
class ToggleLayoutBoundsAction : ToggleAction() {
    override suspend fun setValue(device: IDevice, value: Boolean) {
        device.executeShellCommand("setprop debug.layout $value")
        device.restartSystemUI()
    }

    override suspend fun getValue(device: IDevice): Boolean {
        return device.executeShellCommand("getprop debug.layout").asFlag
    }
}

/**
 * Toggle GPU Rendering Profile (HWUI bars).
 */
class ToggleHWUIAction : ToggleAction() {
    override suspend fun setValue(device: IDevice, value: Boolean) {
        val mode = if (value) "visual_bars" else "false"
        device.executeShellCommand("setprop debug.hwui.profile $mode")
        device.restartSystemUI()
    }

    override suspend fun getValue(device: IDevice): Boolean {
        return device.executeShellCommand("getprop debug.hwui.profile") == "visual_bars"
    }
}

/**
 * Toggle Show FPS Counter overlay.
 */
class ToggleShowFPSAction : ToggleAction() {
    override suspend fun setValue(device: IDevice, value: Boolean) {
        val mode = if (value) "1" else "0"
        device.executeShellCommand("service call SurfaceFlinger 1034 i32 $mode")
    }

    override suspend fun getValue(device: IDevice): Boolean {
        return false // Cannot reliably read this state
    }
}

/**
 * Toggle GPU Overdraw visualization.
 */
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

// ============================================================================
// Force RTL Layout Action
// ============================================================================

/**
 * Toggle Force RTL Layout direction.
 */
class ToggleForceRTLAction : ToggleAction() {
    override suspend fun setValue(device: IDevice, value: Boolean) {
        device.executeShellCommand("settings put global debug.force_rtl ${value.asNumber}")
        device.restartSystemUI()
    }

    override suspend fun getValue(device: IDevice): Boolean {
        return device.executeShellCommand("settings get global debug.force_rtl").asFlag
    }
}

// ============================================================================
// Animation Scale Actions
// ============================================================================

/**
 * Set Window Animation Scale.
 */
class SetWindowAnimationScaleAction : NumericRangeAction(0, 10) {
    override suspend fun setValue(device: IDevice, value: Int) {
        val scale = value / 10f
        device.executeShellCommand("settings put global window_animation_scale $scale")
    }

    override suspend fun getValue(device: IDevice): Int {
        val result = device.executeShellCommand("settings get global window_animation_scale")
        return ((result.toFloatOrNull() ?: 1f) * 10).toInt()
    }
}

/**
 * Set Transition Animation Scale.
 */
class SetTransitionAnimationScaleAction : NumericRangeAction(0, 10) {
    override suspend fun setValue(device: IDevice, value: Int) {
        val scale = value / 10f
        device.executeShellCommand("settings put global transition_animation_scale $scale")
    }

    override suspend fun getValue(device: IDevice): Int {
        val result = device.executeShellCommand("settings get global transition_animation_scale")
        return ((result.toFloatOrNull() ?: 1f) * 10).toInt()
    }
}

/**
 * Set Animator Duration Scale.
 */
class SetAnimatorDurationScaleAction : NumericRangeAction(0, 10) {
    override suspend fun setValue(device: IDevice, value: Int) {
        val scale = value / 10f
        device.executeShellCommand("settings put global animator_duration_scale $scale")
    }

    override suspend fun getValue(device: IDevice): Int {
        val result = device.executeShellCommand("settings get global animator_duration_scale")
        return ((result.toFloatOrNull() ?: 1f) * 10).toInt()
    }
}

// ============================================================================
// Unified Animation Control
// ============================================================================

/**
 * Set all animation scales at once.
 */
suspend fun IDevice.setAllAnimationScales(scale: Float) {
    executeShellCommand("settings put global window_animation_scale $scale")
    executeShellCommand("settings put global transition_animation_scale $scale")
    executeShellCommand("settings put global animator_duration_scale $scale")
}

/**
 * Disable all animations (useful for UI testing).
 */
suspend fun IDevice.disableAnimations() {
    setAllAnimationScales(0f)
}

/**
 * Enable all animations with default scale.
 */
suspend fun IDevice.enableAnimations() {
    setAllAnimationScales(1f)
}
