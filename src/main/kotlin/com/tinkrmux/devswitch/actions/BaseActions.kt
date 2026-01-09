package com.tinkrmux.devswitch.actions

import com.android.ddmlib.IDevice
import com.tinkrmux.devswitch.executeShellCommand

/**
 * Base interface for all device settings actions.
 */
interface SettingsAction<T> {
    suspend fun setValue(device: IDevice, value: T)
    suspend fun getValue(device: IDevice): T
}

/**
 * Base class for toggle (on/off) actions.
 */
abstract class ToggleAction : SettingsAction<Boolean> {
    companion object {
        /** Convert Boolean to "1" or "0" for shell commands */
        val Boolean.asNumber: String get() = if (this) "1" else "0"
        
        /** Convert shell output to Boolean ("1" = true) */
        val String.asFlag: Boolean get() = trim() == "1"
        
        /** Restart SystemUI to apply visual changes */
        suspend fun IDevice.restartSystemUI() {
            executeShellCommand("service call activity 1599295570")
        }
    }
}

/**
 * Base class for range/dropdown actions with String values.
 */
abstract class RangeAction : SettingsAction<String> {
    abstract suspend fun getRange(device: IDevice): List<String>
}

/**
 * Base class for numeric range actions (e.g., animation scales, brightness).
 * @param min Minimum value of the range
 * @param max Maximum value of the range
 */
abstract class NumericRangeAction(val min: Int, val max: Int) {
    abstract suspend fun setValue(device: IDevice, value: Int)
    abstract suspend fun getValue(device: IDevice): Int
}
