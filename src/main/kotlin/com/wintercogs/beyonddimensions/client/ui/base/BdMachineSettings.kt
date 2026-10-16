package com.wintercogs.beyonddimensions.client.ui.base

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wintercogs.beyonddimensions.api.machine.MachineType
import com.wintercogs.beyonddimensions.api.machine.SettingEditor
import com.wintercogs.beyonddimensions.api.registry.BDRegistries
import com.wintercogs.beyonddimensions.client.ui.kit.*
import com.wintercogs.beyonddimensions.client.ui.theme.Bd
import com.wintercogs.beyonddimensions.client.ui.theme.BdColors
import com.wintercogs.beyonddimensions.common.machine.MachineSides
import com.wintercogs.beyonddimensions.common.menu.machine.MachineSettingsSync
import dev.compixel.ui.ore.display.OreText
import net.minecraft.resources.ResourceLocation

/** 设置页的一行，由游戏线程按机器类型生成；[section] 是所在分组的标题，没有分组时为空 */
data class MachineSettingRow(
    val setting: ResourceLocation,
    val title: String,
    val description: String?,
    val editor: MachineSettingControl,
    val enabled: Boolean,
    val section: String? = null,
)

/** 一行设置用的控件 */
sealed interface MachineSettingControl {
    /** 从几个选项中选一个；[cycle] 为真时是点一下换下一项的单个按钮 */
    data class Choice(val options: List<String>, val selected: Int, val cycle: Boolean) : MachineSettingControl

    data class Toggle(val on: Boolean) : MachineSettingControl

    /** 开关的短按钮形式，同一组里相邻的几个并排成一行；[label] 是按钮上的名称 */
    data class Chip(val label: String, val on: Boolean) : MachineSettingControl

    /** 面配置：六个面合成一个展开图，行的 [MachineSettingRow.setting] 是其中第一个面 */
    data class Sides(val state: SidesState) : MachineSettingControl
}

/** 设置页上的一次修改：编辑声明里的第 [index] 个值，开关的 0 为打开、1 为关闭 */
data class MachineSettingPick(val setting: ResourceLocation, val index: Int)

/**
 * 按菜单同步来的设置生成设置页的各行，顺序与分组与机器类型的声明相同；在游戏线程调用。
 * 六个面合成一行面配置，排在第一个面的位置；没有 [sides] 时面配置与主动弹出都不显示，例如动态结构里的接口
 */
fun machineSettingRows(
    snapshot: MachineSettingsSync.Snapshot,
    sides: MachineSidesView? = null
): List<MachineSettingRow> {
    val type = snapshot.machineType() ?: return emptyList()
    val firstFace = type.settings().firstOrNull { MachineSides.isFace(it.setting()) }
    return type.settings().mapNotNull { entry ->
        val row =
            when {
                !MachineSides.isSideSetting(type, entry.setting()) -> settingRow(entry, snapshot)
                sides == null -> null
                entry === firstFace -> sides.row(snapshot)
                MachineSides.isFace(entry.setting()) -> null
                else -> settingRow(entry, snapshot)
            }
        val section = entry.sectionKey()
        if (row == null || section == null) row else row.copy(section = tr(section))
    }
}

private fun <T> settingRow(entry: MachineType.Entry<T>, snapshot: MachineSettingsSync.Snapshot): MachineSettingRow? {
    val setting = entry.setting()
    val id = BDRegistries.MACHINE_SETTINGS.getKey(setting) ?: return null
    val value = entry.valueIn(snapshot.settings())
    val locked = snapshot.isLocked(setting)
    val hint = if (locked) entry.lockedHintKey() ?: entry.hintKey() else entry.hintKey()
    val title = tr(entry.labelKey())
    return when (val editor = entry.editor()) {
        is SettingEditor.Choice<T> -> {
            val selected = editor.options().indexOf(value).coerceAtLeast(0)
            // 选项各自有说明时显示选中项的说明
            val description = editor.optionHint()?.apply(editor.options()[selected]) ?: hint
            MachineSettingRow(
                id,
                title,
                description?.let(::tr),
                MachineSettingControl.Choice(
                    editor.options().map { tr(editor.optionLabel().apply(it)) },
                    selected,
                    editor.cycle()
                ),
                !locked,
            )
        }

        is SettingEditor.Toggle<T> -> {
            val on = value == editor.on()
            val control = entry.chipKey()?.let { MachineSettingControl.Chip(tr(it), on) } ?: MachineSettingControl.Toggle(on)
            MachineSettingRow(id, title, hint?.let(::tr), control, !locked)
        }

        else -> null
    }
}

/**
 * 机器的设置页，每项设置一行，同一组里相邻的短按钮并排成一行；[shown] 为 false 时控件不响应。
 * 机器类型分了组时每组前有标题，组与组之间有分隔线
 */
@Composable
fun BdMachineSettings(rows: List<MachineSettingRow>, shown: Boolean, onPick: (MachineSettingPick) -> Unit) {
    var start = 0
    while (start < rows.size) {
        val row = rows[start]
        var end = start + 1
        if (row.editor is MachineSettingControl.Chip)
            while (end < rows.size && rows[end].editor is MachineSettingControl.Chip && rows[end].section == row.section) end++
        val opensSection = row.section != null && (start == 0 || rows[start - 1].section != row.section)
        val first = start == 0
        val group = rows.subList(start, end)
        key(row.setting) {
            if (opensSection) {
                if (!first) SectionDivider()
                BdSectionLabel(row.section)
            }
            if (row.editor is MachineSettingControl.Chip) SettingChips(group, shown, onPick) else SettingRow(row, shown, onPick)
        }
        start = end
    }
}

@Composable
private fun SettingRow(row: MachineSettingRow, shown: Boolean, onPick: (MachineSettingPick) -> Unit) {
    when (val control = row.editor) {
        is MachineSettingControl.Choice ->
            BdModeSetting(
                row.title,
                control.options,
                control.selected,
                shown && row.enabled,
                description = row.description,
                cycle = control.cycle,
            ) { onPick(MachineSettingPick(row.setting, it)) }

        is MachineSettingControl.Toggle ->
            BdSettingRow(row.title, row.description) {
                BdToggle(
                    control.on,
                    { onPick(MachineSettingPick(row.setting, if (it) 0 else 1)) },
                    shown && row.enabled
                )
            }

        is MachineSettingControl.Chip -> SettingChips(listOf(row), shown, onPick)
        is MachineSettingControl.Sides -> BdMachineSides(row.title, control.state, shown, onPick)
    }
}

/** 并排的短按钮开关，平分一行，打开的带强调色的边框 */
@Composable
private fun SettingChips(rows: List<MachineSettingRow>, shown: Boolean, onPick: (MachineSettingPick) -> Unit) {
    val colors = Bd.colors
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        for (row in rows) key(row.setting) {
            val chip = row.editor as MachineSettingControl.Chip
            BdChip(
                { onPick(MachineSettingPick(row.setting, if (chip.on) 1 else 0)) },
                Modifier.weight(1f),
                enabled = shown && row.enabled,
                active = chip.on,
            ) {
                Spacer(Modifier.weight(1f))
                OreText(chip.label, color = colors[if (chip.on) BdColors.accentDeep else BdColors.faint], maxLines = 1)
                Spacer(Modifier.weight(1f))
            }
        }
    }
}

/** 两组设置之间的分隔：一道凹下的细线，上暗下亮 */
@Composable
private fun SectionDivider() {
    val colors = Bd.colors
    Column(Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(colors[BdColors.outline]))
        Box(Modifier.fillMaxWidth().height(1.dp).background(colors[BdColors.bevelLight]))
    }
}
