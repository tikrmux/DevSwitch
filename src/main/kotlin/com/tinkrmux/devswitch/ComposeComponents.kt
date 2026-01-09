package com.tinkrmux.devswitch

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.android.ddmlib.IDevice
import kotlinx.coroutines.launch
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.*
import java.io.File

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

@Composable
fun ControllerProvider(controller: Controller, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalController provides controller) {
        content()
    }
}

@Composable
fun QuickSettingsPlugin() {
    DevSwitchTheme {
        val scrollState = rememberScrollState()
        val controller by remember {
            mutableStateOf(Controller().apply { initExtensions() })
        }

        ControllerProvider(controller) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top bar with device selector
                DeviceSelectorWithAutoRefresh()
                
                // Scrollable content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(8.dp)
                ) {
                    ControllerStates()
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DeviceSelectorWithAutoRefresh() {
    val controller = LocalController.current
    val coroutineScope = rememberCoroutineScope()
    val adbDeviceManager by remember { mutableStateOf(AdbDeviceManager(findAdbPath())) }
    var isDropdownExpanded by remember { mutableStateOf(false) }
    var availableDevices by remember { mutableStateOf(emptyList<IDevice>()) }
    val autoRefreshEnabledNoDevice by remember { derivedStateOf { controller.selectedDevice != null } }

    // Stop auto-refresh when tool window is hidden (composable leaves composition)
    DisposableEffect(Unit) {
        onDispose {
            controller.stopAutoRefresh()
        }
    }

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

    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Device dropdown
        Dropdown(
            modifier = Modifier.widthIn(min = 120.dp, max = 150.dp),
            menuContent = {
                availableDevices.forEach { device ->
                    selectableItem(
                        selected = device == controller.selectedDevice,
                        onClick = { controller.selectedDevice = device }
                    ) {
                        Text(device.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        ) {
            Text(
                text = controller.selectedDevice?.name ?: "No device",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = controller.autoRefreshEnabled,
                onCheckedChange = { controller.autoRefreshEnabled = it },
                enabled = autoRefreshEnabledNoDevice
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Auto Refresh", maxLines = 1)
        }
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
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onValueChanged(!value) }
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = value,
            onCheckedChange = { onValueChanged(it) },
            enabled = enabled
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
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

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        
        Dropdown(
            enabled = enabled,
            menuContent = {
                options.forEach { option ->
                    selectableItem(
                        selected = option == selectedOption,
                        onClick = { onSelected(option) }
                    ) {
                        Text(option, maxLines = 1)
                    }
                }
            }
        ) {
            Text(selectedOption, maxLines = 1, overflow = TextOverflow.Ellipsis)
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