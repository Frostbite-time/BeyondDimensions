package com.wintercogs.beyonddimensions.client.ui.storage

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.wintercogs.beyonddimensions.api.ui.storage.StorageSidePanel
import com.wintercogs.beyonddimensions.api.ui.storage.StorageSidePanelContext
import com.wintercogs.beyonddimensions.api.ui.storage.StorageSidePanelScope
import com.wintercogs.beyonddimensions.api.ui.storage.StorageSidePanels
import dev.compixel.host.gameState

/** BD 内置的分类编辑侧边面板，在 [com.wintercogs.beyonddimensions.client.init.BDSidePanels] 注册 */
internal object CategoryEditorSidePanel : StorageSidePanel {
    override fun width(context: StorageSidePanelContext): Int = StorageSidePanels.DEFAULT_WIDTH

    override fun onClose(context: StorageSidePanelContext) {
        (context.screen as? StorageScreen)?.onCategoryEditorPanelClosed()
    }

    @Composable
    override fun ColumnScope.Content(context: StorageSidePanelContext, scope: StorageSidePanelScope) {
        val screen = context.screen as? StorageScreen ?: return
        val controller = screen.controller
        val text = screen.layout.text
        val currentEditor by gameState { controller.editorState() }
        // 收起动画期间保留最后一次编辑状态，避免内容突然变空
        val lastEditor = remember { arrayOfNulls<CategoryEditor>(1) }
        currentEditor?.let { lastEditor[0] = it }
        val editor = lastEditor[0] ?: return
        val act: StorageAct = { block -> scope.gameThread.post { controller.block() } }
        androidx.compose.runtime.key(editor.index) {
            CategoryEditorPanel(editor, act, text, enabled = scope.shown && currentEditor != null)
        }
    }
}
