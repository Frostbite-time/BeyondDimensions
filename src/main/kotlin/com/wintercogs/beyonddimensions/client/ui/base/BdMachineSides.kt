package com.wintercogs.beyonddimensions.client.ui.base

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.unit.dp
import com.wintercogs.beyonddimensions.api.machine.SettingEditor
import com.wintercogs.beyonddimensions.api.registry.BDRegistries
import com.wintercogs.beyonddimensions.client.ui.kit.BdSectionLabel
import com.wintercogs.beyonddimensions.client.ui.kit.BdTooltip
import com.wintercogs.beyonddimensions.client.ui.kit.brackets
import com.wintercogs.beyonddimensions.client.ui.theme.Bd
import com.wintercogs.beyonddimensions.client.ui.theme.BdColors
import com.wintercogs.beyonddimensions.common.init.BDMachineSettings
import com.wintercogs.beyonddimensions.common.machine.MachineSides
import com.wintercogs.beyonddimensions.common.machine.SideMode
import com.wintercogs.beyonddimensions.common.menu.machine.MachineSettingsSync
import dev.compixel.forge.item.ItemIcon
import dev.compixel.forge.item.MinecraftItemIcon
import dev.compixel.ui.LocalUiFeedback
import dev.compixel.ui.ore.display.OreText
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import java.util.*

/** 面配置页上的一面：在展开图里的格子、名称、可选与当前的模式，以及相邻的方块 */
data class SideFace(
    val setting: ResourceLocation,
    val column: Int,
    val row: Int,
    /** 方向名，有相邻方块时带上它的名字 */
    val title: String,
    val options: List<SideMode>,
    /** 当前模式在 [options] 里的下标 */
    val selected: Int,
    val enabled: Boolean,
    val neighbor: ItemIcon?,
)

/** 一种模式的名称与说明 */
data class SideModeText(val label: String, val hint: String?)

data class SidePageState(
    val title: String,
    val hint: String,
    val faces: List<SideFace>,
    val modes: Map<SideMode, SideModeText>,
    /** 主动弹出的开关，机器类型没有时为 null */
    val push: MachineSettingRow?,
)

/**
 * 生成面配置页的状态，在游戏线程使用。展开图以打开界面时玩家的朝向为准：正中是面对玩家的一面，
 * 左右是玩家看过去的左右，左下角是背面，上下是顶面与底面。相邻方块变了才重新取图标
 */
class MachineSidesView(private val level: Level, private val pos: BlockPos, facing: Direction) {
    private val layout =
        listOf(
            Triple(Direction.UP, 1, 0),
            Triple(facing.counterClockWise, 0, 1),
            Triple(facing.opposite, 1, 1),
            Triple(facing.clockWise, 2, 1),
            Triple(facing, 0, 2),
            Triple(Direction.DOWN, 1, 2),
        )
    private val neighbors = EnumMap<Direction, Neighbor>(Direction::class.java)
    private val title = tr("ui.beyonddimensions.side.title")
    private val hint = tr("ui.beyonddimensions.side.hint")

    private class Neighbor(val block: Block, val icon: ItemIcon?, val name: String?)

    /** 机器类型没有面配置或设置还没同步到时为 null */
    fun snapshot(snapshot: MachineSettingsSync.Snapshot): SidePageState? {
        val type = snapshot.machineType() ?: return null
        if (!MachineSides.hasSides(type)) return null
        val modes = EnumMap<SideMode, SideModeText>(SideMode::class.java)
        val faces =
            layout.map { (side, column, row) ->
                val setting = MachineSides.face(side)
                val entry = checkNotNull(type.entry(setting))
                val editor = entry.editor() as SettingEditor.Choice<SideMode>
                for (mode in editor.options()) {
                    modes.getOrPut(mode) {
                        SideModeText(tr(editor.optionLabel().apply(mode)), editor.optionHint()?.apply(mode)?.let(::tr))
                    }
                }
                val neighbor = neighbor(side)
                val direction = tr(entry.labelKey())
                SideFace(
                    checkNotNull(BDRegistries.MACHINE_SETTINGS.getKey(setting)),
                    column,
                    row,
                    neighbor.name?.let { tr("ui.beyonddimensions.side.face", direction, it) } ?: direction,
                    editor.options(),
                    editor.options().indexOf(snapshot.settings().get(setting)),
                    !snapshot.isLocked(setting),
                    neighbor.icon,
                )
            }
        val push = type.entry(BDMachineSettings.POP.get())?.let { settingRow(it, snapshot) }
        return SidePageState(title, hint, faces, modes.toMap(), push)
    }

    private fun neighbor(side: Direction): Neighbor {
        val state = level.getBlockState(pos.relative(side))
        neighbors[side]?.takeIf { it.block == state.block }?.let { return it }
        val item = state.block.asItem()
        val icon = if (item == Items.AIR) null else ItemIcon.snapshot(ItemStack(item))
        val name = if (state.isAir) null else state.block.name.string
        return Neighbor(state.block, icon, name).also { neighbors[side] = it }
    }
}

/** 展开图里一格的边长与格间距 */
private const val CELL = 20
private const val CELL_GAP = 2

/** 面配置页：六个面排成展开的方块，左键换到下一种模式、右键换到上一种；下面是图例、说明与主动弹出 */
@Composable
fun BdMachineSides(state: SidePageState, shown: Boolean, onPick: (MachineSettingPick) -> Unit) {
    val colors = Bd.colors
    BdSectionLabel(state.title)
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(Modifier.size((3 * CELL + 2 * CELL_GAP).dp, (3 * CELL + 2 * CELL_GAP).dp)) {
            for (face in state.faces) key(face.setting) {
                FaceCell(
                    face,
                    state.modes,
                    shown,
                    onPick,
                    Modifier.offset((face.column * (CELL + CELL_GAP)).dp, (face.row * (CELL + CELL_GAP)).dp),
                )
            }
        }
    }
    Legend(state.modes)
    OreText(state.hint, color = colors[BdColors.faint], style = Bd.caption)
    state.push?.let { BdMachineSettings(listOf(it), shown, onPick) }
}

/** 输入用强调色，输出用警示色；双向是默认，与普通的格子一样 */
@Composable
private fun sideColor(mode: SideMode?): Color? =
    when (mode) {
        SideMode.INPUT -> Bd.colors[BdColors.accent]
        SideMode.OUTPUT -> Bd.colors[BdColors.warning]
        SideMode.BOTH,
        null -> null
    }

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun FaceCell(
    face: SideFace,
    modes: Map<SideMode, SideModeText>,
    shown: Boolean,
    onPick: (MachineSettingPick) -> Unit,
    modifier: Modifier,
) {
    val colors = Bd.colors
    val feedback = LocalUiFeedback.current
    val mode = face.options.getOrNull(face.selected)
    val tint = sideColor(mode)
    val enabled = shown && face.enabled && face.options.isNotEmpty()
    var hovered by remember { mutableStateOf(false) }
    BdTooltip(
        {
            OreText(face.title, color = colors[BdColors.text], style = Bd.caption)
            mode?.let(modes::get)?.let { text ->
                OreText(text.label, color = tint ?: colors[BdColors.text], style = Bd.caption)
                text.hint?.let { OreText(it, color = colors[BdColors.muted], style = Bd.caption) }
            }
        },
        modifier,
    ) {
        Box(
            Modifier.size(CELL.dp)
                .alpha(if (face.enabled) 1f else 0.55f)
                .background(tint?.copy(alpha = 0.18f) ?: colors[BdColors.surface])
                .border(1.dp, tint ?: colors[if (hovered && enabled) BdColors.lineStrong else BdColors.line])
                .onPointerEvent(PointerEventType.Enter) { hovered = true }
                .onPointerEvent(PointerEventType.Exit) { hovered = false }
                .onPointerEvent(PointerEventType.Press) {
                    if (!enabled) return@onPointerEvent
                    val step =
                        when {
                            it.buttons.isPrimaryPressed -> 1
                            it.buttons.isSecondaryPressed -> -1
                            else -> return@onPointerEvent
                        }
                    onPick(MachineSettingPick(face.setting, (face.selected + step).mod(face.options.size)))
                    feedback.activate()
                },
            contentAlignment = Alignment.Center,
        ) {
            face.neighbor?.let { MinecraftItemIcon(it, Modifier.size(16.dp)) }
            if (hovered && enabled) Box(Modifier.matchParentSize().brackets(tint ?: colors[BdColors.accent], arm = 4.dp))
        }
    }
}

/** 图例：每种模式一个与格子同样配色的小方块 */
@Composable
private fun Legend(modes: Map<SideMode, SideModeText>) {
    val colors = Bd.colors
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        modes.entries.forEachIndexed { index, (mode, text) ->
            if (index > 0) Spacer(Modifier.width(8.dp))
            val tint = sideColor(mode)
            Box(
                Modifier.size(6.dp)
                    .background(tint?.copy(alpha = 0.18f) ?: colors[BdColors.surface])
                    .border(1.dp, tint ?: colors[BdColors.lineStrong])
            )
            Spacer(Modifier.width(3.dp))
            OreText(text.label, color = colors[BdColors.muted], style = Bd.caption, maxLines = 1)
        }
    }
}
