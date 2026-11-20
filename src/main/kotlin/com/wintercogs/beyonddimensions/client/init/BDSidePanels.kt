package com.wintercogs.beyonddimensions.client.init

import com.wintercogs.beyonddimensions.api.ui.storage.StorageSidePanels
import com.wintercogs.beyonddimensions.client.ui.storage.CategoryEditorSidePanel

/** BD 自带的存储界面右侧展开面板，在客户端初始化时注册 */
object BDSidePanels {
    @JvmStatic
    fun register() {
        StorageSidePanels.register(StorageSidePanels.CATEGORY_EDITOR, CategoryEditorSidePanel)
    }
}
