package com.wintercogs.beyonddimensions.Registry;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.GUI.*;
import com.wintercogs.beyonddimensions.Menu.*;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.registries.DeferredRegister;

import java.util.function.Supplier;


@Mod.EventBusSubscriber(modid = BeyondDimensions.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class UIRegister
{
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(Registries.MENU,BeyondDimensions.MODID);
    public static final Supplier<MenuType<DimensionsNetMenu>> Dimensions_Net_Menu = UIRegister.MENU_TYPES.register("dimensions_net_menu", ()-> IForgeMenuType.create(DimensionsNetMenu::new));
    public static final Supplier<MenuType<DimensionsCraftMenu>> Dimensions_Craft_Menu = UIRegister.MENU_TYPES.register("dimensions_craft_menu", ()-> IForgeMenuType.create(DimensionsCraftMenu::new));
    public static final Supplier<MenuType<NetControlMenu>> Net_Control_Menu = UIRegister.MENU_TYPES.register("net_control_menu", ()-> IForgeMenuType.create(NetControlMenu::new));
    public static final Supplier<MenuType<NetEnergyMenu>> Net_Energy_Menu = UIRegister.MENU_TYPES.register("net_energy_menu", ()-> IForgeMenuType.create(NetEnergyMenu::new));
    public static final Supplier<MenuType<NetInterfaceBaseMenu>> Net_Interface_Menu = UIRegister.MENU_TYPES.register("net_interface_menu", ()-> IForgeMenuType.create(NetInterfaceBaseMenu::new));
    public static final Supplier<MenuType<DimensionsCraftMenuTerminal>> Dimensions_Craft_Menu_Terminal = UIRegister.MENU_TYPES.register("dimensions_craft_menu_terminal", ()-> IForgeMenuType.create(DimensionsCraftMenuTerminal::new));
    public static final Supplier<MenuType<NetPumpMenu>> Net_Pump_Menu = UIRegister.MENU_TYPES.register("net_pump_menu", ()-> IForgeMenuType.create(NetPumpMenu::new));
    public static final Supplier<MenuType<NetHopperMenu>> Net_Hopper_Menu = UIRegister.MENU_TYPES.register("net_hopper_menu", ()-> IForgeMenuType.create(NetHopperMenu::new));
    public static final Supplier<MenuType<NetFurnaceMenu>> Net_Furnace_Menu = UIRegister.MENU_TYPES.register("net_furnace_menu", ()-> IForgeMenuType.create(NetFurnaceMenu::new));
    public static final Supplier<MenuType<NetMagnetMenu>> Net_Magnet_Menu = UIRegister.MENU_TYPES.register("net_magnet_menu", ()-> IForgeMenuType.create(NetMagnetMenu::new));
    public static final Supplier<MenuType<NetFeederMenu>> Net_Feeder_Menu = UIRegister.MENU_TYPES.register("net_feeder_menu", ()-> IForgeMenuType.create(NetFeederMenu::new));

    public static void register(IEventBus eventBus)
    {
        MENU_TYPES.register(eventBus);
    }


    public static void registerScreens(FMLClientSetupEvent event)
    {
        event.enqueueWork(

                () -> {
                    //显示指定泛型类型
                    MenuScreens.<DimensionsNetMenu, DimensionsNetGUI<DimensionsNetMenu>>register(Dimensions_Net_Menu.get(), DimensionsNetGUI::new);
                    MenuScreens.<DimensionsCraftMenu, DimensionsCraftGUI<DimensionsCraftMenu>>register(Dimensions_Craft_Menu.get(), DimensionsCraftGUI::new);
                    MenuScreens.register(Net_Control_Menu.get(), NetControlGUI::new);
                    MenuScreens.register(Net_Interface_Menu.get(), NetInterfaceBaseGUI::new);
                    MenuScreens.register(Net_Energy_Menu.get(), NetEnergyGUI::new);
                    MenuScreens.<DimensionsCraftMenuTerminal, DimensionsTerminalCraftGUI>register(Dimensions_Craft_Menu_Terminal.get(), DimensionsTerminalCraftGUI::new);
                    MenuScreens.register(Net_Pump_Menu.get(), NetPumpGUI::new);
                    MenuScreens.register(Net_Hopper_Menu.get(), NetHopperGUI::new);
                    MenuScreens.register(Net_Furnace_Menu.get(), NetFurnaceGUI::new);
                    MenuScreens.register(Net_Magnet_Menu.get(), NetMagnetGUI::new);
                    MenuScreens.register(Net_Feeder_Menu.get(), NetFeederGUI::new);
                }
        );
    }
}
