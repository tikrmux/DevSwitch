package com.tinkrmux.devswitch

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposePanel
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.android.ddmlib.IDevice
import kotlinx.coroutines.launch
import java.awt.BorderLayout
import java.io.File
import javax.swing.JPanel

/**
 * Finds the ADB executable path by checking multiple sources in order:
 * 1. ANDROID_HOME environment variable
 * 2. ANDROID_SDK_ROOT environment variable
 * 3. Common default SDK locations for each OS
 */
private fun findAdbPath(): String {
    val adbExecutable = if (System.getProperty("os.name").lowercase().contains("win")) "adb.exe" else "adb"
    
    // 1. Check ANDROID_HOME environment variable
    System.getenv("ANDROID_HOME")?.let { androidHome ->
        val adbFile = File(androidHome, "platform-tools/$adbExecutable")
        if (adbFile.exists() && adbFile.canExecute()) {
            return adbFile.absolutePath
        }
    }
    
    // 2. Check ANDROID_SDK_ROOT environment variable
    System.getenv("ANDROID_SDK_ROOT")?.let { sdkRoot ->
        val adbFile = File(sdkRoot, "platform-tools/$adbExecutable")
        if (adbFile.exists() && adbFile.canExecute()) {
            return adbFile.absolutePath
        }
    }
    
    // 3. Check common default locations based on OS
    val osName = System.getProperty("os.name").lowercase()
    val userHome = System.getProperty("user.home")
    
    val defaultPaths = when {
        osName.contains("mac") -> listOf(
            "$userHome/Library/Android/sdk/platform-tools/$adbExecutable",
        )
        osName.contains("win") -> listOf(
            "$userHome\\AppData\\Local\\Android\\Sdk\\platform-tools\\$adbExecutable",
        )
        osName.contains("linux") -> listOf(
            "$userHome/Android/Sdk/platform-tools/$adbExecutable",
        )
        else -> emptyList()
    }
    
    for (path in defaultPaths) {
        val adbFile = File(path)
        if (adbFile.exists() && adbFile.canExecute()) {
            return adbFile.absolutePath
        }
    }
    
    // 4. Fallback: assume adb is in PATH
    return adbExecutable
}

fun createQuickSettingsUI(): JPanel {
    return JPanel().apply {
        layout = BorderLayout()

        ComposePanel()
            .apply { setContent { QuickSettingsPlugin() } }
            .also { add(it, BorderLayout.CENTER) }
    }
}

@Composable
fun ControllerProvider(controller: Controller, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalController provides controller) {
        content()
    }
}

@Composable
@Preview
fun QuickSettingsPlugin() {
    QuickSettingsTheme {
        val scrollState = rememberScrollState()
        val controller by remember {
            mutableStateOf(Controller().apply { initExtensions() })
        }

        ControllerProvider(controller) {
            Scaffold(
                topBar = {
                    DeviceSelectorWithAutoRefresh()
                }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                ) {
                    ControllerStates()
                }
            }
        }
    }
}

@Composable
fun DeviceSelectorWithAutoRefresh() {
    val controller = LocalController.current
    val coroutineScope = rememberCoroutineScope()
    val adbDeviceManager by remember { mutableStateOf(AdbDeviceManager(findAdbPath())) }
    var isDropdownExpanded by remember { mutableStateOf(false) }
    var availableDevices by remember { mutableStateOf(emptyList<IDevice>()) }
    val autoRefreshEnabledNoDevice by remember { derivedStateOf { controller.selectedDevice != null } }

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            adbDeviceManager.devices.collect { devices ->
                availableDevices = devices
                controller.selectedDevice = devices
                    .firstOrNull { it.name == controller.selectedDevice?.name }
                    ?: devices.firstOrNull()

            }
        }
    }

    LaunchedEffect(controller.selectedDevice, controller.autoRefreshEnabled) {
        if (controller.selectedDevice != null && controller.autoRefreshEnabled) {
            controller.startAutoRefresh()
        } else {
            controller.stopAutoRefresh()
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Box(modifier = Modifier) {
            OutlinedButton(onClick = { isDropdownExpanded = true }) {
                PluginText(
                    text = controller.selectedDevice?.name ?: "No device",
                    maxLines = 1,
                )
            }
            DropdownMenu(
                expanded = isDropdownExpanded,
                onDismissRequest = { isDropdownExpanded = false }
            ) {
                availableDevices.forEach { device ->
                    DropdownMenuItem(
                        onClick = {
                            isDropdownExpanded = false
                            controller.selectedDevice = device
                        }
                    ) {
                        PluginText(text = device.name, maxLines = 1)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Box(
            modifier = Modifier
                .width(2.dp)
                .height(24.dp)
                .background(Color.Gray, shape = CircleShape)
        )

        Checkbox(
            checked = controller.autoRefreshEnabled,
            enabled = autoRefreshEnabledNoDevice,
            onCheckedChange = { controller.autoRefreshEnabled = it }
        )

        PluginText(text = "Auto Refresh")
    }
}

@Composable
fun ControllerStates() {
    Column {
        ToggleWiFi()
        ToggleData()
        ToggleLayoutBounds()
        ToggleHwui()
        ToggleShowTaps()
        ToggleStayAwake()
        ToggleShowFps()
        AnimationScale()
        FpsScale()
    }
}

@Composable
fun ToggleActionComponent(
    text: String,
    value: Boolean,
    onValueChanged: (Boolean) -> Unit
) {
    val controller = LocalController.current
    val enabled by remember { derivedStateOf { controller.selectedDevice != null } }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Checkbox(checked = value, onCheckedChange = onValueChanged, enabled = enabled)
        PluginText(text)
    }
}

@Composable
fun ToggleWiFi() {
    val controller = LocalController.current
    val state by remember { controller.wifiState }

    ToggleActionComponent(
        text = "Toggle WiFi",
        value = state,
        onValueChanged = { controller.changeWifi(it) }
    )
}

@Composable
fun ToggleData() {
    val controller = LocalController.current
    val state by remember { controller.dataState }

    ToggleActionComponent(
        text = "Toggle Mobile Data",
        value = state,
        onValueChanged = { controller.changeData(it) }
    )
}

@Composable
fun ToggleLayoutBounds() {
    val controller = LocalController.current
    val state by remember { controller.layoutBoundsState }

    ToggleActionComponent(
        text = "Toggle Layout Bounds",
        value = state,
        onValueChanged = { controller.changeLayoutBounds(it) }
    )
}

@Composable
fun ToggleHwui() {
    val controller = LocalController.current
    val state by remember { controller.hwuiState }

    ToggleActionComponent(
        text = "Toggle HWUI",
        value = state,
        onValueChanged = { controller.changeHwui(it) }
    )
}

@Composable
fun ToggleShowTaps() {
    val controller = LocalController.current
    val state by remember { controller.showTapsState }

    ToggleActionComponent(
        text = "Toggle Show Taps",
        value = state,
        onValueChanged = { controller.changeShowTaps(it) }
    )
}

@Composable
fun ToggleStayAwake() {
    val controller = LocalController.current
    val state by remember { controller.stayAwakeState }

    ToggleActionComponent(
        text = "Toggle Stay Awake",
        value = state,
        onValueChanged = { controller.changeStayAwake(it) }
    )
}

@Composable
fun ToggleShowFps() {
    val controller = LocalController.current
    val state by remember { controller.showFpsState }

    ToggleActionComponent(
        text = "Toggle Show FPS",
        value = state,
        onValueChanged = { controller.changeShowFps(it) }
    )
}

@Composable
fun RangeActionComponent(
    text: String,
    options: List<String>,
    selectedOption: String = "Select an Option",
    onSelected: (String) -> Unit
) {
    val controller = LocalController.current
    val enabled by remember { derivedStateOf { controller.selectedDevice != null } }
    var expanded by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        PluginText(text)
        Spacer(modifier = Modifier.width(16.dp))
        Box(modifier = Modifier.weight(1f)) {
            OutlinedButton(
                onClick = { expanded = enabled },
                modifier = Modifier.wrapContentSize()
            ) {
                PluginText(text = selectedOption, maxLines = 1)
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(onClick = {
                        expanded = false
                        onSelected(option)
                    }) {
                        PluginText(text = option, maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
fun AnimationScale() {
    val controller = LocalController.current
    val state by remember { controller.animationScaleState }
    var options by remember { mutableStateOf(emptyList<String>()) }

    LaunchedEffect(controller.selectedDevice) {
        options = controller.getAnimationScaleRange()
    }

    RangeActionComponent(
        text = "Animation Scale",
        options = options,
        selectedOption = state,
        onSelected = { controller.changeAnimationScale(it) },
    )
}

@Composable
fun FpsScale() {
    val controller = LocalController.current
    val state by remember { controller.fpsScaleState }
    var options by remember { mutableStateOf(emptyList<String>()) }

    LaunchedEffect(controller.selectedDevice) {
        options = controller.getFpsScaleRange()
    }

    RangeActionComponent(
        text = "FPS Scale",
        options = options,
        selectedOption = state,
        onSelected = { controller.changeFpsScale(it) },
    )
}

@Composable
fun PluginText(
    text: String,
    modifier: Modifier = Modifier,
    maxLines: Int = 2
) {
    Text(
        text = text,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}