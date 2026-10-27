package com.wintercogs.beyonddimensions.client.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.wintercogs.beyonddimensions.client.ui.base.Legend
import com.wintercogs.beyonddimensions.client.ui.base.SLOT_PITCH
import com.wintercogs.beyonddimensions.client.ui.base.tr
import com.wintercogs.beyonddimensions.client.ui.kit.*
import com.wintercogs.beyonddimensions.client.ui.machine.CookConduit
import com.wintercogs.beyonddimensions.client.ui.machine.Flames
import com.wintercogs.beyonddimensions.common.init.BDItems
import com.wintercogs.beyonddimensions.common.machine.setting.SideMode
import dev.compixel.forge.item.ItemIcon
import dev.compixel.forge.item.MinecraftItemIcon
import dev.compixel.ui.ore.display.OreText
import dev.compixel.ui.ore.inventory.OreSlot
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

/** 颜色编辑器里预览用的物品图标与文字，打开编辑器时在游戏线程取出 */
internal class BdPreviewContent {
    val icon = ItemIcon.snapshot(ItemStack(BDItems.NET_TERMINAL_ITEM.get()))
    val items =
        listOf(
            Items.DIAMOND,
            Items.IRON_INGOT,
            Items.OAK_LOG,
            Items.REDSTONE,
            Items.GOLD_INGOT,
            Items.EMERALD,
            Items.COBBLESTONE,
            Items.LAPIS_LAZULI,
        ).map { ItemIcon.snapshot(ItemStack(it)) }
    val title = tr("ui.beyonddimensions.color_preview")
    val storage = tr("ui.beyonddimensions.storage.storage")
    val crafting = tr("ui.beyonddimensions.storage.crafting")
    val settings = tr("ui.beyonddimensions.color_preview.settings")
    val search = tr("ui.beyonddimensions.storage.search")
    val stored = tr("ui.beyonddimensions.color_preview.items")
    val body = tr("ui.beyonddimensions.color_preview.body")
    val muted = tr("ui.beyonddimensions.color_preview.muted")
    val faint = tr("ui.beyonddimensions.color_preview.faint")
    val online = tr("ui.beyonddimensions.color_preview.online")
    val danger = tr("ui.beyonddimensions.color_preview.danger")
    val setting = tr("ui.beyonddimensions.color_preview.setting")
    val settingHint = tr("ui.beyonddimensions.color_preview.setting.hint")
    val chip = tr("ui.beyonddimensions.color_preview.chip")
    val tag = tr("ui.beyonddimensions.color_preview.tag")
    val popover = tr("ui.beyonddimensions.color_preview.popover")
    val popoverHint = tr("ui.beyonddimensions.color_preview.popover.hint")

    // 分段按钮与面配置的图例借用界面里现成的文字
    val filters = listOf("ignore", "white", "black").map { tr("ui.beyonddimensions.machine.filter.$it") }
    val sides =
        listOf(SideMode.INPUT, SideMode.OUTPUT, SideMode.BOTH).associateWith {
            tr("ui.beyonddimensions.side.mode.${it.name.lowercase()}")
        }
}

/**
 * 颜色编辑器旁的预览：一个缩小的 BD 窗口，由界面里真实的控件组成，每种颜色都有地方显示。
 * 窗口四周露出背景遮罩；槽位有普通、悬停、标记、标记且悬停与空格，输入框是悬停时的样子，里面选中了一段文字
 */
@Composable
internal fun BdPreview(content: BdPreviewContent) {
    val colors = Bd.colors
    val items = content.items
    var off by remember { mutableStateOf(false) }
    var on by remember { mutableStateOf(true) }
    var filter by remember { mutableIntStateOf(1) }
    var amount by remember { mutableLongStateOf(64) }
    Box(Modifier.background(colors[BdColors.backdrop]).padding(6.dp)) {
        BdWindow(Modifier.width(200.dp)) {
            BdHeader(content.icon, content.title, {}, tag = "#0001")
            Box {
                Column(
                    Modifier.fillMaxWidth()
                        .padding(start = SIDE_RAIL_WIDTH.dp)
                        .bdScreen(
                            colors,
                            start = SCREEN_INSET_X.dp,
                            top = SCREEN_INSET_TOP.dp,
                            end = SCREEN_INSET_X.dp,
                            bottom = SCREEN_INSET_BOTTOM.dp,
                        )
                        .padding(start = 7.dp, top = 5.dp, end = 7.dp, bottom = 7.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    BdSearchField("", {}, content.search, Modifier.fillMaxWidth())
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        BdSectionLabel(content.stored)
                        Row {
                            Box(Modifier.bdSlotFrame(colors)) {
                                Column {
                                    Row {
                                        PreviewSlot(items[0], "64")
                                        PreviewSlot(items[1], "1.2K", hovered = true)
                                        PreviewSlot(items[2], "", marked = true)
                                        PreviewSlot(items[3], "9", marked = true, hovered = true)
                                        PreviewSlot(null, "")
                                        PreviewSlot(items[4], "3")
                                    }
                                    Row {
                                        PreviewSlot(items[5], "512")
                                        PreviewSlot(items[6], "8K")
                                        PreviewSlot(items[7], "27")
                                        repeat(3) { PreviewSlot(null, "") }
                                    }
                                }
                            }
                            Spacer(Modifier.width(2.dp))
                            BdScrollbar(
                                0,
                                2,
                                5,
                                {},
                                Modifier.width(SCROLLBAR_WIDTH.dp).height((2 * SLOT_PITCH + 1).dp),
                            )
                        }
                    }
                    Column {
                        OreText(content.body, color = colors[BdColors.text], maxLines = 1)
                        OreText(content.muted, color = colors[BdColors.muted], style = Bd.caption, maxLines = 1)
                        OreText(content.faint, color = colors[BdColors.faint], style = Bd.caption, maxLines = 1)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Status(colors[BdColors.online], content.online)
                        Spacer(Modifier.width(6.dp))
                        Status(colors[BdColors.danger], content.danger)
                    }
                    BdSettingRow(content.setting, content.settingHint) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            BdToggle(off, { off = it })
                            Spacer(Modifier.width(3.dp))
                            BdToggle(on, { on = it })
                        }
                    }
                    BdSegmented(content.filters, filter, { filter = it }, Modifier.fillMaxWidth(), fill = true)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BdNumberEditor(amount, { amount = it }, 0L..999L)
                        Spacer(Modifier.width(4.dp))
                        Row(
                            Modifier.weight(1f)
                                .height(FIELD_HEIGHT.dp)
                                .bdInset(colors, edge = colors[BdColors.lineStrong])
                                .padding(horizontal = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            OreText(
                                content.body.take(2),
                                Modifier.background(colors[BdColors.selection]),
                                color = colors[BdColors.text],
                                maxLines = 1,
                            )
                            OreText(content.body.drop(2), color = colors[BdColors.text], maxLines = 1)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BdChip({}, active = true) { OreText(content.chip, color = colors[BdColors.text], maxLines = 1) }
                        Spacer(Modifier.width(4.dp))
                        Box(Modifier.background(colors[BdColors.accentDeep], Bd.ChipShape).padding(horizontal = 3.dp)) {
                            OreText(content.tag, color = colors[BdColors.onAccent], style = Bd.caption, maxLines = 1)
                        }
                        Spacer(Modifier.weight(1f))
                        CookConduit(0.85f, null)
                        Flames(1f, 0, null)
                    }
                    Legend(content.sides)
                    Column(
                        Modifier.fillMaxWidth()
                            .bdRaised(colors, fill = colors[BdColors.popover])
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        OreText(content.popover, color = colors[BdColors.text], style = Bd.caption, maxLines = 1)
                        OreText(content.popoverHint, color = colors[BdColors.muted], style = Bd.caption, maxLines = 1)
                    }
                }
                // 竖条叠在屏幕左侧的机壳上，高度跟随屏幕；选中的页签接进屏幕
                Box(Modifier.matchParentSize()) {
                    Column(
                        Modifier.width(SIDE_RAIL_WIDTH.dp).fillMaxHeight().padding(vertical = 6.dp),
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        BdRailTab(content.storage, BdIcons.Main, selected = true) {}
                        BdRailTab(content.crafting, BdIcons.Crafting, selected = false) {}
                        Spacer(Modifier.weight(1f))
                        BdRailTab(content.settings, BdIcons.Settings, selected = false) {}
                    }
                }
            }
        }
    }
}

/** 与存储格子里的虚拟槽位画法相同 */
@Composable
private fun PreviewSlot(icon: ItemIcon?, amount: String, marked: Boolean = false, hovered: Boolean = false) {
    val colors = Bd.colors
    Box(Modifier.size(SLOT_PITCH.dp)) {
        OreSlot(Modifier.matchParentSize(), contentModifier = Modifier.size(16.dp)) {
            if (marked) Box(Modifier.matchParentSize().background(colors[BdColors.accentSoft]))
            icon?.let { MinecraftItemIcon(it, Modifier.fillMaxSize()) }
        }
        if (amount.isNotEmpty())
            BdAmountPill(amount, Modifier.align(Alignment.BottomEnd).padding(end = 0.5.dp, bottom = 0.5.dp))
        if (hovered) Box(Modifier.matchParentSize().brackets(colors[BdColors.marker], arm = 4.dp))
    }
}

@Composable
private fun Status(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(4.dp).background(color))
        Spacer(Modifier.width(3.dp))
        OreText(label, color = color, style = Bd.caption, maxLines = 1)
    }
}
