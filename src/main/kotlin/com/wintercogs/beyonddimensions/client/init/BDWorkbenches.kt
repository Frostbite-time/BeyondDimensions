package com.wintercogs.beyonddimensions.client.init

import com.wintercogs.beyonddimensions.api.ui.storage.StorageWorkbenches
import com.wintercogs.beyonddimensions.client.ui.storage.AnvilWorkbench
import com.wintercogs.beyonddimensions.client.ui.storage.CraftingWorkbench
import com.wintercogs.beyonddimensions.client.ui.storage.SmithingWorkbench
import com.wintercogs.beyonddimensions.client.ui.storage.StonecutterWorkbench

/**
 * BD 自带工作台在存储界面工作区里的界面，按下拉框的顺序注册：合成台、铁砧、锻造台、切石机。
 * 它们在菜单里的部件在通用的 BDWorkbenchWidgets 里注册
 */
object BDWorkbenches {
    @JvmStatic
    fun register() {
        StorageWorkbenches.register(StorageWorkbenches.CRAFTING, CraftingWorkbench)
        StorageWorkbenches.register(StorageWorkbenches.ANVIL, AnvilWorkbench)
        StorageWorkbenches.register(StorageWorkbenches.SMITHING, SmithingWorkbench)
        StorageWorkbenches.register(StorageWorkbenches.STONECUTTER, StonecutterWorkbench)
    }
}
