package com.tinkrmux.devswitch.actions

import com.android.ddmlib.IDevice
import com.tinkrmux.devswitch.executeShellCommand
import com.tinkrmux.devswitch.actions.ToggleAction.Companion.asFlag
import com.tinkrmux.devswitch.actions.ToggleAction.Companion.asNumber

// ============================================================================
// Network Toggle Actions
// ============================================================================

/**
 * Toggle WiFi on/off.
 */
class ToggleWiFiAction : ToggleAction() {
    private val Boolean.enableOrDisable: String get() = if (this) "enable" else "disable"

    override suspend fun setValue(device: IDevice, value: Boolean) {
        device.executeShellCommand("svc wifi ${value.enableOrDisable}")
    }

    override suspend fun getValue(device: IDevice): Boolean {
        return device.executeShellCommand("settings get global wifi_on").asFlag
    }
}

/**
 * Toggle Mobile Data on/off.
 */
class ToggleDataAction : ToggleAction() {
    private val Boolean.enableOrDisable: String get() = if (this) "enable" else "disable"

    override suspend fun setValue(device: IDevice, value: Boolean) {
        device.executeShellCommand("svc data ${value.enableOrDisable}")
    }

    override suspend fun getValue(device: IDevice): Boolean {
        return device.executeShellCommand("settings get global mobile_data").asFlag
    }
}

/**
 * Toggle Airplane Mode.
 */
class ToggleAirplaneModeAction : ToggleAction() {
    override suspend fun setValue(device: IDevice, value: Boolean) {
        device.executeShellCommand("settings put global airplane_mode_on ${value.asNumber}")
        device.executeShellCommand("am broadcast -a android.intent.action.AIRPLANE_MODE --ez state $value")
    }

    override suspend fun getValue(device: IDevice): Boolean {
        return device.executeShellCommand("settings get global airplane_mode_on").asFlag
    }
}

/**
 * Toggle Bluetooth.
 */
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
// Network Utility Functions
// ============================================================================

/**
 * Sets HTTP proxy configuration.
 */
suspend fun IDevice.setProxy(host: String, port: Int) {
    executeShellCommand("settings put global http_proxy $host:$port")
}

/**
 * Clears proxy configuration.
 */
suspend fun IDevice.clearProxy() {
    executeShellCommand("settings put global http_proxy :0")
}

/**
 * Gets current proxy configuration.
 */
suspend fun IDevice.getProxy(): String? {
    val result = executeShellCommand("settings get global http_proxy").trim()
    return if (result == "null" || result == ":0" || result.isEmpty()) null else result
}

/**
 * Broadcasts network connectivity change.
 */
suspend fun IDevice.broadcastNetworkChange() {
    executeShellCommand("am broadcast -a android.net.conn.CONNECTIVITY_CHANGE")
}

// ============================================================================
// Network Profile Data
// ============================================================================

data class NetworkProfile(
    val name: String,
    val latencyMs: Int,
    val bandwidthKbps: Int,
    val packetLoss: Float
)

val NETWORK_PROFILES = listOf(
    NetworkProfile("No Limit", 0, 0, 0f),
    NetworkProfile("4G/LTE", 50, 20000, 0.1f),
    NetworkProfile("3G", 200, 2000, 1f),
    NetworkProfile("2G/EDGE", 500, 128, 2f),
    NetworkProfile("GPRS", 800, 56, 5f),
    NetworkProfile("High Latency", 2000, 10000, 0f),
    NetworkProfile("Lossy Network", 100, 5000, 10f)
)
