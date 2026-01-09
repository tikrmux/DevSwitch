package com.tinkrmux.devswitch

import androidx.compose.runtime.Composable
import com.intellij.openapi.project.Project
import org.jetbrains.jewel.bridge.theme.SwingBridgeTheme
import org.jetbrains.jewel.foundation.ExperimentalJewelApi

/**
 * DevSwitch theme wrapper using Jewel's SwingBridgeTheme.
 * This automatically picks up IntelliJ's current theme (light/dark).
 */
@OptIn(ExperimentalJewelApi::class)
@Composable
fun DevSwitchTheme(
    content: @Composable () -> Unit
) {
    SwingBridgeTheme {
        content()
    }
}
