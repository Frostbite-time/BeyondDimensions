package com.wintercogs.beyonddimensions.client.ui.base

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import com.wintercogs.beyonddimensions.api.machine.MachineType
import com.wintercogs.beyonddimensions.api.machine.SettingEditor
import com.wintercogs.beyonddimensions.api.registry.BDRegistries
import com.wintercogs.beyonddimensions.client.ui.kit.BdModeSetting
import com.wintercogs.beyonddimensions.client.ui.kit.BdSettingRow
import com.wintercogs.beyonddimensions.client.ui.kit.BdToggle
import com.wintercogs.beyonddimensions.common.machine.MachineSides
import com.wintercogs.beyonddimensions.common.menu.machine.MachineSettingsSync
import net.minecraft.resources.ResourceLocation

/** 设置页的一行，由游戏线程按机器类型生成 */
data class MachineSettingRow(
    val setting: ResourceLocation,
    val title: String,
    val description: String?,
    val editor: MachineSettingControl,
    val enabled: Boolean,
)

/** 一行设置用的控件 */
sealed interface MachineSettingControl {
    /** 从几个选项中选一个；[cycle] 为真时是点一下换下一项的单个按钮 */
    data class Choice(val options: List<String>, val selected: Int, val cycle: Boolean) : MachineSettingControl

    data class Toggle(val on: Boolean) : MachineSettingControl

    /** 面配置：六个面合成一个展开图，行的 [MachineSettingRow.setting] 是其中第一个面 */
    data class Sides(val state: SidesState) : MachineSettingControl
}

/** 设置页上的一次修改：编辑声明里的第 [index] 个值，开关的 0 为打开、1 为关闭 */
data class MachineSettingPick(val setting: ResourceLocation, val index: Int)

/**
 * 按菜单同步来的设置生成设置页的各行，顺序与机器类型的声明相同；在游戏线程调用。
 * 六个面合成一行面配置，排在第一个面的位置；没有 [sides] 时面配置与主动弹出都不显示，例如动态结构里的接口
 */
fun machineSettingRows(snapshot: MachineSettingsSync.Snapshot, sides: MachineSidesView? = null): List<MachineSettingRow> {
    val type = snapshot.machineType() ?: return emptyList()
    val firstFace = type.settings().firstOrNull { MachineSides.isFace(it.setting()) }
    return type.settings().mapNotNull {
        when {
            !MachineSides.isSideSetting(type, it.setting()) -> settingRow(it, snapshot)
            sides == null -> null
            it === firstFace -> sides.row(snapshot)
            MachineSides.isFace(it.setting()) -> null
            else -> settingRow(it, snapshot)
        }
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

        is SettingEditor.Toggle<T> ->
            MachineSettingRow(id, title, hint?.let(::tr), MachineSettingControl.Toggle(value == editor.on()), !locked)

        else -> null
    }
}

/** 机器的设置页，每项设置一行；[shown] 为 false 时控件不响应 */
@Composable
fun BdMachineSettings(rows: List<MachineSettingRow>, shown: Boolean, onPick: (MachineSettingPick) -> Unit) {
    for (row in rows) key(row.setting) {
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

            is MachineSettingControl.Sides -> BdMachineSides(row.title, control.state, shown, onPick)
        }
    }
}
