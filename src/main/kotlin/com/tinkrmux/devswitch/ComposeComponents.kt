package com.tinkrmux.devswitch

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.ddmlib.IDevice
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.projectRoots.ProjectJdkTable
import kotlinx.coroutines.launch
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.*
import java.io.File

// Main tabs for the tool window
enum class MainTab(val displayName: String) {
    SETTINGS("Settings"),
    APP_MANAGER("App Manager"),
    DEEP_LINKS("Deep Links"),
    PRESETS("Presets"),
    CAPTURE("Capture")
}

// Setting categories
enum class SettingCategory(val displayName: String) {
    NETWORK("Network"),
    DEBUG("Debug Overlays"),
    DEVELOPER("Developer Options"),
    DISPLAY("Display & Accessibility"),
    PERFORMANCE("Performance"),
    BATTERY("Battery & Power")
}

// Setting item definition
data class SettingItem(
    val id: String,
    val name: String,
    val category: SettingCategory,
    val shortcutKey: Key? = null, // Optional keyboard shortcut (Alt + key)
    val isRange: Boolean = false
)

// Define all settings with their categories and shortcuts
val ALL_SETTINGS = listOf(
    // Network
    SettingItem("wifi", "WiFi", SettingCategory.NETWORK, Key.W),
    SettingItem("data", "Mobile Data", SettingCategory.NETWORK, Key.D),
    SettingItem("airplane_mode", "Airplane Mode", SettingCategory.NETWORK, Key.A),
    SettingItem("bluetooth", "Bluetooth", SettingCategory.NETWORK, Key.B),
    
    // Debug Overlays
    SettingItem("layout_bounds", "Layout Bounds", SettingCategory.DEBUG, Key.L),
    SettingItem("hwui", "HWUI Rendering", SettingCategory.DEBUG, Key.H),
    SettingItem("show_taps", "Show Taps", SettingCategory.DEBUG, Key.T),
    SettingItem("show_fps", "Show FPS", SettingCategory.DEBUG, Key.F),
    SettingItem("pointer_location", "Pointer Location", SettingCategory.DEBUG, Key.P),
    SettingItem("gpu_overdraw", "GPU Overdraw", SettingCategory.DEBUG, Key.O),
    SettingItem("surface_updates", "Surface Updates", SettingCategory.DEBUG),
    SettingItem("strict_mode", "Strict Mode Flash", SettingCategory.DEBUG),
    
    // Developer Options
    SettingItem("stay_awake", "Stay Awake", SettingCategory.DEVELOPER, Key.S),
    SettingItem("dont_keep_activities", "Don't Keep Activities", SettingCategory.DEVELOPER),
    SettingItem("force_rtl", "Force RTL", SettingCategory.DEVELOPER, Key.R),
    SettingItem("usb_debugging", "USB Debugging", SettingCategory.DEVELOPER),
    SettingItem("demo_mode", "Demo Mode", SettingCategory.DEVELOPER),
    SettingItem("background_limit", "Background Limit", SettingCategory.DEVELOPER, isRange = true),
    
    // Display & Accessibility
    SettingItem("dark_mode", "Dark Mode", SettingCategory.DISPLAY),
    SettingItem("font_scale", "Font Scale", SettingCategory.DISPLAY, isRange = true),
    SettingItem("display_density", "Display Density", SettingCategory.DISPLAY, isRange = true),
    SettingItem("high_contrast", "High Contrast", SettingCategory.DISPLAY),
    SettingItem("color_inversion", "Color Inversion", SettingCategory.DISPLAY),
    SettingItem("color_correction", "Color Correction", SettingCategory.DISPLAY, isRange = true),
    SettingItem("magnification", "Magnification", SettingCategory.DISPLAY),
    SettingItem("bold_text", "Bold Text", SettingCategory.DISPLAY),
    SettingItem("talkback", "TalkBack", SettingCategory.DISPLAY),
    SettingItem("locale", "Locale", SettingCategory.DISPLAY, isRange = true),
    SettingItem("screen_timeout", "Screen Timeout", SettingCategory.DISPLAY, isRange = true),
    SettingItem("brightness", "Brightness", SettingCategory.DISPLAY, isRange = true),
    SettingItem("auto_brightness", "Auto Brightness", SettingCategory.DISPLAY),
    
    // Performance
    SettingItem("animation_scale", "Animation Scale", SettingCategory.PERFORMANCE, isRange = true),
    SettingItem("fps_scale", "FPS Scale", SettingCategory.PERFORMANCE, isRange = true),
    
    // Battery
    SettingItem("battery_saver", "Battery Saver", SettingCategory.BATTERY),
)

// Persistence keys for collapsed state
private const val COLLAPSED_CATEGORIES_KEY = "com.tinkrmux.devswitch.collapsedCategories"
private const val SELECTED_TAB_KEY = "com.tinkrmux.devswitch.selectedTab"

/**
 * Finds the ADB executable path by checking multiple sources in order:
 * 1. IDE's configured Android SDK
 * 2. ANDROID_HOME environment variable
 * 3. ANDROID_SDK_ROOT environment variable
 * 4. Common default SDK locations for each OS
 */
private fun findAdbPath(): String {
    val adbExecutable = if (System.getProperty("os.name").lowercase().contains("win")) "adb.exe" else "adb"
    
    // 1. Try to get SDK path from IDE's Android SDK configuration
    try {
        val sdks = ProjectJdkTable.getInstance().allJdks
        for (sdk in sdks) {
            // Check if it's an Android SDK by looking at the SDK type name
            val sdkTypeName = sdk.sdkType.name
            if (sdkTypeName.contains("Android", ignoreCase = true)) {
                sdk.homePath?.let { homePath ->
                    val adbFile = File(homePath, "platform-tools/$adbExecutable")
                    if (adbFile.exists() && adbFile.canExecute()) {
                        return adbFile.absolutePath
                    }
                }
            }
        }
    } catch (e: Exception) {
        // SDK lookup might fail, continue to fallback
    }
    
    // 2. Check ANDROID_HOME environment variable
    System.getenv("ANDROID_HOME")?.let { androidHome ->
        val adbFile = File(androidHome, "platform-tools/$adbExecutable")
        if (adbFile.exists() && adbFile.canExecute()) {
            return adbFile.absolutePath
        }
    }
    
    // 3. Check ANDROID_SDK_ROOT environment variable
    System.getenv("ANDROID_SDK_ROOT")?.let { sdkRoot ->
        val adbFile = File(sdkRoot, "platform-tools/$adbExecutable")
        if (adbFile.exists() && adbFile.canExecute()) {
            return adbFile.absolutePath
        }
    }
    
    // 4. Check common default locations based on OS
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
    
    // 5. Fallback: assume adb is in PATH
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
        val controller by remember {
            mutableStateOf(Controller().apply { initExtensions() })
        }
        
        // Tab state - persisted
        var selectedTab by remember {
            val saved = PropertiesComponent.getInstance().getValue(SELECTED_TAB_KEY, MainTab.SETTINGS.name)
            mutableStateOf(MainTab.entries.find { it.name == saved } ?: MainTab.SETTINGS)
        }

        // Dispose controller when composable leaves composition
        DisposableEffect(controller) {
            onDispose {
                controller.dispose()
            }
        }
        
        // Handle keyboard shortcuts (Alt + Key)
        val keyboardHandler = Modifier.onPreviewKeyEvent { event ->
            if (event.type == KeyEventType.KeyDown && event.isAltPressed && controller.selectedDevice != null) {
                val setting = ALL_SETTINGS.find { it.shortcutKey == event.key }
                if (setting != null && !setting.isRange) {
                    controller.toggleSetting(setting.id)
                    true
                } else false
            } else false
        }

        ControllerProvider(controller) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .then(keyboardHandler)
            ) {
                // Top bar with device selector
                DeviceSelectorWithAutoRefresh()
                
                // Tab bar
                TabBar(
                    selectedTab = selectedTab,
                    onTabSelected = { tab ->
                        selectedTab = tab
                        PropertiesComponent.getInstance().setValue(SELECTED_TAB_KEY, tab.name)
                    }
                )
                
                // Tab content
                when (selectedTab) {
                    MainTab.SETTINGS -> SettingsTab()
                    MainTab.APP_MANAGER -> AppManagerTab()
                    MainTab.DEEP_LINKS -> DeepLinksTab()
                    MainTab.PRESETS -> PresetsTab()
                    MainTab.CAPTURE -> CaptureTab()
                }
            }
        }
    }
}

@Composable
fun TabBar(selectedTab: MainTab, onTabSelected: (MainTab) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        MainTab.entries.forEach { tab ->
            val isSelected = tab == selectedTab
            Text(
                text = tab.displayName,
                maxLines = 1,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        if (isSelected) JewelTheme.globalColors.outlines.focused.copy(alpha = 0.2f)
                        else Color.Transparent
                    )
                    .clickable { onTabSelected(tab) }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
fun SettingsTab() {
    val scrollState = rememberScrollState()
    
    // Search filter state
    var searchQuery by remember { mutableStateOf("") }
    
    // Collapsed categories state - persisted
    var collapsedCategories by remember {
        val saved = PropertiesComponent.getInstance().getValue(COLLAPSED_CATEGORIES_KEY, "")
        mutableStateOf(saved.split(",").filter { it.isNotEmpty() }.toSet())
    }
    
    Column {
        // Search bar
        SearchBar(
            query = searchQuery,
            onQueryChange = { searchQuery = it }
        )
        
        // Scrollable content with categories
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(8.dp)
        ) {
            ControllerStates(
                searchQuery = searchQuery,
                collapsedCategories = collapsedCategories,
                onToggleCategory = { category ->
                    val newSet = if (category.name in collapsedCategories) {
                        collapsedCategories - category.name
                    } else {
                        collapsedCategories + category.name
                    }
                    collapsedCategories = newSet
                    PropertiesComponent.getInstance().setValue(
                        COLLAPSED_CATEGORIES_KEY,
                        newSet.joinToString(",")
                    )
                }
            )
        }
    }
}

@Composable
fun SearchBar(query: String, onQueryChange: (String) -> Unit) {
    val textFieldState = remember { TextFieldState(query) }
    
    // Sync external changes to state
    LaunchedEffect(query) {
        if (textFieldState.text.toString() != query) {
            textFieldState.setTextAndPlaceCursorAtEnd(query)
        }
    }
    
    // Observe state changes and propagate
    LaunchedEffect(textFieldState) {
        snapshotFlow { textFieldState.text.toString() }
            .collect { onQueryChange(it) }
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextField(
            state = textFieldState,
            placeholder = { Text("Search settings...") },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DeviceSelectorWithAutoRefresh() {
    val controller = LocalController.current
    val coroutineScope = rememberCoroutineScope()
    val adbDeviceManager by remember { mutableStateOf(AdbDeviceManager(findAdbPath())) }
    var availableDevices by remember { mutableStateOf(emptyList<IDevice>()) }
    val autoRefreshEnabledNoDevice by remember { derivedStateOf { controller.selectedDevice != null } }
    
    // Device online status - derived from availableDevices to update on device changes
    val isDeviceOnline by remember(availableDevices, controller.selectedDevice) {
        derivedStateOf { 
            availableDevices.find { it.serialNumber == controller.selectedDevice?.serialNumber }?.isOnline == true
        }
    }

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
                    .firstOrNull { it.serialNumber == controller.selectedDevice?.serialNumber }
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Device dropdown with status indicator
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Online/Offline indicator
                DeviceStatusIndicator(isOnline = isDeviceOnline)
                Spacer(modifier = Modifier.width(8.dp))
                
                Dropdown(
                    modifier = Modifier.widthIn(min = 120.dp, max = 150.dp),
                    menuContent = {
                        availableDevices.forEach { device ->
                            selectableItem(
                                selected = device == controller.selectedDevice,
                                onClick = { controller.selectedDevice = device }
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    DeviceStatusIndicator(isOnline = device.isOnline)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(device.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
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
            }

            // Auto Refresh checkbox
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable(enabled = autoRefreshEnabledNoDevice) {
                    controller.autoRefreshEnabled = !controller.autoRefreshEnabled
                }
            ) {
                Checkbox(
                    checked = controller.autoRefreshEnabled,
                    onCheckedChange = { controller.autoRefreshEnabled = it },
                    enabled = autoRefreshEnabledNoDevice
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Auto", maxLines = 1)
            }
            
            // Refresh interval dropdown
            if (controller.autoRefreshEnabled) {
                Dropdown(
                    modifier = Modifier.widthIn(min = 60.dp, max = 80.dp),
                    enabled = autoRefreshEnabledNoDevice,
                    menuContent = {
                        REFRESH_INTERVALS.forEach { (interval, label) ->
                            selectableItem(
                                selected = interval == controller.refreshInterval,
                                onClick = { controller.refreshInterval = interval }
                            ) {
                                Text(label, maxLines = 1)
                            }
                        }
                    }
                ) {
                    Text(
                        text = REFRESH_INTERVALS.find { it.first == controller.refreshInterval }?.second ?: "1s",
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun DeviceStatusIndicator(isOnline: Boolean) {
    val color = if (isOnline) Color(0xFF4CAF50) else Color(0xFFE53935)
    Box(
        modifier = Modifier
            .size(8.dp)
            .background(color, CircleShape)
    )
}

@Composable
fun ControllerStates(
    searchQuery: String = "",
    collapsedCategories: Set<String> = emptySet(),
    onToggleCategory: (SettingCategory) -> Unit = {}
) {
    val controller = LocalController.current
    
    // Filter settings based on search query
    val filteredSettings = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            ALL_SETTINGS
        } else {
            ALL_SETTINGS.filter { 
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.category.displayName.contains(searchQuery, ignoreCase = true)
            }
        }
    }
    
    // Group by category
    val settingsByCategory = remember(filteredSettings) {
        filteredSettings.groupBy { it.category }
    }
    
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SettingCategory.entries.forEach { category ->
            val settingsInCategory = settingsByCategory[category] ?: emptyList()
            if (settingsInCategory.isNotEmpty()) {
                CategorySection(
                    category = category,
                    isCollapsed = category.name in collapsedCategories,
                    onToggleCollapse = { onToggleCategory(category) }
                ) {
                    settingsInCategory.forEach { setting ->
                        SettingRow(setting)
                    }
                }
            }
        }
    }
}

@Composable
fun CategorySection(
    category: SettingCategory,
    isCollapsed: Boolean,
    onToggleCollapse: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        // Category header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .clickable { onToggleCollapse() }
                .background(JewelTheme.globalColors.outlines.focused.copy(alpha = 0.1f))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = category.displayName,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            Text(
                text = if (isCollapsed) "▶" else "▼",
                maxLines = 1
            )
        }
        
        // Category content
        if (!isCollapsed) {
            Column(
                modifier = Modifier.padding(start = 4.dp, top = 4.dp),
                content = content
            )
        }
    }
}

@Composable
fun SettingRow(setting: SettingItem) {
    val controller = LocalController.current
    val shortcutHint = setting.shortcutKey?.let { "Alt+${it.toString().removePrefix("Key ")}" }
    
    if (setting.isRange) {
        // Range setting - use dropdown
        when (setting.id) {
            "animation_scale" -> AnimationScale()
            "fps_scale" -> FpsScale()
            "font_scale" -> FontScale()
            "display_density" -> DisplayDensity()
            "color_correction" -> ColorCorrection()
            "background_limit" -> BackgroundLimit()
            "locale" -> LocaleSelector()
            "screen_timeout" -> ScreenTimeout()
            "brightness" -> Brightness()
        }
    } else {
        // Toggle setting
        val state = when (setting.id) {
            "wifi" -> controller.wifiState
            "data" -> controller.dataState
            "airplane_mode" -> controller.airplaneModeState
            "bluetooth" -> controller.bluetoothState
            "layout_bounds" -> controller.layoutBoundsState
            "hwui" -> controller.hwuiState
            "show_taps" -> controller.showTapsState
            "show_fps" -> controller.showFpsState
            "pointer_location" -> controller.pointerLocationState
            "gpu_overdraw" -> controller.gpuOverdrawState
            "surface_updates" -> controller.surfaceUpdatesState
            "strict_mode" -> controller.strictModeState
            "stay_awake" -> controller.stayAwakeState
            "dont_keep_activities" -> controller.dontKeepActivitiesState
            "force_rtl" -> controller.forceRtlState
            "usb_debugging" -> controller.usbDebuggingState
            "demo_mode" -> controller.demoModeState
            "dark_mode" -> controller.darkModeState
            "high_contrast" -> controller.highContrastState
            "color_inversion" -> controller.colorInversionState
            "magnification" -> controller.magnificationState
            "bold_text" -> controller.boldTextState
            "talkback" -> controller.talkBackState
            "auto_brightness" -> controller.autoBrightnessState
            "battery_saver" -> controller.batterySaverState
            else -> return
        }
        
        val onChange: (Boolean) -> Unit = when (setting.id) {
            "wifi" -> { v -> controller.changeWifi(v) }
            "data" -> { v -> controller.changeData(v) }
            "airplane_mode" -> { v -> controller.changeAirplaneMode(v) }
            "bluetooth" -> { v -> controller.changeBluetooth(v) }
            "layout_bounds" -> { v -> controller.changeLayoutBounds(v) }
            "hwui" -> { v -> controller.changeHwui(v) }
            "show_taps" -> { v -> controller.changeShowTaps(v) }
            "show_fps" -> { v -> controller.changeShowFps(v) }
            "pointer_location" -> { v -> controller.changePointerLocation(v) }
            "gpu_overdraw" -> { v -> controller.changeGpuOverdraw(v) }
            "surface_updates" -> { v -> controller.changeSurfaceUpdates(v) }
            "strict_mode" -> { v -> controller.changeStrictMode(v) }
            "stay_awake" -> { v -> controller.changeStayAwake(v) }
            "dont_keep_activities" -> { v -> controller.changeDontKeepActivities(v) }
            "force_rtl" -> { v -> controller.changeForceRtl(v) }
            "usb_debugging" -> { v -> controller.changeUsbDebugging(v) }
            "demo_mode" -> { v -> controller.changeDemoMode(v) }
            "dark_mode" -> { v -> controller.changeDarkMode(v) }
            "high_contrast" -> { v -> controller.changeHighContrast(v) }
            "color_inversion" -> { v -> controller.changeColorInversion(v) }
            "magnification" -> { v -> controller.changeMagnification(v) }
            "bold_text" -> { v -> controller.changeBoldText(v) }
            "talkback" -> { v -> controller.changeTalkBack(v) }
            "auto_brightness" -> { v -> controller.changeAutoBrightness(v) }
            "battery_saver" -> { v -> controller.changeBatterySaver(v) }
            else -> return
        }
        
        val value by remember { state }
        ToggleActionComponent(
            text = setting.name,
            value = value,
            onValueChanged = onChange,
            shortcutHint = shortcutHint
        )
    }
}

@Composable
fun ToggleActionComponent(
    text: String,
    value: Boolean,
    onValueChanged: (Boolean) -> Unit,
    shortcutHint: String? = null
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
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (shortcutHint != null) {
            Text(
                text = shortcutHint,
                maxLines = 1,
                style = JewelTheme.defaultTextStyle.copy(
                    color = JewelTheme.globalColors.text.info,
                    fontSize = 10.sp
                )
            )
        }
    }
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

@Composable
fun FontScale() {
    val controller = LocalController.current
    val state by remember { controller.fontScaleState }
    var options by remember { mutableStateOf(emptyList<String>()) }

    LaunchedEffect(controller.selectedDevice) {
        options = controller.getFontScaleRange()
    }

    RangeActionComponent(
        text = "Font Scale",
        options = options,
        selectedOption = state.ifEmpty { "1.0x" },
        onSelected = { controller.changeFontScale(it) },
    )
}

@Composable
fun DisplayDensity() {
    val controller = LocalController.current
    val state by remember { controller.displayDensityState }
    var options by remember { mutableStateOf(emptyList<String>()) }

    LaunchedEffect(controller.selectedDevice) {
        options = controller.getDisplayDensityRange()
    }

    RangeActionComponent(
        text = "Display Density",
        options = options,
        selectedOption = state.ifEmpty { "Default" },
        onSelected = { controller.changeDisplayDensity(it) },
    )
}

@Composable
fun ColorCorrection() {
    val controller = LocalController.current
    val state by remember { controller.colorCorrectionState }
    var options by remember { mutableStateOf(emptyList<String>()) }

    LaunchedEffect(controller.selectedDevice) {
        options = controller.getColorCorrectionRange()
    }

    RangeActionComponent(
        text = "Color Correction",
        options = options,
        selectedOption = state.ifEmpty { "Disabled" },
        onSelected = { controller.changeColorCorrection(it) },
    )
}

@Composable
fun BackgroundLimit() {
    val controller = LocalController.current
    val state by remember { controller.backgroundLimitState }
    var options by remember { mutableStateOf(emptyList<String>()) }

    LaunchedEffect(controller.selectedDevice) {
        options = controller.getBackgroundLimitRange()
    }

    RangeActionComponent(
        text = "Background Limit",
        options = options,
        selectedOption = state.ifEmpty { "Standard limit" },
        onSelected = { controller.changeBackgroundLimit(it) },
    )
}

@Composable
fun LocaleSelector() {
    val controller = LocalController.current
    val state by remember { controller.localeState }
    var options by remember { mutableStateOf(emptyList<String>()) }

    LaunchedEffect(controller.selectedDevice) {
        options = controller.getLocaleRange()
    }

    RangeActionComponent(
        text = "Locale",
        options = options,
        selectedOption = state.ifEmpty { "English (US) (en-US)" },
        onSelected = { controller.changeLocale(it) },
    )
}

@Composable
fun ScreenTimeout() {
    val controller = LocalController.current
    val state by remember { controller.screenTimeoutState }
    var options by remember { mutableStateOf(emptyList<String>()) }

    LaunchedEffect(controller.selectedDevice) {
        options = controller.getScreenTimeoutRange()
    }

    RangeActionComponent(
        text = "Screen Timeout",
        options = options,
        selectedOption = state.ifEmpty { "1 min" },
        onSelected = { controller.changeScreenTimeout(it) },
    )
}

@Composable
fun Brightness() {
    val controller = LocalController.current
    val state by remember { controller.brightnessState }
    var options by remember { mutableStateOf(emptyList<String>()) }

    LaunchedEffect(controller.selectedDevice) {
        options = controller.getBrightnessRange()
    }

    RangeActionComponent(
        text = "Brightness",
        options = options,
        selectedOption = state.ifEmpty { "50%" },
        onSelected = { controller.changeBrightness(it) },
    )
}

// ============================================================================
// App Manager Tab
// ============================================================================

@Composable
fun AppManagerTab() {
    val controller = LocalController.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    
    var packages by remember { mutableStateOf(emptyList<String>()) }
    var selectedPackage by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    
    val enabled = controller.selectedDevice != null
    
    // Load packages when device changes
    LaunchedEffect(controller.selectedDevice) {
        controller.selectedDevice?.let { device ->
            isLoading = true
            try {
                packages = device.getInstalledPackages(includeSystem = false)
            } catch (e: Exception) {
                controller.showError("Error", "Failed to load packages: ${e.message}")
            }
            isLoading = false
        }
    }
    
    val filteredPackages = remember(packages, searchQuery) {
        if (searchQuery.isBlank()) packages
        else packages.filter { it.contains(searchQuery, ignoreCase = true) }
    }
    
    val searchFieldState = remember { TextFieldState("") }
    
    // Sync searchFieldState to searchQuery
    LaunchedEffect(searchFieldState) {
        snapshotFlow { searchFieldState.text.toString() }
            .collect { searchQuery = it }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        // Search
        TextField(
            state = searchFieldState,
            placeholder = { Text("Search packages...") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            OutlinedButton(
                onClick = {
                    coroutineScope.launch {
                        controller.selectedDevice?.let { device ->
                            try {
                                val fg = device.getForegroundApp()
                                if (fg != null) {
                                    selectedPackage = fg
                                    searchQuery = fg
                                }
                            } catch (e: Exception) {
                                controller.showError("Error", e.message ?: "Failed")
                            }
                        }
                    }
                },
                enabled = enabled
            ) {
                Text("Current App", maxLines = 1, fontSize = 10.sp)
            }
            
            OutlinedButton(
                onClick = {
                    coroutineScope.launch {
                        isLoading = true
                        controller.selectedDevice?.let { device ->
                            packages = device.getInstalledPackages(includeSystem = false)
                        }
                        isLoading = false
                    }
                },
                enabled = enabled
            ) {
                Text("Refresh", maxLines = 1, fontSize = 10.sp)
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        if (isLoading) {
            Text("Loading packages...")
        } else {
            // Package list
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
            ) {
                filteredPackages.take(50).forEach { pkg ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (pkg == selectedPackage) 
                                    JewelTheme.globalColors.outlines.focused.copy(alpha = 0.2f)
                                else Color.Transparent
                            )
                            .clickable { selectedPackage = pkg }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = pkg,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (filteredPackages.size > 50) {
                    Text(
                        "... and ${filteredPackages.size - 50} more",
                        fontSize = 10.sp,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
        
        // Selected package actions
        selectedPackage?.let { pkg ->
            Spacer(modifier = Modifier.height(8.dp))
            Text("Selected: $pkg", fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        coroutineScope.launch {
                            controller.selectedDevice?.let { device ->
                                try {
                                    device.launchApp(pkg)
                                } catch (e: Exception) {
                                    controller.showError("Error", e.message ?: "Failed")
                                }
                            }
                        }
                    },
                    enabled = enabled
                ) {
                    Text("Launch", fontSize = 10.sp)
                }
                
                OutlinedButton(
                    onClick = {
                        coroutineScope.launch {
                            controller.selectedDevice?.let { device ->
                                try {
                                    device.forceStopApp(pkg)
                                    controller.showWarning("Success", "Force stopped $pkg")
                                } catch (e: Exception) {
                                    controller.showError("Error", e.message ?: "Failed")
                                }
                            }
                        }
                    },
                    enabled = enabled
                ) {
                    Text("Stop", fontSize = 10.sp)
                }
                
                OutlinedButton(
                    onClick = {
                        coroutineScope.launch {
                            controller.selectedDevice?.let { device ->
                                try {
                                    device.clearAppData(pkg)
                                    controller.showWarning("Success", "Cleared data for $pkg")
                                } catch (e: Exception) {
                                    controller.showError("Error", e.message ?: "Failed")
                                }
                            }
                        }
                    },
                    enabled = enabled
                ) {
                    Text("Clear", fontSize = 10.sp)
                }
            }
        }
    }
}

// ============================================================================
// Deep Links Tab
// ============================================================================

@Composable
fun DeepLinksTab() {
    val controller = LocalController.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    
    var deepLinkUrl by remember { mutableStateOf("") }
    var targetPackage by remember { mutableStateOf("") }
    var urlHistory by remember { mutableStateOf(listOf<String>()) }
    var selectedBroadcast by remember { mutableStateOf<String?>(null) }
    
    val deepLinkState = remember { TextFieldState("") }
    val targetPackageState = remember { TextFieldState("") }
    
    // Sync states
    LaunchedEffect(deepLinkState) {
        snapshotFlow { deepLinkState.text.toString() }
            .collect { deepLinkUrl = it }
    }
    LaunchedEffect(targetPackageState) {
        snapshotFlow { targetPackageState.text.toString() }
            .collect { targetPackage = it }
    }
    
    val enabled = controller.selectedDevice != null
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(8.dp)
    ) {
        // Deep Link Section
        Text("Deep Link Tester", fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        
        TextField(
            state = deepLinkState,
            placeholder = { Text("https://example.com/path or myapp://action") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        TextField(
            state = targetPackageState,
            placeholder = { Text("Target package (optional)") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            DefaultButton(
                onClick = {
                    coroutineScope.launch {
                        controller.selectedDevice?.let { device ->
                            try {
                                device.launchDeepLink(
                                    url = deepLinkUrl,
                                    packageName = targetPackage.ifBlank { null }
                                )
                                if (deepLinkUrl !in urlHistory) {
                                    urlHistory = (listOf(deepLinkUrl) + urlHistory).take(10)
                                }
                            } catch (e: Exception) {
                                controller.showError("Error", e.message ?: "Failed to launch")
                            }
                        }
                    }
                },
                enabled = enabled && deepLinkUrl.isNotBlank()
            ) {
                Text("Launch", maxLines = 1)
            }
            
            OutlinedButton(
                onClick = {
                    coroutineScope.launch {
                        controller.selectedDevice?.let { device ->
                            try {
                                device.launchDeepLinkWithFlags(
                                    url = deepLinkUrl,
                                    packageName = targetPackage.ifBlank { null },
                                    clearTask = true,
                                    newTask = true
                                )
                            } catch (e: Exception) {
                                controller.showError("Error", e.message ?: "Failed")
                            }
                        }
                    }
                },
                enabled = enabled && deepLinkUrl.isNotBlank()
            ) {
                Text("+ Clear Task", maxLines = 1, fontSize = 10.sp)
            }
        }
        
        // URL History
        if (urlHistory.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Recent URLs", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            urlHistory.forEach { url ->
                Text(
                    text = url,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { deepLinkUrl = url }
                        .padding(vertical = 2.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Broadcast Section
        Text("Send Broadcast", fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        
        COMMON_BROADCASTS.take(10).forEach { broadcast ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        if (broadcast.name == selectedBroadcast) 
                            JewelTheme.globalColors.outlines.focused.copy(alpha = 0.2f)
                        else Color.Transparent
                    )
                    .clickable { selectedBroadcast = broadcast.name }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(broadcast.name, fontSize = 11.sp, maxLines = 1)
                    Text(broadcast.description, fontSize = 9.sp, maxLines = 1, 
                        color = JewelTheme.globalColors.text.info)
                }
                
                OutlinedButton(
                    onClick = {
                        coroutineScope.launch {
                            controller.selectedDevice?.let { device ->
                                try {
                                    device.sendCommonBroadcast(broadcast.name)
                                    controller.showWarning("Sent", broadcast.name)
                                } catch (e: Exception) {
                                    controller.showError("Error", e.message ?: "Failed")
                                }
                            }
                        }
                    },
                    enabled = enabled
                ) {
                    Text("Send", fontSize = 10.sp)
                }
            }
        }
    }
}

// ============================================================================
// Presets Tab
// ============================================================================

@Composable
fun PresetsTab() {
    val controller = LocalController.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    
    val presetsManager = remember { PresetsManager() }
    var presets by remember { mutableStateOf(presetsManager.getAllPresets()) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var newPresetName by remember { mutableStateOf("") }
    var newPresetDescription by remember { mutableStateOf("") }
    
    val presetNameState = remember { TextFieldState("") }
    val presetDescState = remember { TextFieldState("") }
    
    // Sync states
    LaunchedEffect(presetNameState) {
        snapshotFlow { presetNameState.text.toString() }
            .collect { newPresetName = it }
    }
    LaunchedEffect(presetDescState) {
        snapshotFlow { presetDescState.text.toString() }
            .collect { newPresetDescription = it }
    }
    
    val enabled = controller.selectedDevice != null
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        // Header with save button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Presets", fontWeight = FontWeight.Bold)
            OutlinedButton(
                onClick = { showSaveDialog = !showSaveDialog },
                enabled = enabled
            ) {
                Text(if (showSaveDialog) "Cancel" else "Save Current", fontSize = 10.sp)
            }
        }
        
        // Save preset form
        if (showSaveDialog) {
            Spacer(modifier = Modifier.height(8.dp))
            TextField(
                state = presetNameState,
                placeholder = { Text("Preset name") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(4.dp))
            TextField(
                state = presetDescState,
                placeholder = { Text("Description (optional)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(4.dp))
            DefaultButton(
                onClick = {
                    coroutineScope.launch {
                        controller.selectedDevice?.let { device ->
                            try {
                                val preset = controller.captureCurrentSettings(
                                    device, newPresetName, newPresetDescription
                                )
                                presetsManager.savePreset(preset)
                                presets = presetsManager.getAllPresets()
                                showSaveDialog = false
                                newPresetName = ""
                                newPresetDescription = ""
                                controller.showWarning("Saved", "Preset '$newPresetName' saved")
                            } catch (e: Exception) {
                                controller.showError("Error", e.message ?: "Failed to save")
                            }
                        }
                    }
                },
                enabled = newPresetName.isNotBlank()
            ) {
                Text("Save Preset")
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        // Preset list
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
        ) {
            presets.forEach { preset ->
                val isDefault = presetsManager.isDefaultPreset(preset.name)
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(JewelTheme.globalColors.outlines.focused.copy(alpha = 0.05f))
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(preset.name, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            if (isDefault) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "Built-in",
                                    fontSize = 9.sp,
                                    color = JewelTheme.globalColors.text.info
                                )
                            }
                        }
                        if (preset.description.isNotBlank()) {
                            Text(
                                preset.description,
                                fontSize = 10.sp,
                                color = JewelTheme.globalColors.text.info,
                                maxLines = 2
                            )
                        }
                        Text(
                            "${preset.settings.size} settings",
                            fontSize = 9.sp,
                            color = JewelTheme.globalColors.text.info
                        )
                    }
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        DefaultButton(
                            onClick = {
                                coroutineScope.launch {
                                    controller.selectedDevice?.let { device ->
                                        try {
                                            controller.applyPreset(device, preset)
                                            controller.showWarning("Applied", "Preset '${preset.name}' applied")
                                        } catch (e: Exception) {
                                            controller.showError("Error", e.message ?: "Failed")
                                        }
                                    }
                                }
                            },
                            enabled = enabled
                        ) {
                            Text("Apply", fontSize = 10.sp)
                        }
                        
                        if (!isDefault) {
                            OutlinedButton(
                                onClick = {
                                    presetsManager.deletePreset(preset.name)
                                    presets = presetsManager.getAllPresets()
                                }
                            ) {
                                Text("Delete", fontSize = 10.sp)
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

// ============================================================================
// Capture Tab
// ============================================================================

@Composable
fun CaptureTab() {
    val controller = LocalController.current
    val coroutineScope = rememberCoroutineScope()
    
    val recordingManager = remember { RecordingManager() }
    var isRecording by remember { mutableStateOf(false) }
    var recordingStartTime by remember { mutableStateOf(0L) }
    var recordingElapsedSeconds by remember { mutableStateOf(0) }
    var lastCapturePath by remember { mutableStateOf<String?>(null) }
    
    // Recording timer
    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordingStartTime = System.currentTimeMillis()
            while (isRecording) {
                recordingElapsedSeconds = ((System.currentTimeMillis() - recordingStartTime) / 1000).toInt()
                kotlinx.coroutines.delay(1000)
            }
        } else {
            recordingElapsedSeconds = 0
        }
    }
    
    val enabled = controller.selectedDevice != null
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Screenshot Section
        Text("Screenshot", fontWeight = FontWeight.Bold)
        
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DefaultButton(
                onClick = {
                    coroutineScope.launch {
                        controller.selectedDevice?.let { device ->
                            try {
                                val userHome = System.getProperty("user.home")
                                val filename = getScreenshotFilename()
                                val path = "$userHome/Desktop/$filename"
                                
                                if (device.takeScreenshot(path)) {
                                    lastCapturePath = path
                                    controller.showWarning("Screenshot", "Saved to $filename")
                                } else {
                                    controller.showError("Error", "Failed to capture screenshot")
                                }
                            } catch (e: Exception) {
                                controller.showError("Error", e.message ?: "Failed")
                            }
                        }
                    }
                },
                enabled = enabled
            ) {
                Text("Take Screenshot")
            }
            
            OutlinedButton(
                onClick = {
                    coroutineScope.launch {
                        controller.selectedDevice?.let { device ->
                            try {
                                val userHome = System.getProperty("user.home")
                                val filename = getScreenshotFilename("clean_screenshot")
                                val path = "$userHome/Desktop/$filename"
                                
                                if (device.takeCleanScreenshot(path)) {
                                    lastCapturePath = path
                                    controller.showWarning("Screenshot", "Clean screenshot saved to $filename")
                                } else {
                                    controller.showError("Error", "Failed to capture")
                                }
                            } catch (e: Exception) {
                                controller.showError("Error", e.message ?: "Failed")
                            }
                        }
                    }
                },
                enabled = enabled
            ) {
                Text("Clean (Demo Mode)", fontSize = 10.sp)
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Recording Section
        Text("Screen Recording", fontWeight = FontWeight.Bold)
        
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!isRecording) {
                DefaultButton(
                    onClick = {
                        coroutineScope.launch {
                            controller.selectedDevice?.let { device ->
                                try {
                                    if (recordingManager.startRecording(device)) {
                                        isRecording = true
                                        controller.showWarning("Recording", "Started recording (max 3 min)")
                                    }
                                } catch (e: Exception) {
                                    controller.showError("Error", e.message ?: "Failed")
                                }
                            }
                        }
                    },
                    enabled = enabled
                ) {
                    Text("Start Recording")
                }
            } else {
                DefaultButton(
                    onClick = {
                        coroutineScope.launch {
                            controller.selectedDevice?.let { device ->
                                try {
                                    val userHome = System.getProperty("user.home")
                                    val filename = getRecordingFilename()
                                    val path = "$userHome/Desktop/$filename"
                                    
                                    if (recordingManager.stopRecording(device, path)) {
                                        isRecording = false
                                        lastCapturePath = path
                                        controller.showWarning("Recording", "Saved to $filename")
                                    }
                                } catch (e: Exception) {
                                    controller.showError("Error", e.message ?: "Failed")
                                    isRecording = false
                                }
                            }
                        }
                    },
                    enabled = enabled
                ) {
                    Text("Stop Recording")
                }
                
                OutlinedButton(
                    onClick = {
                        coroutineScope.launch {
                            controller.selectedDevice?.let { device ->
                                recordingManager.cancelRecording(device)
                                isRecording = false
                            }
                        }
                    }
                ) {
                    Text("Cancel")
                }
            }
        }
        
        if (isRecording) {
            val minutes = recordingElapsedSeconds / 60
            val seconds = recordingElapsedSeconds % 60
            val timeString = String.format("%02d:%02d", minutes, seconds)
            val remainingSeconds = 180 - recordingElapsedSeconds
            val remainingMinutes = remainingSeconds / 60
            val remainingSecs = remainingSeconds % 60
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "🔴 Recording: $timeString",
                    color = Color(0xFFE53935),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "(${String.format("%d:%02d", remainingMinutes, remainingSecs)} remaining)",
                    color = JewelTheme.globalColors.text.info,
                    fontSize = 10.sp
                )
            }
        }
        
        // Last capture info
        lastCapturePath?.let { path ->
            Spacer(modifier = Modifier.height(8.dp))
            Text("Last capture:", fontSize = 10.sp, color = JewelTheme.globalColors.text.info)
            Text(path, fontSize = 10.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Device info
        Text("Device Info", fontWeight = FontWeight.Bold)
        var deviceInfo by remember { mutableStateOf<DeviceInfo?>(null) }
        
        LaunchedEffect(controller.selectedDevice) {
            controller.selectedDevice?.let { device ->
                try {
                    deviceInfo = device.getDeviceInfo()
                } catch (e: Exception) {
                    deviceInfo = null
                }
            }
        }
        
        deviceInfo?.let { info ->
            Column {
                Text("${info.manufacturer} ${info.model}", fontSize = 11.sp)
                Text("Android ${info.androidVersion} (API ${info.apiLevel})", fontSize = 10.sp)
                Text("${info.screenResolution} @ ${info.screenDensity}dpi", fontSize = 10.sp)
            }
        }
    }
}