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
import com.wintercogs.beyonddimensions.client.ui.kit.*
import com.wintercogs.beyonddimensions.client.ui.theme.Bd
import com.wintercogs.beyonddimensions.client.ui.theme.BdColors
import com.wintercogs.beyonddimensions.common.block.orientation.FacingRule
import com.wintercogs.beyonddimensions.common.machine.MachineSides
import com.wintercogs.beyonddimensions.common.machine.RelativeSide
import com.wintercogs.beyonddimensions.common.machine.setting.SideMode
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
import net.minecraft.world.level.block.entity.BlockEntity
import java.util.*

/** 面配置里的一面：在展开图里的格子、名称、可选与当前的模式，以及相邻的方块 */
data class SideFace(
    val setting: ResourceLocation,
    val column: Int,
    val row: Int,
    /** 面的名称，有相邻方块时带上它的名字 */
    val title: String,
    val options: List<SideMode>,
    /** 当前模式在 [options] 里的下标 */
    val selected: Int,
    val enabled: Boolean,
    val neighbor: ItemIcon?,
)

/** 设置页上的面配置：六个面与各模式的名称 */
data class SidesState(val faces: List<SideFace>, val modes: Map<SideMode, String>)

/**
 * 生成设置页上的面配置，在游戏线程使用。展开图以方块的朝向为准：正中是正面，左右是站在正面前看过去的左右，
 * 左下角是背面，上下是顶面与底面。相邻方块变了才重新取图标
 */
class MachineSidesView(private val level: Level, private val pos: BlockPos) {
    private val neighbors = EnumMap<Direction, Neighbor>(Direction::class.java)
    private val title = tr("ui.beyonddimensions.side.title")

    private class Neighbor(val block: Block, val icon: ItemIcon?, val name: String?)

    /** 设置页上面配置的一行，代替六个面各自的行；机器类型没有面配置时为 null */
    fun row(snapshot: MachineSettingsSync.Snapshot): MachineSettingRow? {
        val type = snapshot.machineType() ?: return null
        if (!MachineSides.hasSides(type)) return null
        val orientation = FacingRule.orientation(level.getBlockState(pos))
        val modes = EnumMap<SideMode, String>(SideMode::class.java)
        val faces =
            LAYOUT.map { (side, column, row) ->
                val setting = MachineSides.face(side)
                val entry = checkNotNull(type.entry(setting))
                val editor = entry.editor() as SettingEditor.Choice<SideMode>
                for (mode in editor.options()) modes.getOrPut(mode) { tr(editor.optionLabel().apply(mode)) }
                val neighbor = neighbor(side.direction(orientation))
                val name = tr(entry.labelKey())
                SideFace(
                    checkNotNull(BDRegistries.MACHINE_SETTINGS.getKey(setting)),
                    column,
                    row,
                    neighbor.name?.let { tr("ui.beyonddimensions.side.face", name, it) } ?: name,
                    editor.options(),
                    editor.options().indexOf(entry.valueIn(snapshot.settings())),
                    !snapshot.isLocked(setting),
                    neighbor.icon,
                )
            }
        val sides = MachineSettingControl.Sides(SidesState(faces, modes.toMap()))
        return MachineSettingRow(faces.first().setting, title, null, sides, true)
    }

    private fun neighbor(direction: Direction): Neighbor {
        val state = level.getBlockState(pos.relative(direction))
        neighbors[direction]?.takeIf { it.block == state.block }?.let { return it }
        val item = state.block.asItem()
        val icon = if (item == Items.AIR) null else ItemIcon.snapshot(ItemStack(item))
        val name = if (state.isAir) null else state.block.name.string
        return Neighbor(state.block, icon, name).also { neighbors[direction] = it }
    }
}

/** 展开图里各面的格子：正面居中，背面在左下角 */
private val LAYOUT =
    listOf(
        Triple(RelativeSide.TOP, 1, 0),
        Triple(RelativeSide.LEFT, 0, 1),
        Triple(RelativeSide.FRONT, 1, 1),
        Triple(RelativeSide.RIGHT, 2, 1),
        Triple(RelativeSide.BACK, 0, 2),
        Triple(RelativeSide.BOTTOM, 1, 2),
    )

/** 方块机器的面配置视图；方块实体不在世界里时为 null，例如动态结构里的接口 */
fun machineSidesView(be: BlockEntity?): MachineSidesView? = be?.level?.let { MachineSidesView(it, be.blockPos) }

/** 展开图里一格的边长与格间距 */
private const val CELL = 20
private const val CELL_GAP = 2

/** 设置页上的面配置：标题下是六个面排成的展开方块，左键换到下一种模式、右键换到上一种；再下面是图例 */
@Composable
fun BdMachineSides(title: String, state: SidesState, shown: Boolean, onPick: (MachineSettingPick) -> Unit) {
    val colors = Bd.colors
    Column(Modifier.fillMaxWidth()) {
        OreText(title, color = colors[BdColors.text], maxLines = 1)
        Spacer(Modifier.height(2.dp))
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
        Spacer(Modifier.height(4.dp))
        Legend(state.modes)
    }
}

/** 输入、输出与双向各有配色；禁用的面与普通的格子一样 */
@Composable
private fun sideColor(mode: SideMode?): Color? =
    when (mode) {
        SideMode.INPUT -> Bd.colors[BdColors.sideInput]
        SideMode.OUTPUT -> Bd.colors[BdColors.sideOutput]
        SideMode.BOTH -> Bd.colors[BdColors.sideBoth]
        SideMode.NONE,
        null -> null
    }

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun FaceCell(
    face: SideFace,
    modes: Map<SideMode, String>,
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
            mode?.let(modes::get)?.let { OreText(it, color = tint ?: colors[BdColors.text], style = Bd.caption) }
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
            if (hovered && enabled) Box(
                Modifier.matchParentSize().brackets(tint ?: colors[BdColors.accent], arm = 4.dp)
            )
        }
    }
}

/** 图例：每种模式一个与格子同样配色的小方块，一行放不下时折行 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Legend(modes: Map<SideMode, String>) {
    val colors = Bd.colors
    FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        for ((mode, label) in modes) {
            val tint = sideColor(mode)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(6.dp)
                        .background(tint?.copy(alpha = 0.18f) ?: colors[BdColors.surface])
                        .border(1.dp, tint ?: colors[BdColors.lineStrong])
                )
                Spacer(Modifier.width(3.dp))
                OreText(label, color = colors[BdColors.muted], style = Bd.caption, maxLines = 1)
            }
        }
    }
}
