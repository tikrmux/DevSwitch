package com.tinkrmux.devswitch.actions

import com.android.ddmlib.IDevice
import com.tinkrmux.devswitch.executeShellCommand
import com.tinkrmux.devswitch.actions.ToggleAction.Companion.asFlag
import com.tinkrmux.devswitch.actions.ToggleAction.Companion.asNumber
import com.tinkrmux.devswitch.actions.ToggleAction.Companion.restartSystemUI

// ============================================================================
// USB & Debug Actions
// ============================================================================

/**
 * Toggle USB Debugging.
 */
class ToggleUSBDebugAction : ToggleAction() {
    override suspend fun setValue(device: IDevice, value: Boolean) {
        device.executeShellCommand("settings put global adb_enabled ${value.asNumber}")
    }

    override suspend fun getValue(device: IDevice): Boolean {
        return device.executeShellCommand("settings get global adb_enabled").asFlag
    }
}

/**
 * Toggle Stay Awake while charging.
 */
class ToggleStayAwakeAction : ToggleAction() {
    override suspend fun setValue(device: IDevice, value: Boolean) {
        val value = if (value) "1" else "0"
        device.executeShellCommand("settings put global stay_on_while_plugged_in $value")
    }

    override suspend fun getValue(device: IDevice): Boolean {
        val result = device.executeShellCommand("settings get global stay_on_while_plugged_in")
        return result.trim() != "0"
    }
}

/**
 * Toggle Demo Mode for clean screenshots.
 */
class ToggleDemoModeAction : ToggleAction() {
    override suspend fun setValue(device: IDevice, value: Boolean) {
        if (value) {
            // Enable demo mode
            device.executeShellCommand("settings put global sysui_demo_allowed 1")
            device.executeShellCommand("am broadcast -a com.android.systemui.demo -e command enter")
            // Set perfect status bar
            device.executeShellCommand("am broadcast -a com.android.systemui.demo -e command clock -e hhmm 1200")
            device.executeShellCommand("am broadcast -a com.android.systemui.demo -e command battery -e level 100 -e plugged false")
            device.executeShellCommand("am broadcast -a com.android.systemui.demo -e command network -e wifi show -e level 4")
            device.executeShellCommand("am broadcast -a com.android.systemui.demo -e command network -e mobile show -e level 4 -e datatype none")
            device.executeShellCommand("am broadcast -a com.android.systemui.demo -e command notifications -e visible false")
        } else {
            // Exit demo mode
            device.executeShellCommand("am broadcast -a com.android.systemui.demo -e command exit")
            device.executeShellCommand("settings put global sysui_demo_allowed 0")
        }
    }

    override suspend fun getValue(device: IDevice): Boolean {
        return device.executeShellCommand("settings get global sysui_demo_allowed").asFlag
    }
}

/**
 * Toggle Strict Mode visual indicators.
 */
class ToggleStrictModeAction : ToggleAction() {
    override suspend fun setValue(device: IDevice, value: Boolean) {
        device.executeShellCommand("settings put global strict_mode_visual_indicator ${value.asNumber}")
    }

    override suspend fun getValue(device: IDevice): Boolean {
        return device.executeShellCommand("settings get global strict_mode_visual_indicator").asFlag
    }
}

// ============================================================================
// Activity Lifecycle Actions
// ============================================================================

/**
 * Toggle Don't Keep Activities (destroys activities on leave).
 */
class ToggleDontKeepActivitiesAction : ToggleAction() {
    override suspend fun setValue(device: IDevice, value: Boolean) {
        device.executeShellCommand("settings put global always_finish_activities ${value.asNumber}")
    }

    override suspend fun getValue(device: IDevice): Boolean {
        return device.executeShellCommand("settings get global always_finish_activities").asFlag
    }
}

/**
 * Set background process limit.
 */
class SetBackgroundProcessLimitAction : NumericRangeAction(0, 4) {
    override suspend fun setValue(device: IDevice, value: Int) {
        // -1 = standard limit, 0-4 = max number of processes
        val limit = if (value == 0) -1 else value - 1
        device.executeShellCommand("settings put global app_standby_enabled ${if (value > 0) 1 else 0}")
        device.executeShellCommand("am set-inactive-processes $limit")
    }

    override suspend fun getValue(device: IDevice): Int {
        return 0 // Standard limit
    }
}

// ============================================================================
// Input & Interaction Actions
// ============================================================================

/**
 * Toggle Show Taps on screen.
 */
class ToggleShowTapsAction : ToggleAction() {
    override suspend fun setValue(device: IDevice, value: Boolean) {
        device.executeShellCommand("settings put system show_touches ${value.asNumber}")
    }

    override suspend fun getValue(device: IDevice): Boolean {
        return device.executeShellCommand("settings get system show_touches").asFlag
    }
}

/**
 * Toggle Pointer Location overlay.
 */
class TogglePointerLocationAction : ToggleAction() {
    override suspend fun setValue(device: IDevice, value: Boolean) {
        device.executeShellCommand("settings put system pointer_location ${value.asNumber}")
    }

    override suspend fun getValue(device: IDevice): Boolean {
        return device.executeShellCommand("settings get system pointer_location").asFlag
    }
}
