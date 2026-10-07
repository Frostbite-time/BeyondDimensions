package com.wintercogs.beyonddimensions.client.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.wintercogs.beyonddimensions.client.ui.base.SLOT_PITCH
import com.wintercogs.beyonddimensions.client.ui.base.tr
import com.wintercogs.beyonddimensions.client.ui.kit.*
import com.wintercogs.beyonddimensions.client.ui.machine.Flames
import com.wintercogs.beyonddimensions.common.init.BDItems
import dev.compixel.forge.item.ItemIcon
import dev.compixel.forge.item.MinecraftItemIcon
import dev.compixel.ui.ore.display.OreGlyph
import dev.compixel.ui.ore.display.OreText
import dev.compixel.ui.ore.inventory.OreSlot
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

/** 颜色编辑器里预览用的物品图标与文字，打开编辑器时在游戏线程取出 */
internal class BdPreviewContent {
    val icon = ItemIcon.snapshot(ItemStack(BDItems.NET_TERMINAL_ITEM.get()))
    val items =
        listOf(Items.DIAMOND, Items.IRON_INGOT, Items.OAK_LOG, Items.REDSTONE, Items.GOLD_INGOT, Items.EMERALD).map {
            ItemIcon.snapshot(ItemStack(it))
        }
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
    val warning = tr("ui.beyonddimensions.color_preview.warning")
    val danger = tr("ui.beyonddimensions.color_preview.danger")
    val setting = tr("ui.beyonddimensions.color_preview.setting")
    val settingHint = tr("ui.beyonddimensions.color_preview.setting.hint")
    val chip = tr("ui.beyonddimensions.color_preview.chip")
    val tag = tr("ui.beyonddimensions.color_preview.tag")
    val popover = tr("ui.beyonddimensions.color_preview.popover")
    val popoverHint = tr("ui.beyonddimensions.color_preview.popover.hint")
}

/**
 * 颜色编辑器旁的预览：一个缩小的 BD 窗口，由界面里真实的控件组成，每种颜色都有地方显示。
 * 槽位依次是普通、悬停、标记、标记且悬停与空格；开关可以点。高度与真实的 BD 窗口相当：1080p 在自动界面缩放下，
 * 编辑器给预览的高度约 230，再矮时编辑器让它滚动。
 */
@Composable
internal fun BdPreview(content: BdPreviewContent) {
    val colors = Bd.colors
    val items = content.items
    var checked by remember { mutableStateOf(true) }
    BdWindow(Modifier.width(200.dp).height(228.dp)) {
        BdHeader(content.icon, content.title, {}, tag = "#0001")
        Row(Modifier.weight(1f)) {
            Column(
                Modifier.width(SIDE_RAIL_WIDTH.dp)
                    .fillMaxHeight()
                    .background(colors[BdColors.sunken])
                    .drawBehind {
                        val x = size.width - 0.5f
                        drawLine(colors[BdColors.line], Offset(x, 0f), Offset(x, size.height), 1.dp.toPx())
                    }
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                BdRailTab(content.storage, BdGlyphs.Main, selected = true) {}
                BdRailTab(content.crafting, BdGlyphs.Crafting, selected = false) {}
                Spacer(Modifier.weight(1f))
                BdRailTab(content.settings, OreGlyph.Gear.art, selected = false) {}
            }
            Column(Modifier.fillMaxHeight().padding(6.dp)) {
                BdSearchField("", {}, content.search, Modifier.fillMaxWidth())
                Spacer(Modifier.height(5.dp))
                BdSectionLabel(content.stored)
                Spacer(Modifier.height(4.dp))
                Row(Modifier.background(colors[BdColors.line]).padding(0.5.dp)) {
                    PreviewSlot(items[0], "64")
                    PreviewSlot(items[1], "1.2K", hovered = true)
                    PreviewSlot(items[2], "", marked = true)
                    PreviewSlot(items[3], "9", marked = true, hovered = true)
                    PreviewSlot(null, "")
                    PreviewSlot(items[4], "3")
                    PreviewSlot(items[5], "512")
                }
                Spacer(Modifier.height(6.dp))
                OreText(content.body, color = colors[BdColors.text], maxLines = 1)
                OreText(content.muted, color = colors[BdColors.muted], style = Bd.caption, maxLines = 1)
                OreText(content.faint, color = colors[BdColors.faint], style = Bd.caption, maxLines = 1)
                Spacer(Modifier.height(5.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Status(colors[BdColors.online], content.online)
                    Spacer(Modifier.width(6.dp))
                    Status(colors[BdColors.warning], content.warning)
                    Spacer(Modifier.width(6.dp))
                    Status(colors[BdColors.danger], content.danger)
                }
                Spacer(Modifier.height(5.dp))
                BdSettingRow(content.setting, content.settingHint) { BdToggle(checked, { checked = it }) }
                Spacer(Modifier.height(5.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BdChip({}, active = true) { OreText(content.chip, color = colors[BdColors.text], maxLines = 1) }
                    Spacer(Modifier.width(4.dp))
                    Box(Modifier.background(colors[BdColors.accentDeep], Bd.ChipShape).padding(horizontal = 3.dp)) {
                        OreText(content.tag, color = colors[BdColors.onAccent], style = Bd.caption, maxLines = 1)
                    }
                    Spacer(Modifier.weight(1f))
                    Flames(1f, 0, null)
                }
                Spacer(Modifier.height(4.dp))
                BdMeter(0.6f, Modifier.fillMaxWidth().height(5.dp))
                Spacer(Modifier.weight(1f).heightIn(min = 6.dp))
                Column(
                    Modifier.fillMaxWidth()
                        .background(colors[BdColors.popover], Bd.ChipShape)
                        .border(1.dp, colors[BdColors.lineStrong], Bd.ChipShape)
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    OreText(content.popover, color = colors[BdColors.text], style = Bd.caption, maxLines = 1)
                    OreText(content.popoverHint, color = colors[BdColors.muted], style = Bd.caption, maxLines = 1)
                }
            }
        }
    }
}

/** 与存储格子里的虚拟槽位画法相同：Ore 凹槽，标记槽铺强调色，数量标签与悬停角标 */
@Composable
private fun PreviewSlot(icon: ItemIcon?, amount: String, marked: Boolean = false, hovered: Boolean = false) {
    val colors = Bd.colors
    Box(Modifier.size(SLOT_PITCH.dp)) {
        OreSlot(Modifier.matchParentSize(), highlighted = hovered, contentModifier = Modifier.size(16.dp)) {
            if (marked) Box(Modifier.matchParentSize().background(colors[BdColors.accentSoft]))
            icon?.let { MinecraftItemIcon(it, Modifier.fillMaxSize()) }
        }
        if (amount.isNotEmpty())
            BdAmountPill(amount, Modifier.align(Alignment.BottomEnd).padding(end = 1.dp, bottom = 1.dp))
        if (hovered) Box(Modifier.matchParentSize().brackets(colors[BdColors.accent], arm = 4.dp))
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
