package com.tinkrmux.devswitch.actions

import com.android.ddmlib.IDevice
import com.tinkrmux.devswitch.executeShellCommand

// ============================================================================
// Deep Link Testing
// ============================================================================

/**
 * Launch a deep link / URI on the device.
 */
suspend fun IDevice.launchDeepLink(uri: String, packageName: String? = null): String {
    val packageFlag = packageName?.let { "-n $it" } ?: ""
    val command = "am start -a android.intent.action.VIEW -d \"$uri\" $packageFlag"
    return executeShellCommand(command)
}

/**
 * Launch a deep link with specific flags.
 */
suspend fun IDevice.launchDeepLinkWithFlags(
    uri: String,
    packageName: String? = null,
    newTask: Boolean = true,
    clearTask: Boolean = false,
    singleTop: Boolean = false
): String {
    val flags = buildList {
        if (newTask) add("--activity-new-task")
        if (clearTask) add("--activity-clear-task")
        if (singleTop) add("--activity-single-top")
    }.joinToString(" ")

    val packageFlag = packageName?.let { "-n $it" } ?: ""
    val command = "am start -a android.intent.action.VIEW -d \"$uri\" $packageFlag $flags"
    return executeShellCommand(command)
}

// ============================================================================
// Intent Sending
// ============================================================================

/**
 * Send a custom intent.
 */
suspend fun IDevice.sendIntent(
    action: String,
    data: String? = null,
    extras: Map<String, Any> = emptyMap(),
    component: String? = null,
    category: String? = null
): String {
    val parts = mutableListOf("am start", "-a \"$action\"")

    data?.let { parts.add("-d \"$it\"") }
    component?.let { parts.add("-n \"$it\"") }
    category?.let { parts.add("-c \"$it\"") }

    extras.forEach { (key, value) ->
        when (value) {
            is String -> parts.add("--es \"$key\" \"$value\"")
            is Int -> parts.add("--ei \"$key\" $value")
            is Long -> parts.add("--el \"$key\" $value")
            is Float -> parts.add("--ef \"$key\" $value")
            is Boolean -> parts.add("--ez \"$key\" $value")
        }
    }

    return executeShellCommand(parts.joinToString(" "))
}

/**
 * Start an activity by component name.
 */
suspend fun IDevice.startActivity(
    packageName: String,
    activityName: String,
    extras: Map<String, Any> = emptyMap()
): String {
    val fullActivity = if (activityName.startsWith(".")) {
        "$packageName/$packageName$activityName"
    } else if (activityName.contains("/")) {
        activityName
    } else {
        "$packageName/$activityName"
    }

    val extrasStr = extras.entries.joinToString(" ") { (key, value) ->
        when (value) {
            is String -> "--es \"$key\" \"$value\""
            is Int -> "--ei \"$key\" $value"
            is Long -> "--el \"$key\" $value"
            is Float -> "--ef \"$key\" $value"
            is Boolean -> "--ez \"$key\" $value"
            else -> ""
        }
    }

    return executeShellCommand("am start -n \"$fullActivity\" $extrasStr")
}

// ============================================================================
// Broadcast Actions
// ============================================================================

/**
 * Send a broadcast intent.
 */
suspend fun IDevice.sendBroadcast(
    action: String,
    extras: Map<String, Any> = emptyMap(),
    permission: String? = null
): String {
    val parts = mutableListOf("am broadcast", "-a \"$action\"")

    permission?.let { parts.add("--receiver-permission \"$it\"") }

    extras.forEach { (key, value) ->
        when (value) {
            is String -> parts.add("--es \"$key\" \"$value\"")
            is Int -> parts.add("--ei \"$key\" $value")
            is Long -> parts.add("--el \"$key\" $value")
            is Float -> parts.add("--ef \"$key\" $value")
            is Boolean -> parts.add("--ez \"$key\" $value")
        }
    }

    return executeShellCommand(parts.joinToString(" "))
}

/**
 * Common broadcast actions.
 */
object BroadcastActions {
    const val BOOT_COMPLETED = "android.intent.action.BOOT_COMPLETED"
    const val SCREEN_ON = "android.intent.action.SCREEN_ON"
    const val SCREEN_OFF = "android.intent.action.SCREEN_OFF"
    const val BATTERY_LOW = "android.intent.action.BATTERY_LOW"
    const val BATTERY_OKAY = "android.intent.action.BATTERY_OKAY"
    const val POWER_CONNECTED = "android.intent.action.ACTION_POWER_CONNECTED"
    const val POWER_DISCONNECTED = "android.intent.action.ACTION_POWER_DISCONNECTED"
    const val CONNECTIVITY_CHANGE = "android.net.conn.CONNECTIVITY_CHANGE"
    const val AIRPLANE_MODE = "android.intent.action.AIRPLANE_MODE"
    const val TIMEZONE_CHANGED = "android.intent.action.TIMEZONE_CHANGED"
    const val LOCALE_CHANGED = "android.intent.action.LOCALE_CHANGED"
    const val CONFIGURATION_CHANGED = "android.intent.action.CONFIGURATION_CHANGED"
}

// ============================================================================
// Deep Link History (in-memory for session)
// ============================================================================

data class DeepLinkEntry(
    val uri: String,
    val packageName: String?,
    val timestamp: Long = System.currentTimeMillis(),
    val success: Boolean = true
)

/**
 * Manager for deep link history within a session.
 */
class DeepLinkHistoryManager {
    private val history = mutableListOf<DeepLinkEntry>()
    private val maxHistorySize = 50

    fun addEntry(uri: String, packageName: String?, success: Boolean = true) {
        history.add(0, DeepLinkEntry(uri, packageName, success = success))
        if (history.size > maxHistorySize) {
            history.removeAt(history.lastIndex)
        }
    }

    fun getHistory(): List<DeepLinkEntry> = history.toList()

    fun clearHistory() {
        history.clear()
    }

    fun getRecentUris(limit: Int = 10): List<String> {
        return history.take(limit).map { it.uri }.distinct()
    }
}

// ============================================================================
// Common Deep Link Patterns
// ============================================================================

val COMMON_DEEP_LINK_PATTERNS = listOf(
    "https://example.com" to "Web URL",
    "tel:+1234567890" to "Phone Number",
    "mailto:test@example.com" to "Email",
    "sms:+1234567890" to "SMS",
    "geo:0,0?q=address" to "Maps Location",
    "market://details?id=com.example" to "Play Store",
    "intent://..." to "Custom Intent"
)
