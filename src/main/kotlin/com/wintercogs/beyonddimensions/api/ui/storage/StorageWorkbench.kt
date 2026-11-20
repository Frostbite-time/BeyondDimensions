package com.wintercogs.beyonddimensions.api.ui.storage

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wintercogs.beyonddimensions.client.ui.base.tr
import com.wintercogs.beyonddimensions.client.ui.kit.BdGlyphButton
import com.wintercogs.beyonddimensions.client.ui.kit.BdIcons
import com.wintercogs.beyonddimensions.common.menu.DimensionsNetMenu
import dev.compixel.forge.slots.ComposeMenuSlots
import dev.compixel.forge.sync.MenuAction
import dev.compixel.host.GameThread
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.player.LocalPlayer
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation

/**
 * 工作台内容的大小，单位 dp，不含工作区顶上下拉框的一行；
 * 宽高超过 [StorageWorkbenches.MAX_WIDTH]、[StorageWorkbenches.MAX_HEIGHT] 的部分会被裁掉
 */
data class StorageWorkbenchSize(val width: Int, val height: Int)

/**
 * 存储界面工作区里一个工作台的界面，用 [StorageWorkbenches.register] 注册。工作区一次显示一个工作台，
 * 玩家在工作区左上角的下拉框里选择；工作区比工作台大时，工作台在其中居中。
 * 下拉框只列出这次打开时玩家能用的工作台，由菜单一侧的
 * [com.wintercogs.beyonddimensions.api.menu.workbench.StorageWorkbenchType] 在服务端判断。
 *
 * 工作台与存储界面一起运行在游戏线程。会变化的游戏数据在 [Actions] 与 [Content] 中通过 `gameState { ... }` 读取
 * （返回不可变值，不要放菜单、物品堆等游戏对象）；修改游戏状态或向服务端发请求的操作通过
 * [StorageWorkbenchScope.gameThread] 提交。
 * 工作台的槽位来自菜单一侧的部件，界面用 `BdSlot` 画出它们。部件放着玩家的物品
 * （[com.wintercogs.beyonddimensions.api.menu.widget.ClearableWidget]）时，下拉框一行的末端还有一个退回方向按钮，
 * 所有工作台共用这个设置
 */
interface StorageWorkbench {
    /** 下拉框里的名称；打开界面时取一次 */
    fun title(context: StorageWorkbenchContext): Component

    /** 内容的大小；打开界面时取一次。工作区取所有工作台里最大的宽高，切换时不变大小 */
    fun size(context: StorageWorkbenchContext): StorageWorkbenchSize

    /** 下拉框所在一行右端的按钮，高 11 dp，例如 [WorkbenchClearButtons]；退回方向按钮在它们右边 */
    @Composable
    fun Actions(context: StorageWorkbenchContext, scope: StorageWorkbenchScope) {
    }

    /** 工作台的内容，按 [size] 的大小组合；可以直接使用 BD 的界面组件 */
    @Composable
    fun Content(context: StorageWorkbenchContext, scope: StorageWorkbenchScope)
}

/** 工作台访问存储界面与菜单的上下文，每个工作台一份 */
class StorageWorkbenchContext internal constructor(
    /** 工作台的 ID */
    val id: ResourceLocation,
    /** 存储界面的菜单（客户端一侧） */
    val menu: DimensionsNetMenu,
    /** 界面本身 */
    val screen: Screen,
    private val close: () -> Unit,
) {
    private val values = HashMap<Any, Any?>()

    val minecraft: Minecraft
        get() = Minecraft.getInstance()

    val player: LocalPlayer
        get() = checkNotNull(minecraft.player)

    /** 关闭界面 */
    fun requestClose() = close()

    /** 本工作台在这个界面里按 [key] 保存的值，第一次取时由 [create] 生成；界面关闭后丢弃 */
    @Suppress("UNCHECKED_CAST")
    fun <T> remember(key: Any, create: () -> T): T = values.getOrPut(key, create) as T

    /** 把注册在 [com.wintercogs.beyonddimensions.api.menu.BdMenuActions] 或菜单部件里的动作送到服务器；返回 false 表示没有发出 */
    fun <V> request(action: MenuAction<in DimensionsNetMenu, V>, value: V): Boolean {
        val sync = menu.menuSync()
        return sync.hasSnapshot() && sync.request(action, value).queued()
    }

    /**
     * 请服务器把这个工作台里的物品清回存储（[toStorage]）或背包；
     * 它的部件要实现 [com.wintercogs.beyonddimensions.api.menu.widget.ClearableWidget]
     */
    fun clearItems(toStorage: Boolean) = menu.commands().clearWorkbench(id, toStorage)
}

/** 工作台在 [StorageWorkbench.Actions] 与 [StorageWorkbench.Content] 中可以使用的宿主能力 */
interface StorageWorkbenchScope {
    /** 界面的槽位。用 `BdSlot(slots, id)` 画出菜单的槽位 */
    val slots: ComposeMenuSlots<*>

    /** 宿主界面的游戏线程任务入口，用于在 UI 回调中提交修改游戏状态或服务端请求的操作 */
    val gameThread: GameThread

    /** 关闭界面 */
    fun requestClose()
}

/** [WorkbenchClearButtons] 的提示文字 */
class WorkbenchClearText {
    val toStorage = tr("ui.beyonddimensions.storage.workbench.clear_to_storage")
    val toInventory = tr("ui.beyonddimensions.storage.workbench.clear_to_inventory")
}

/**
 * 放在 [StorageWorkbench.Actions] 里的两个按钮：把工作台里的物品清回存储或背包。
 * [onClear] 收到 true 时清回存储，通常通过 [StorageWorkbenchScope.gameThread] 调用
 * [StorageWorkbenchContext.clearItems]
 */
@Composable
fun WorkbenchClearButtons(text: WorkbenchClearText, onClear: (toStorage: Boolean) -> Unit) {
    BdGlyphButton(BdIcons.ToStorage, text.toStorage, { onClear(true) }, size = 11.dp)
    Spacer(Modifier.width(2.dp))
    BdGlyphButton(BdIcons.ToInventory, text.toInventory, { onClear(false) }, size = 11.dp)
}
