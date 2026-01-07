package com.tinkrmux.devswitch

import com.android.ddmlib.IDevice
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

    override suspend fun getRange(device: IDevice): List<String> =
        (1..10 step 1).map { it.toString() }.toList()

    override suspend fun setValue(device: IDevice, value: String) {
        val scale = value.toFloat()
        device.executeShellCommand("settings put global window_animation_scale $scale")
        device.executeShellCommand("settings put global transition_animation_scale $scale")
        device.executeShellCommand("settings put global animator_duration_scale $scale")
    }

    override suspend fun getValue(device: IDevice): String {
        return device.executeShellCommand("settings get global window_animation_scale")
            .trim()
            .dropLast(2)
    }
}

class FpsScaleAction : RangeAction() {

    private companion object {
        const val DUMPSYS_DISPLAY = "dumpsys display"
        val displayModeRegex = """DisplayMode\{.*? vsyncRate=([\d.]+)""".toRegex()
        val currentDisplayMode = """mActiveSfDisplayMode=DisplayMode\{.*? vsyncRate=([\d.]+)""".toRegex()
    }

    override suspend fun getRange(device: IDevice): List<String> {
        val dumpsysOutput = device.executeShellCommand(DUMPSYS_DISPLAY)
        return displayModeRegex.findAll(dumpsysOutput)
            .mapNotNull { match -> match.groupValues.getOrNull(1)?.toDoubleOrNull() }
            .map { round(it).toString() }
            .toSet()
            .toList()
    }

    override suspend fun setValue(device: IDevice, value: String) {
        device.executeShellCommand("settings put system min_refresh_rate $value")
        device.executeShellCommand("settings put system peak_refresh_rate $value")
    }

    override suspend fun getValue(device: IDevice): String {
        val dumpsysOutput = device.executeShellCommand(DUMPSYS_DISPLAY)
        return currentDisplayMode.find(dumpsysOutput)
            ?.groupValues
            ?.getOrNull(1)
            ?.toDoubleOrNull()
            ?.run { round(this) }
            ?.toString() ?: "-"
    }
}
