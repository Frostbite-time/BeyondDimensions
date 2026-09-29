package com.wintercogs.beyonddimensions.client.ui.device

import com.wintercogs.beyonddimensions.common.init.BDDataComponents
import com.wintercogs.beyonddimensions.common.machine.HopperFluidMode
import com.wintercogs.beyonddimensions.common.machine.HopperItemMode
import com.wintercogs.beyonddimensions.common.machine.HopperNBTMode
import com.wintercogs.beyonddimensions.common.machine.HopperXpMode
import com.wintercogs.beyonddimensions.common.menu.NetFeederMenu
import com.wintercogs.beyonddimensions.common.menu.NetHopperMenu
import com.wintercogs.beyonddimensions.common.menu.NetInterfaceBaseMenu
import com.wintercogs.beyonddimensions.common.menu.NetMagnetMenu
import com.wintercogs.beyonddimensions.common.menu.NetPumpMenu
import com.wintercogs.beyonddimensions.common.menu.NetRestockerMenu
import net.minecraft.core.component.DataComponentType
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.registries.DeferredHolder

private const val FILTER_SLOTS = "menu.label.beyonddimensions.filter_slots"

/** 物品上的设置：读取时缺省为 [fallback]，写回物品后由菜单同步 */
private fun <T : Any> ItemStack.setting(component: DeferredHolder<DataComponentType<*>, DataComponentType<T>>, fallback: T): Pair<() -> T, (T) -> Unit> =
    Pair({ get(component.get()) ?: fallback }, { value -> set(component.get(), value) })

class PumpScreen(menu: NetPumpMenu, inventory: Inventory, title: Component) :
    DeviceScreen<NetPumpMenu>(
        menu,
        inventory,
        title,
        FILTER_SLOTS,
        listOf(
            filterTab({ menu.be.filterMode }, { menu.be.filterMode = it }),
            controlTab({ menu.be.controlMode }, { menu.be.controlMode = it }),
        ),
    )

class HopperScreen(menu: NetHopperMenu, inventory: Inventory, title: Component) :
    DeviceScreen<NetHopperMenu>(
        menu,
        inventory,
        title,
        FILTER_SLOTS,
        listOf(
            filterTab({ menu.be.filterMode }, { menu.be.filterMode = it }),
            controlTab({ menu.be.controlMode }, { menu.be.controlMode = it }),
            allowTab("item", HopperItemMode.DENY, HopperItemMode.ALLOW, { menu.be.hopperItemMode }, { menu.be.hopperItemMode = it }),
            allowTab("xp", HopperXpMode.DENY, HopperXpMode.ALLOW, { menu.be.hopperXpMode }, { menu.be.hopperXpMode = it }),
            allowTab("nbt", HopperNBTMode.DENY, HopperNBTMode.ALLOW, { menu.be.hopperNBTMode }, { menu.be.hopperNBTMode = it }),
            allowTab("fluid", HopperFluidMode.DENY, HopperFluidMode.ALLOW, { menu.be.hopperFluidMode }, { menu.be.hopperFluidMode = it }),
            rangeTab("hopper", { menu.be.hopperRangeMode }, { menu.be.hopperRangeMode = it }),
        ),
    )

class MagnetScreen(menu: NetMagnetMenu, inventory: Inventory, title: Component) :
    DeviceScreen<NetMagnetMenu>(menu, inventory, title, FILTER_SLOTS, magnetTabs(menu.menuStack))

private fun magnetTabs(stack: ItemStack): List<ModeTab<*>> {
    val filter = stack.setting(BDDataComponents.FILTER_MODE, com.wintercogs.beyonddimensions.common.machine.FilterMode.BLACK)
    val control = stack.setting(BDDataComponents.CONTROL_MODE, com.wintercogs.beyonddimensions.common.machine.RedStoneControlMode.IGNORE)
    val item = stack.setting(BDDataComponents.HOPPER_ITEM_MODE, HopperItemMode.ALLOW)
    val xp = stack.setting(BDDataComponents.HOPPER_XP_MODE, HopperXpMode.DENY)
    val nbt = stack.setting(BDDataComponents.HOPPER_NBT_MODE, HopperNBTMode.DENY)
    val fluid = stack.setting(BDDataComponents.HOPPER_FLUID_MODE, HopperFluidMode.DENY)
    val range = stack.setting(BDDataComponents.HOPPER_RANGE_MODE, com.wintercogs.beyonddimensions.common.machine.HopperRangeMode.RADIUS_LOWEST)
    return listOf(
        filterTab(filter.first, filter.second),
        controlTab(control.first, control.second),
        allowTab("item", HopperItemMode.DENY, HopperItemMode.ALLOW, item.first, item.second),
        allowTab("xp", HopperXpMode.DENY, HopperXpMode.ALLOW, xp.first, xp.second),
        allowTab("nbt", HopperNBTMode.DENY, HopperNBTMode.ALLOW, nbt.first, nbt.second),
        allowTab("fluid", HopperFluidMode.DENY, HopperFluidMode.ALLOW, fluid.first, fluid.second),
        rangeTab("magnet", range.first, range.second),
    )
}

class FeederScreen(menu: NetFeederMenu, inventory: Inventory, title: Component) :
    DeviceScreen<NetFeederMenu>(menu, inventory, title, FILTER_SLOTS, feederTabs(menu.menuStack))

private fun feederTabs(stack: ItemStack): List<ModeTab<*>> {
    val mode = stack.setting(BDDataComponents.FEEDER_MODE, com.wintercogs.beyonddimensions.common.machine.FeederMode.NORMAL)
    val control = stack.setting(BDDataComponents.CONTROL_MODE, com.wintercogs.beyonddimensions.common.machine.RedStoneControlMode.IGNORE)
    return listOf(feederTab(mode.first, mode.second), controlTab(control.first, control.second))
}

class RestockerScreen(menu: NetRestockerMenu, inventory: Inventory, title: Component) :
    DeviceScreen<NetRestockerMenu>(menu, inventory, title, "menu.label.beyonddimensions.restock_slots", restockerTabs(menu.menuStack))

private fun restockerTabs(stack: ItemStack): List<ModeTab<*>> {
    val fuzzy = stack.setting(BDDataComponents.FUZZY_MODE, com.wintercogs.beyonddimensions.common.machine.FuzzyMode.DISABLE)
    val receive = stack.setting(BDDataComponents.RECEIVE_MODE, com.wintercogs.beyonddimensions.common.machine.ReceiveMode.STOP)
    val control = stack.setting(BDDataComponents.CONTROL_MODE, com.wintercogs.beyonddimensions.common.machine.RedStoneControlMode.IGNORE)
    return listOf(fuzzyTab(fuzzy.first, fuzzy.second), receiveTab(receive.first, receive.second), controlTab(control.first, control.second))
}

class InterfaceScreen(menu: NetInterfaceBaseMenu, inventory: Inventory, title: Component) :
    DeviceScreen<NetInterfaceBaseMenu>(
        menu,
        inventory,
        title,
        "menu.label.beyonddimensions.tag_and_stored_slots",
        listOf(
            popTab({ menu.access.popMode }, { menu.access.popMode = it }, { menu.access.canConfigurePopMode() }, POP_UNAVAILABLE),
            controlTab({ menu.access.controlMode }, { menu.access.controlMode = it }),
            fuzzyTab({ menu.access.fuzzyMode }, { menu.access.fuzzyMode = it }),
        ),
    )
