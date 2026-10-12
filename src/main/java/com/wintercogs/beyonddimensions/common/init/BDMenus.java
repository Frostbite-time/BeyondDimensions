package com.wintercogs.beyonddimensions.common.init;

import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.common.menu.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class BDMenus
{
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(Registries.MENU, BDConstants.MODID);
    public static final Supplier<MenuType<DimensionsNetMenu>> Dimensions_Net_Menu = MENU_TYPES.register("dimensions_net_menu", () -> IMenuTypeExtension.create(DimensionsNetMenu::new));
    public static final Supplier<MenuType<DimensionsCraftMenu>> Dimensions_Craft_Menu = MENU_TYPES.register("dimensions_craft_menu", () -> IMenuTypeExtension.create(DimensionsCraftMenu::new));
    public static final Supplier<MenuType<NetControlMenu>> Net_Control_Menu = MENU_TYPES.register("net_control_menu", () -> IMenuTypeExtension.create(NetControlMenu::new));
    public static final Supplier<MenuType<NetEnergyMenu>> Net_Energy_Menu = MENU_TYPES.register("net_energy_menu", () -> IMenuTypeExtension.create(NetEnergyMenu::new));
    public static final Supplier<MenuType<NetInterfaceBaseMenu>> Net_Interface_Menu = MENU_TYPES.register("net_interface_menu", () -> IMenuTypeExtension.create(NetInterfaceBaseMenu::fromNetwork));
    public static final Supplier<MenuType<DimensionsCraftMenuTerminal>> Dimensions_Craft_Menu_Terminal = MENU_TYPES.register("dimensions_craft_menu_terminal", () -> IMenuTypeExtension.create(DimensionsCraftMenuTerminal::new));
    public static final Supplier<MenuType<NetPumpMenu>> Net_Pump_Menu = MENU_TYPES.register("net_pump_menu", () -> IMenuTypeExtension.create(NetPumpMenu::new));
    public static final Supplier<MenuType<NetHopperMenu>> Net_Hopper_Menu = MENU_TYPES.register("net_hopper_menu", () -> IMenuTypeExtension.create(NetHopperMenu::new));
    public static final Supplier<MenuType<NetFurnaceMenu>> Net_Furnace_Menu = MENU_TYPES.register("net_furnace_menu", () -> IMenuTypeExtension.create(NetFurnaceMenu::new));
    public static final Supplier<MenuType<NetMagnetMenu>> Net_Magnet_Menu = MENU_TYPES.register("net_magnet_menu", () -> IMenuTypeExtension.create(NetMagnetMenu::new));
    public static final Supplier<MenuType<NetFeederMenu>> Net_Feeder_Menu = MENU_TYPES.register("net_feeder_menu", () -> IMenuTypeExtension.create(NetFeederMenu::new));
    public static final Supplier<MenuType<NetRestockerMenu>> Net_Restocker_Menu = MENU_TYPES.register("net_restocker_menu", () -> IMenuTypeExtension.create(NetRestockerMenu::new));
    public static final Supplier<MenuType<XpExchangeMenu>> Xp_Exchange_Menu = MENU_TYPES.register("xp_exchange_menu", () -> IMenuTypeExtension.create(XpExchangeMenu::new));
    public static final Supplier<MenuType<PrimaryNetSwitcherMenu>> Primary_Net_Switcher_Menu = MENU_TYPES.register("primary_net_switcher_menu", () -> IMenuTypeExtension.create(PrimaryNetSwitcherMenu::new));

    public static void register(IEventBus eventBus)
    {
        MENU_TYPES.register(eventBus);
    }
}
