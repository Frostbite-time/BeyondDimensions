package com.wintercogs.beyonddimensions.client.ui.device

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wintercogs.beyonddimensions.client.ui.base.BdController
import com.wintercogs.beyonddimensions.client.ui.base.BdIcons
import com.wintercogs.beyonddimensions.client.ui.base.BdInventoryScreen
import com.wintercogs.beyonddimensions.client.ui.base.BdThemes
import com.wintercogs.beyonddimensions.client.ui.base.at
import com.wintercogs.beyonddimensions.client.ui.base.endAt
import com.wintercogs.beyonddimensions.client.ui.base.slotAt
import com.wintercogs.beyonddimensions.client.ui.base.tr
import com.wintercogs.beyonddimensions.client.ui.storage.PlayerInventory
import com.wintercogs.beyonddimensions.common.menu.BDBaseMenu
import dev.compixel.forge.item.ItemIcon
import dev.compixel.forge.item.MinecraftItemIcon
import dev.compixel.forge.slots.ComposeMenuSlots
import dev.compixel.host.UiBinding
import dev.compixel.ui.ore.button.OreIconButton
import dev.compixel.ui.ore.display.OreText
import dev.compixel.ui.ore.layout.OrePanel
import dev.compixel.ui.ore.theme.OreTheme
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory

/**
 * 设备界面上的一个模式按钮，与旧版相同：按固定顺序循环切换，每个状态有自己的图标与提示。
 * [get] 与 [set] 读写方块实体或物品上的设置，切换后由菜单把设置同步给服务端。
 */
class ModeTab<T : Any>(
    private val states: List<T>,
    private val icon: (T) -> String,
    private val tooltip: (T) -> String,
    private val get: () -> T,
    private val set: (T) -> Unit,
    /** 为 false 时按钮不可用，提示改为 [unavailable] */
    private val available: () -> Boolean = { true },
    private val unavailable: String? = null,
    /** 放在面板左侧（旧版的范围按钮） */
    val left: Boolean = false,
    /** 在侧边的第几格；旧版每格相隔 30 */
    val row: Int = -1,
) {
    fun view(row: Int): TabView {
        val state = get()
        val usable = available()
        val text = if (!usable && unavailable != null) tr(unavailable) else tr(tooltip(state))
        return TabView(BdIcons.sprite(icon(state)), text, usable, left, row)
    }

    fun cycle() {
        if (!available()) return
        set(states[(states.indexOf(get()) + 1).mod(states.size)])
    }
}

data class TabView(val icon: ItemIcon, val tooltip: String, val enabled: Boolean, val left: Boolean, val row: Int)

data class DeviceState(val tabs: List<TabView>, val extra: Any? = null)

/**
 * 设备界面的游戏线程逻辑：发布各模式按钮的当前状态，点击后切换并同步给服务端。
 * [extra] 发布界面自己的内容（能量、目标等级等），[custom] 处理这些内容发回的操作。
 */
class DeviceController<M : BDBaseMenu>(
    private val menu: M,
    private val tabs: List<ModeTab<*>>,
    private val extra: () -> Any? = { null },
    private val custom: (Any) -> Unit = {},
) : BdController<DeviceState, Any>(DeviceState(emptyList())) {
    init {
        ui.update(snapshot())
    }

    override fun snapshot(): DeviceState {
        var right = 0
        return DeviceState(
            tabs.map { tab -> tab.view(if (tab.row >= 0) tab.row else if (tab.left) 0 else right++) },
            extra(),
        )
    }

    override fun handle(action: Any) {
        if (action !is Int) return custom(action)
        val tab = tabs.getOrNull(action) ?: return
        tab.cycle()
        menu.writeAndSendQuickData()
    }
}

/** 槽位编号与旧版界面里的坐标 */
class SlotPlace(val id: Int, val x: Int, val y: Int)

/**
 * 设备界面，布局与旧版一致：标题与右侧的说明文字，设备的槽位（按旧版坐标排布）或自定义内容，下方是玩家背包；
 * 模式按钮排在面板右侧，范围按钮在左侧。全部内容按旧版界面坐标绝对定位，面板尺寸由菜单里背包槽位的位置决定。
 */
open class DeviceScreen<M : BDBaseMenu>
protected constructor(
    menu: M,
    title: Component,
    controller: DeviceController<M>,
    labels: DeviceLabels,
    body: @Composable (ComposeMenuSlots<M>, DeviceState, (Any) -> Unit) -> Unit,
) : BdInventoryScreen<M>(menu, title, controller, { slots -> DeviceView(slots, controller.ui, labels, body) }) {
    /** 设备槽位按旧版坐标排布的普通设备界面 */
    constructor(
        menu: M,
        inventory: Inventory,
        title: Component,
        section: String?,
        tabs: List<ModeTab<*>>,
    ) : this(menu, title, DeviceController(menu, tabs), deviceLabels(menu, inventory, title, section), deviceSlots(menu))

    companion object {
        /** [titleY] 是旧版标题的 y：多数设备界面为 8，沿用原版默认值的界面为 6 */
        fun deviceLabels(menu: BDBaseMenu, inventory: Inventory, title: Component, section: String?, titleY: Int = 8) =
            DeviceLabels(
                title.string,
                section?.let { tr(it) },
                inventory.displayName.string,
                (menu.inventoryStartIndex until menu.inventoryEndIndex).toList(),
                menu.slots[menu.inventoryStartIndex].y,
                titleY,
            )

        fun places(menu: BDBaseMenu) =
            menu.slots
                .filter { it.index < menu.inventoryStartIndex || it.index >= menu.inventoryEndIndex }
                .map { SlotPlace(it.index, it.x, it.y) }

        /** 除玩家背包外的全部槽位，按旧版坐标排布 */
        fun <M : BDBaseMenu> deviceSlots(menu: M): @Composable (ComposeMenuSlots<M>, DeviceState, (Any) -> Unit) -> Unit {
            val places = places(menu)
            return { slots, _, _ -> SlotLayout(slots, places) }
        }
    }
}

/**
 * [playerTop] 是旧版背包第一行槽位的 y。旧版的背包标题在它上方 11，面板底边在它下方 82（快捷栏下留 7），
 * 各设备界面都遵循这一布局，因此面板高度与背包位置都由它得出。
 */
class DeviceLabels(
    val title: String,
    val section: String?,
    val inventory: String,
    val player: List<Int>,
    val playerTop: Int,
    val titleY: Int,
)

private const val WIDTH = 176

@Composable
private fun <M : BDBaseMenu> DeviceView(
    slots: ComposeMenuSlots<M>,
    binding: UiBinding<DeviceState, Any>,
    labels: DeviceLabels,
    body: @Composable (ComposeMenuSlots<M>, DeviceState, (Any) -> Unit) -> Unit,
) {
    val state = binding.value
    Box(Modifier.fillMaxSize().background(OreTheme.colors.backdrop), contentAlignment = Alignment.Center) {
        Box {
            OrePanel(
                "",
                Modifier.size(WIDTH.dp, (labels.playerTop + 82).dp).then(slots.areaModifier()),
                showTitleBar = false,
                contentPadding = PaddingValues(0.dp),
            ) {
                Box(Modifier.fillMaxSize()) {
                    OreText(labels.title, Modifier.at(8, labels.titleY), maxLines = 1)
                    if (labels.section != null) OreText(labels.section, Modifier.endAt(WIDTH - 6, labels.titleY + 3), maxLines = 1)
                    body(slots, state) { binding.send(it) }
                    OreText(labels.inventory, Modifier.at(8, labels.playerTop - 11), maxLines = 1)
                    Box(Modifier.slotAt(8, labels.playerTop)) { PlayerInventory(slots, labels.player) }
                }
            }
            Tabs(state.tabs.withIndex().filter { !it.value.left }, binding, Modifier.offset(x = (WIDTH - 2).dp, y = 6.dp))
            Tabs(state.tabs.withIndex().filter { it.value.left }, binding, Modifier.offset(x = (-22).dp, y = 6.dp))
        }
    }
}

/** 模式按钮：与旧版一样每格相隔 30 */
@Composable
internal fun Tabs(tabs: List<IndexedValue<TabView>>, binding: UiBinding<DeviceState, Any>, modifier: Modifier) {
    if (tabs.isEmpty()) return
    OreTheme(id = BdThemes.Tab) {
        Box(modifier) {
            for ((index, tab) in tabs)
                OreIconButton(tab.tooltip, { binding.send(index) }, Modifier.offset(y = (tab.row * 30).dp).size(24.dp), tab.enabled) {
                    MinecraftItemIcon(tab.icon, Modifier.size(16.dp))
                }
        }
    }
}

/** 按旧版坐标排布槽位 */
@Composable
fun SlotLayout(slots: ComposeMenuSlots<*>, places: List<SlotPlace>) {
    OreTheme(id = BdThemes.Grid) { for (place in places) slots.Slot(place.id, Modifier.slotAt(place.x, place.y)) }
}
