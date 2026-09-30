package com.wintercogs.beyonddimensions.client.ui.device

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wintercogs.beyonddimensions.client.ui.base.BdThemes
import com.wintercogs.beyonddimensions.client.ui.base.at
import com.wintercogs.beyonddimensions.client.ui.base.tr
import com.wintercogs.beyonddimensions.common.init.BDDataComponents
import com.wintercogs.beyonddimensions.common.item.XpExchangeSettings
import com.wintercogs.beyonddimensions.common.menu.NetEnergyMenu
import com.wintercogs.beyonddimensions.common.menu.XpExchangeMenu
import com.wintercogs.beyonddimensions.util.StringFormat
import dev.compixel.forge.slots.ComposeMenuSlots
import dev.compixel.ui.ore.display.OreProgressBar
import dev.compixel.ui.ore.display.OreText
import dev.compixel.ui.ore.input.OreIntField
import dev.compixel.ui.ore.theme.OreTheme
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.ItemStack

/* 以数值为主的设备界面：能量网络与经验棒设置。布局沿用旧版，数值在游戏线程读取与格式化。 */

/** 能量条的填充比例，以及其下方的储量与每刻变化 */
data class EnergyView(val progress: Float, val amount: String, val rate: String)

/** 能量网络：能量条、储量与每刻变化；右侧是弹出与红石控制按钮 */
class EnergyScreen(menu: NetEnergyMenu, inventory: Inventory, title: Component) :
    DeviceScreen<NetEnergyMenu>(
        menu,
        title,
        DeviceController(
            menu,
            listOf(
                popTab({ menu.be.popMode }, { menu.be.popMode = it }),
                controlTab({ menu.be.controlMode }, { menu.be.controlMode = it }),
            ),
            extra = {
                EnergyView(
                    if (menu.lastEnergyCapacity > 0) (menu.lastEnergyStored.toDouble() / menu.lastEnergyCapacity).toFloat() else 0f,
                    StringFormat.formatCount(menu.lastEnergyStored) + "/" + StringFormat.formatCount(menu.lastEnergyCapacity),
                    StringFormat.formatChange(menu.lastEnergySpeedState) + " FE/t",
                )
            },
        ),
        DeviceScreen.deviceLabels(menu, inventory, title, null, titleY = 6),
        { _, state, _ -> EnergyBody(state.extra as EnergyView) },
    )

@Composable
private fun EnergyBody(view: EnergyView) {
    // 旧版能量条连同外框占 (7, 34) 起的 162×18，储量与变化在其下方
    OreTheme(id = BdThemes.Energy) { OreProgressBar(view.progress, Modifier.at(7, 34).size(162.dp, 18.dp)) }
    OreText(view.amount, Modifier.at(8, 52), maxLines = 1)
    OreText(view.rate, Modifier.at(8, 62), maxLines = 1)
}

/** 经验棒要保持的目标等级 */
data class XpView(val target: Int)

/** 界面发回的新目标等级，在游戏线程限制到有效范围后写回物品 */
data class TargetLevel(val level: Int)

/** 经验棒设置：目标等级与最高等级；右侧是保持模式按钮 */
class XpScreen(menu: XpExchangeMenu, inventory: Inventory, title: Component) :
    DeviceScreen<XpExchangeMenu>(
        menu,
        title,
        DeviceController(
            menu,
            listOf(keepTab(menu.menuStack)),
            extra = { XpView(XpExchangeSettings.getTargetLevel(menu.menuStack)) },
            custom = { action ->
                if (action is TargetLevel) {
                    XpExchangeSettings.setTargetLevel(menu.menuStack, XpExchangeSettings.sanitizeTargetLevel(action.level))
                    menu.writeAndSendQuickData()
                }
            },
        ),
        DeviceScreen.deviceLabels(menu, inventory, title, null),
        xpBody(),
    )

private fun keepTab(stack: ItemStack) =
    ModeTab(
        listOf(true, false),
        { if (it) "control_mode_ignore" else "control_mode_not_working" },
        { "tooltip.button.beyonddimensions.xp_exchange.keep_mode_${if (it) "working" else "not_working"}" },
        { stack.getOrDefault(BDDataComponents.XP_NET_KEEP_MODE.get(), false) },
        { stack.set(BDDataComponents.XP_NET_KEEP_MODE.get(), it) },
    )

/** 文字在游戏线程取出后交给界面 */
private fun xpBody(): @Composable (ComposeMenuSlots<XpExchangeMenu>, DeviceState, (Any) -> Unit) -> Unit {
    val target = tr("menu.label.beyonddimensions.xp_exchange.target_level")
    val max = tr("menu.label.beyonddimensions.xp_exchange.max_level", XpExchangeSettings.MAX_TARGET_LEVEL)
    return { _, state, send -> XpBody(state.extra as XpView, target, max, send) }
}

@Composable
private fun XpBody(view: XpView, target: String, max: String, send: (Any) -> Unit) {
    // 旧版输入框占 y 24 到 39；Ore 的数字输入框更高，与它居中对齐，标签随之与旧版基线对齐。
    // 输入框占满标签右侧到槽位右缘的空间，标签过长时截断，输入框不会伸出面板
    Row(Modifier.at(8, 21).width(160.dp), verticalAlignment = Alignment.CenterVertically) {
        OreText(target, Modifier.widthIn(max = 74.dp), maxLines = 1)
        Spacer(Modifier.width(6.dp))
        OreTheme(id = BdThemes.Field) {
            OreIntField(
                view.target,
                { send(TargetLevel(it)) },
                Modifier.weight(1f),
                range = 0..XpExchangeSettings.MAX_TARGET_LEVEL,
            )
        }
    }
    OreText(max, Modifier.at(8, 41), maxLines = 1)
}
