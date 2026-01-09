package com.tinkrmux.devswitch.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.intellij.ide.util.PropertiesComponent
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.*

// ============================================================================
// Main Tabs Definition
// ============================================================================

enum class MainTab(val displayName: String) {
    SETTINGS("Settings"),
    APP_MANAGER("App Manager"),
    DEEP_LINKS("Deep Links"),
    PRESETS("Presets"),
    CAPTURE("Capture")
}

// ============================================================================
// Setting Categories & Items
// ============================================================================

enum class SettingCategory(val displayName: String) {
    NETWORK("Network"),
    DEBUG("Debug Overlays"),
    DEVELOPER("Developer Options"),
    DISPLAY("Display & Accessibility"),
    PERFORMANCE("Performance"),
    BATTERY("Battery & Power")
}

data class SettingItem(
    val id: String,
    val name: String,
    val category: SettingCategory,
    val shortcutKey: Key? = null,
    val isRange: Boolean = false
)

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

// ============================================================================
// Persistence Keys
// ============================================================================

const val COLLAPSED_CATEGORIES_KEY = "com.tinkrmux.devswitch.collapsedCategories"
const val SELECTED_TAB_KEY = "com.tinkrmux.devswitch.selectedTab"
const val AUTO_REFRESH_KEY = "com.tinkrmux.devswitch.autoRefresh"

// ============================================================================
// Common UI Components
// ============================================================================

@Composable
fun TabBar(selectedTab: MainTab, onTabSelected: (MainTab) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        MainTab.entries.forEach { tab ->
            val isSelected = tab == selectedTab
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        if (isSelected) JewelTheme.globalColors.outlines.focused
                        else Color.Transparent
                    )
                    .clickable { onTabSelected(tab) }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = tab.displayName,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) 
                        JewelTheme.globalColors.text.normal 
                    else 
                        JewelTheme.globalColors.text.normal.copy(alpha = 0.6f)
                )
            }
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
    
    TextField(
        state = textFieldState,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        placeholder = { Text("Search settings... (Ctrl+K)") }
    )
}

@Composable
fun DeviceStatusIndicator(isOnline: Boolean) {
    Box(
        modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(if (isOnline) Color(0xFF4CAF50) else Color(0xFFFF5722))
    )
}

@Composable
fun CategoryHeader(
    category: SettingCategory,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    settingsCount: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = if (isExpanded) "▼" else "►",
                fontSize = 10.sp,
                color = JewelTheme.globalColors.text.normal.copy(alpha = 0.6f)
            )
            Text(
                text = category.displayName,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
        }
        Text(
            text = "$settingsCount items",
            fontSize = 11.sp,
            color = JewelTheme.globalColors.text.normal.copy(alpha = 0.5f)
        )
    }
}

@Composable
fun ErrorBanner(title: String, message: String, onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFFCDD2))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color(0xFFB71C1C)
                )
                Text(
                    text = message,
                    fontSize = 11.sp,
                    color = Color(0xFFC62828),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(24.dp)
            ) {
                Text("✕", fontSize = 12.sp, color = Color(0xFFB71C1C))
            }
        }
    }
}

@Composable
fun LoadingIndicator(text: String = "Loading...") {
    Row(
        modifier = Modifier.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CircularProgressIndicator(modifier = Modifier.size(16.dp))
        Text(text, fontSize = 12.sp)
    }
}

@Composable
fun EmptyStateMessage(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            fontSize = 13.sp,
            color = JewelTheme.globalColors.text.normal.copy(alpha = 0.6f)
        )
    }
}

@Composable
fun SectionDivider() {
    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(JewelTheme.globalColors.borders.normal.copy(alpha = 0.3f))
    )
}

@Composable
fun ShortcutBadge(key: Key) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(3.dp))
            .background(JewelTheme.globalColors.outlines.focused)
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Text(
            text = "Alt+${key.toString().removePrefix("Key: ")}",
            fontSize = 9.sp,
            color = JewelTheme.globalColors.text.normal.copy(alpha = 0.6f)
        )
    }
}
