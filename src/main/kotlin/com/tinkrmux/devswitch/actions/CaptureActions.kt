package com.tinkrmux.devswitch.actions

import com.android.ddmlib.IDevice
import com.tinkrmux.devswitch.executeShellCommand
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// ============================================================================
// Screenshot Actions
// ============================================================================

/**
 * Capture a screenshot from the device.
 */
suspend fun IDevice.captureScreenshot(outputPath: String): Boolean {
    return try {
        val devicePath = "/sdcard/screenshot_temp.png"
        executeShellCommand("screencap -p $devicePath")

        // Pull file to local path
        pullFile(devicePath, outputPath)
        
        // Clean up device file
        executeShellCommand("rm $devicePath")
        
        File(outputPath).exists()
    } catch (e: Exception) {
        false
    }
}

/**
 * Capture screenshot with auto-generated filename.
 */
suspend fun IDevice.captureScreenshotWithTimestamp(outputDir: String): String? {
    val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))
    val filename = "screenshot_$timestamp.png"
    val outputPath = "$outputDir/$filename"

    return if (captureScreenshot(outputPath)) outputPath else null
}

// ============================================================================
// Screen Recording Actions
// ============================================================================

/**
 * Start screen recording.
 * Note: Recording stops automatically after timeLimit or when stopScreenRecording is called.
 */
suspend fun IDevice.startScreenRecording(
    outputPath: String,
    timeLimit: Int = 180, // seconds, max 180 for most devices
    bitRate: Int = 4000000, // 4 Mbps
    size: String? = null // e.g., "1280x720"
): String {
    val sizeArg = size?.let { "--size $it" } ?: ""
    val command = "screenrecord --time-limit $timeLimit --bit-rate $bitRate $sizeArg $outputPath"
    return executeShellCommand(command)
}

/**
 * Stop screen recording by killing screenrecord process.
 */
suspend fun IDevice.stopScreenRecording() {
    executeShellCommand("pkill -SIGINT screenrecord")
}

/**
 * Pull recording from device to local path.
 */
suspend fun IDevice.pullRecording(devicePath: String, localPath: String): Boolean {
    return try {
        pullFile(devicePath, localPath)
        File(localPath).exists()
    } catch (e: Exception) {
        false
    }
}

// ============================================================================
// Bug Report / Logs
// ============================================================================

/**
 * Capture a bug report.
 */
suspend fun IDevice.captureBugReport(outputDir: String): String? {
    val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))
    val filename = "bugreport_$timestamp.zip"
    val devicePath = "/sdcard/$filename"
    val localPath = "$outputDir/$filename"

    try {
        executeShellCommand("bugreportz -o $devicePath")
        pullFile(devicePath, localPath)
        executeShellCommand("rm $devicePath")
        
        if (File(localPath).exists()) {
            return localPath
        }
    } catch (e: Exception) {
        // Fallback to regular bugreport
        try {
            val textFile = "$outputDir/bugreport_$timestamp.txt"
            val report = executeShellCommand("bugreport")
            File(textFile).writeText(report)
            if (File(textFile).exists()) {
                return textFile
            }
        } catch (e2: Exception) {
            // Ignore
        }
    }
    return null
}

/**
 * Get logcat output.
 */
suspend fun IDevice.getLogcat(
    lines: Int = 500,
    filter: String? = null,
    tag: String? = null,
    priority: String = "V" // V, D, I, W, E, F, S
): String {
    val filterArg = filter?.let { "-e \"$it\"" } ?: ""
    val tagArg = tag?.let { "-s $it" } ?: ""
    return executeShellCommand("logcat -d -t $lines $tagArg *:$priority $filterArg")
}

/**
 * Clear logcat buffer.
 */
suspend fun IDevice.clearLogcat() {
    executeShellCommand("logcat -c")
}

/**
 * Save logcat to file.
 */
suspend fun IDevice.saveLogcat(outputPath: String, lines: Int = 5000): Boolean {
    return try {
        val logcat = getLogcat(lines)
        File(outputPath).writeText(logcat)
        true
    } catch (e: Exception) {
        false
    }
}

// ============================================================================
// Window Dump / UI Analysis
// ============================================================================

/**
 * Dump window hierarchy (useful for UI automation).
 */
suspend fun IDevice.dumpWindowHierarchy(outputPath: String): Boolean {
    return try {
        val devicePath = "/sdcard/window_dump.xml"
        executeShellCommand("uiautomator dump $devicePath")
        pullFile(devicePath, outputPath)
        executeShellCommand("rm $devicePath")
        File(outputPath).exists()
    } catch (e: Exception) {
        false
    }
}

/**
 * Get current activity information.
 */
suspend fun IDevice.getCurrentActivity(): String {
    val result = executeShellCommand("dumpsys activity activities | grep mResumedActivity")
    return result.trim()
}

/**
 * Get current focused window.
 */
suspend fun IDevice.getCurrentFocusedWindow(): String {
    val result = executeShellCommand("dumpsys window | grep mCurrentFocus")
    return result.trim()
}
