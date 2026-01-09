package com.tinkrmux.devswitch

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import org.jetbrains.jewel.bridge.addComposeTab

class AndroidQuickSettingsWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        toolWindow.addComposeTab("DevSwitch") {
            QuickSettingsPlugin()
        }
    }
}