package com.tinkrmux.devswitch

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory

class AndroidQuickSettingsWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val content = toolWindow.contentManager.factory.createContent(
            createQuickSettingsUI(),
            null,
            false
        )
        toolWindow.contentManager.addContent(content)
    }
}