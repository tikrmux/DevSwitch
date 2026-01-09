package com.tinkrmux.devswitch.actions

import com.android.ddmlib.IDevice
import com.tinkrmux.devswitch.executeShellCommand
import com.tinkrmux.devswitch.actions.ToggleAction.Companion.asNumber

// ============================================================================
// Battery Simulation
// ============================================================================

enum class BatteryStatus(val value: Int, val displayName: String) {
    UNKNOWN(1, "Unknown"),
    CHARGING(2, "Charging"),
    DISCHARGING(3, "Discharging"),
    NOT_CHARGING(4, "Not Charging"),
    FULL(5, "Full")
}

enum class BatteryPlugged(val value: Int, val displayName: String) {
    NONE(0, "Unplugged"),
    AC(1, "AC Charger"),
    USB(2, "USB"),
    WIRELESS(4, "Wireless")
}

/**
 * Unplug battery (simulate unplugged state).
 */
suspend fun IDevice.unplugBattery() {
    executeShellCommand("dumpsys battery unplug")
}

/**
 * Reset battery to real state.
 */
suspend fun IDevice.resetBattery() {
    executeShellCommand("dumpsys battery reset")
}

/**
 * Set battery level (requires unplug first).
 */
suspend fun IDevice.setBatteryLevel(level: Int) {
    executeShellCommand("dumpsys battery set level $level")
}

/**
 * Set battery status.
 */
suspend fun IDevice.setBatteryStatus(status: BatteryStatus) {
    executeShellCommand("dumpsys battery set status ${status.value}")
}

/**
 * Set battery plugged state.
 */
suspend fun IDevice.setBatteryPlugged(plugged: BatteryPlugged) {
    executeShellCommand("dumpsys battery set ${if (plugged == BatteryPlugged.NONE) "unplug" else "plug ${plugged.value}"}")
}

/**
 * Get current battery info.
 */
suspend fun IDevice.getBatteryInfo(): Map<String, String> {
    val result = executeShellCommand("dumpsys battery")
    val info = mutableMapOf<String, String>()
    
    result.lines().forEach { line ->
        val parts = line.trim().split(":")
        if (parts.size == 2) {
            info[parts[0].trim()] = parts[1].trim()
        }
    }
    
    return info
}

// ============================================================================
// Power Simulation
// ============================================================================

/**
 * Simulate power connected broadcast.
 */
suspend fun IDevice.simulatePowerConnected() {
    executeShellCommand("am broadcast -a android.intent.action.ACTION_POWER_CONNECTED")
}

/**
 * Simulate power disconnected broadcast.
 */
suspend fun IDevice.simulatePowerDisconnected() {
    executeShellCommand("am broadcast -a android.intent.action.ACTION_POWER_DISCONNECTED")
}

/**
 * Simulate battery low broadcast.
 */
suspend fun IDevice.simulateBatteryLow() {
    executeShellCommand("am broadcast -a android.intent.action.BATTERY_LOW")
}

/**
 * Simulate battery okay broadcast.
 */
suspend fun IDevice.simulateBatteryOkay() {
    executeShellCommand("am broadcast -a android.intent.action.BATTERY_OKAY")
}

// ============================================================================
// Doze Mode Simulation
// ============================================================================

/**
 * Force device into Doze mode.
 */
suspend fun IDevice.enterDozeMode() {
    executeShellCommand("dumpsys deviceidle force-idle")
}

/**
 * Exit Doze mode.
 */
suspend fun IDevice.exitDozeMode() {
    executeShellCommand("dumpsys deviceidle unforce")
}

/**
 * Step through Doze states.
 */
suspend fun IDevice.stepDozeMode() {
    executeShellCommand("dumpsys deviceidle step")
}

/**
 * Get current Doze state.
 */
suspend fun IDevice.getDozeState(): String {
    val result = executeShellCommand("dumpsys deviceidle get deep")
    return result.trim()
}

// ============================================================================
// App Standby Buckets (Android 9+)
// ============================================================================

enum class StandbyBucket(val value: Int, val displayName: String) {
    ACTIVE(10, "Active"),
    WORKING_SET(20, "Working Set"),
    FREQUENT(30, "Frequent"),
    RARE(40, "Rare"),
    RESTRICTED(45, "Restricted"),
    NEVER(50, "Never")
}

/**
 * Set app standby bucket.
 */
suspend fun IDevice.setAppStandbyBucket(packageName: String, bucket: StandbyBucket) {
    executeShellCommand("am set-standby-bucket $packageName ${bucket.value}")
}

/**
 * Get app standby bucket.
 */
suspend fun IDevice.getAppStandbyBucket(packageName: String): String {
    return executeShellCommand("am get-standby-bucket $packageName").trim()
}
