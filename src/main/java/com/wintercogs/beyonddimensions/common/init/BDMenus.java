package com.wintercogs.beyonddimensions.common.init;

import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.common.menu.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class BDMenus
{
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(Registries.MENU, BDConstants.MODID);
    public static final Supplier<MenuType<DimensionsNetMenu>> Dimensions_Net_Menu = MENU_TYPES.register("dimensions_net_menu", () -> IForgeMenuType.create(DimensionsNetMenu::new));
    public static final Supplier<MenuType<DimensionsCraftMenu>> Dimensions_Craft_Menu = MENU_TYPES.register("dimensions_craft_menu", () -> IForgeMenuType.create(DimensionsCraftMenu::new));
    public static final Supplier<MenuType<NetControlMenu>> Net_Control_Menu = MENU_TYPES.register("net_control_menu", () -> IForgeMenuType.create(NetControlMenu::new));
    public static final Supplier<MenuType<NetEnergyMenu>> Net_Energy_Menu = MENU_TYPES.register("net_energy_menu", () -> IForgeMenuType.create(NetEnergyMenu::new));
    public static final Supplier<MenuType<NetInterfaceBaseMenu>> Net_Interface_Menu = MENU_TYPES.register("net_interface_menu", () -> IForgeMenuType.create(NetInterfaceBaseMenu::fromNetwork));
    public static final Supplier<MenuType<DimensionsCraftMenuTerminal>> Dimensions_Craft_Menu_Terminal = MENU_TYPES.register("dimensions_craft_menu_terminal", () -> IForgeMenuType.create(DimensionsCraftMenuTerminal::new));
    public static final Supplier<MenuType<NetPumpMenu>> Net_Pump_Menu = MENU_TYPES.register("net_pump_menu", () -> IForgeMenuType.create(NetPumpMenu::new));
    public static final Supplier<MenuType<NetHopperMenu>> Net_Hopper_Menu = MENU_TYPES.register("net_hopper_menu", () -> IForgeMenuType.create(NetHopperMenu::new));
    public static final Supplier<MenuType<NetFurnaceMenu>> Net_Furnace_Menu = MENU_TYPES.register("net_furnace_menu", () -> IForgeMenuType.create(NetFurnaceMenu::new));
    public static final Supplier<MenuType<NetMagnetMenu>> Net_Magnet_Menu = MENU_TYPES.register("net_magnet_menu", () -> IForgeMenuType.create(NetMagnetMenu::new));
    public static final Supplier<MenuType<NetFeederMenu>> Net_Feeder_Menu = MENU_TYPES.register("net_feeder_menu", () -> IForgeMenuType.create(NetFeederMenu::new));
    public static final Supplier<MenuType<NetRestockerMenu>> Net_Restocker_Menu = MENU_TYPES.register("net_restocker_menu", () -> IForgeMenuType.create(NetRestockerMenu::new));
    public static final Supplier<MenuType<NetStockEmitterMenu>> Net_Stock_Emitter_Menu = MENU_TYPES.register("net_stock_emitter_menu", () -> IForgeMenuType.create(NetStockEmitterMenu::new));
    public static final Supplier<MenuType<XpExchangeMenu>> Xp_Exchange_Menu = MENU_TYPES.register("xp_exchange_menu", () -> IForgeMenuType.create(XpExchangeMenu::new));
    public static final Supplier<MenuType<PrimaryNetSwitcherMenu>> Primary_Net_Switcher_Menu = MENU_TYPES.register("primary_net_switcher_menu", () -> IForgeMenuType.create(PrimaryNetSwitcherMenu::new));

    public static void register(IEventBus eventBus)
    {
        MENU_TYPES.register(eventBus);
    }
}
