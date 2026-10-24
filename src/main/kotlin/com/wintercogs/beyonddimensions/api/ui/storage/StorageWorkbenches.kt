package com.wintercogs.beyonddimensions.api.ui.storage

import com.wintercogs.beyonddimensions.api.menu.workbench.StorageWorkbenchTypes
import net.minecraft.resources.ResourceLocation

/**
 * 存储界面工作区里工作台的界面，以菜单一侧注册的 ID 对应
 * [StorageWorkbenchTypes]：那边决定玩家能否使用、菜单里有哪些槽位，这里决定怎么画。
 * 下拉框按菜单一侧的注册顺序列出可用的工作台。在客户端的模组构造函数或初始化事件里注册：
 * ```
 * StorageWorkbenches.register(ResourceLocation.fromNamespaceAndPath("example", "loom"), LoomWorkbench)
 * ```
 */
object StorageWorkbenches {
    /** 工作台内容的最大宽度，与 9 列存储格的内容区一样宽 */
    const val MAX_WIDTH = 173

    /** 工作台内容的最大高度，比 4 行槽位稍高 */
    const val MAX_HEIGHT = 76

    @JvmField
    val CRAFTING: ResourceLocation = StorageWorkbenchTypes.CRAFTING

    @JvmField
    val ANVIL: ResourceLocation = StorageWorkbenchTypes.ANVIL

    @JvmField
    val SMITHING: ResourceLocation = StorageWorkbenchTypes.SMITHING

    @JvmField
    val STONECUTTER: ResourceLocation = StorageWorkbenchTypes.STONECUTTER

    @Volatile
    private var entries = emptyMap<ResourceLocation, StorageWorkbench<*, *>>()

    /** 注册工作台 [id] 的界面 */
    @JvmStatic
    @Synchronized
    fun register(id: ResourceLocation, workbench: StorageWorkbench<*, *>) {
        check(id !in entries) { "Storage workbench UI $id is already registered" }
        entries = entries + (id to workbench)
    }

    /** 工作台 [id] 的界面，没有注册时为 null */
    @JvmStatic
    fun get(id: ResourceLocation): StorageWorkbench<*, *>? = entries[id]
}
