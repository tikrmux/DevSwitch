package com.tinkrmux.devswitch.actions

import com.android.ddmlib.IDevice
import com.tinkrmux.devswitch.executeShellCommand

// ============================================================================
// App Package Information
// ============================================================================

data class AppPackageInfo(
    val packageName: String,
    val versionName: String = "",
    val versionCode: String = "",
    val isSystemApp: Boolean = false,
    val isEnabled: Boolean = true,
    val firstInstallTime: String = "",
    val lastUpdateTime: String = ""
)

// ============================================================================
// App Management Actions
// ============================================================================

/**
 * Get list of installed packages.
 */
suspend fun IDevice.getInstalledPackages(includeSystem: Boolean = false): List<String> {
    val flag = if (includeSystem) "" else "-3" // -3 = third-party only
    val result = executeShellCommand("pm list packages $flag")
    return result.lines()
        .filter { it.startsWith("package:") }
        .map { it.removePrefix("package:").trim() }
        .sorted()
}

/**
 * Get detailed package info.
 */
suspend fun IDevice.getPackageInfo(packageName: String): AppPackageInfo? {
    val dumpsys = executeShellCommand("dumpsys package $packageName")
    if (dumpsys.contains("Unable to find package")) {
        return null
    }

    val versionName = Regex("versionName=([^\\s]+)").find(dumpsys)?.groupValues?.get(1) ?: ""
    val versionCode = Regex("versionCode=(\\d+)").find(dumpsys)?.groupValues?.get(1) ?: ""
    val isSystem = dumpsys.contains("SYSTEM") || dumpsys.contains("flags=.*s".toRegex())
    val isEnabled = !dumpsys.contains("enabled=2") && !dumpsys.contains("enabled=3")
    val firstInstall = Regex("firstInstallTime=([^\\s]+)").find(dumpsys)?.groupValues?.get(1) ?: ""
    val lastUpdate = Regex("lastUpdateTime=([^\\s]+)").find(dumpsys)?.groupValues?.get(1) ?: ""

    return AppPackageInfo(
        packageName = packageName,
        versionName = versionName,
        versionCode = versionCode,
        isSystemApp = isSystem,
        isEnabled = isEnabled,
        firstInstallTime = firstInstall,
        lastUpdateTime = lastUpdate
    )
}

/**
 * Force stop an app.
 */
suspend fun IDevice.forceStopApp(packageName: String): String {
    return executeShellCommand("am force-stop $packageName")
}

/**
 * Clear app data.
 */
suspend fun IDevice.clearAppData(packageName: String): String {
    return executeShellCommand("pm clear $packageName")
}

/**
 * Uninstall an app.
 */
suspend fun IDevice.uninstallApp(packageName: String, keepData: Boolean = false): String {
    val keepFlag = if (keepData) "-k" else ""
    return executeShellCommand("pm uninstall $keepFlag $packageName")
}

/**
 * Enable an app.
 */
suspend fun IDevice.enableApp(packageName: String): String {
    return executeShellCommand("pm enable $packageName")
}

/**
 * Disable an app.
 */
suspend fun IDevice.disableApp(packageName: String): String {
    return executeShellCommand("pm disable-user --user 0 $packageName")
}

/**
 * Launch an app by package name (opens default launcher activity).
 */
suspend fun IDevice.launchApp(packageName: String): String {
    return executeShellCommand("monkey -p $packageName -c android.intent.category.LAUNCHER 1")
}

// ============================================================================
// Permission Management
// ============================================================================

/**
 * Get permissions for a package.
 */
suspend fun IDevice.getAppPermissions(packageName: String): List<String> {
    val result = executeShellCommand("dumpsys package $packageName | grep permission")
    return result.lines()
        .filter { it.contains("android.permission.") }
        .map { it.trim() }
        .distinct()
}

/**
 * Get granted runtime permissions.
 */
suspend fun IDevice.getGrantedPermissions(packageName: String): List<String> {
    val result = executeShellCommand("dumpsys package $packageName | grep -A 1000 'runtime permissions:'")
    return result.lines()
        .filter { it.contains(": granted=true") }
        .mapNotNull { Regex("([\\w.]+): granted").find(it)?.groupValues?.get(1) }
}

/**
 * Grant a permission to an app.
 */
suspend fun IDevice.grantPermission(packageName: String, permission: String): String {
    return executeShellCommand("pm grant $packageName $permission")
}

/**
 * Revoke a permission from an app.
 */
suspend fun IDevice.revokePermission(packageName: String, permission: String): String {
    return executeShellCommand("pm revoke $packageName $permission")
}

/**
 * Reset all runtime permissions for an app.
 */
suspend fun IDevice.resetAppPermissions(packageName: String): String {
    return executeShellCommand("pm reset-permissions -p $packageName")
}

// ============================================================================
// Common Runtime Permissions
// ============================================================================

val COMMON_RUNTIME_PERMISSIONS = listOf(
    "android.permission.CAMERA" to "Camera",
    "android.permission.RECORD_AUDIO" to "Microphone",
    "android.permission.ACCESS_FINE_LOCATION" to "Fine Location",
    "android.permission.ACCESS_COARSE_LOCATION" to "Coarse Location",
    "android.permission.ACCESS_BACKGROUND_LOCATION" to "Background Location",
    "android.permission.READ_CONTACTS" to "Read Contacts",
    "android.permission.WRITE_CONTACTS" to "Write Contacts",
    "android.permission.READ_CALENDAR" to "Read Calendar",
    "android.permission.WRITE_CALENDAR" to "Write Calendar",
    "android.permission.READ_EXTERNAL_STORAGE" to "Read Storage",
    "android.permission.WRITE_EXTERNAL_STORAGE" to "Write Storage",
    "android.permission.CALL_PHONE" to "Phone Calls",
    "android.permission.READ_PHONE_STATE" to "Phone State",
    "android.permission.SEND_SMS" to "Send SMS",
    "android.permission.RECEIVE_SMS" to "Receive SMS",
    "android.permission.BODY_SENSORS" to "Body Sensors",
    "android.permission.ACTIVITY_RECOGNITION" to "Activity Recognition"
)

// ============================================================================
// Process Management
// ============================================================================

/**
 * Get running processes for an app.
 */
suspend fun IDevice.getAppProcesses(packageName: String): List<String> {
    val result = executeShellCommand("ps -A | grep $packageName")
    return result.lines().filter { it.isNotBlank() }
}

/**
 * Kill app process.
 */
suspend fun IDevice.killAppProcess(packageName: String): String {
    return executeShellCommand("am kill $packageName")
}

/**
 * Get memory info for an app.
 */
suspend fun IDevice.getAppMemoryInfo(packageName: String): String {
    return executeShellCommand("dumpsys meminfo $packageName")
}

// ============================================================================
// Battery & Background
// ============================================================================

/**
 * Put app in standby mode.
 */
suspend fun IDevice.setAppStandby(packageName: String, standby: Boolean): String {
    val mode = if (standby) "true" else "false"
    return executeShellCommand("am set-inactive $packageName $mode")
}

/**
 * Check if app is in standby.
 */
suspend fun IDevice.isAppInStandby(packageName: String): Boolean {
    val result = executeShellCommand("am get-inactive $packageName")
    return result.contains("Idle=true")
}

/**
 * Whitelist app from battery optimization.
 */
suspend fun IDevice.whitelistAppFromBatteryOptimization(packageName: String): String {
    return executeShellCommand("dumpsys deviceidle whitelist +$packageName")
}

/**
 * Remove app from battery whitelist.
 */
suspend fun IDevice.removeAppFromBatteryWhitelist(packageName: String): String {
    return executeShellCommand("dumpsys deviceidle whitelist -$packageName")
}
