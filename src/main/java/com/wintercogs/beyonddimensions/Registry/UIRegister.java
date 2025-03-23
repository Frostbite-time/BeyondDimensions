package com.wintercogs.beyonddimensions.Registry;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.GUI.DimensionsNetGUI;
import com.wintercogs.beyonddimensions.GUI.NetControlGUI;
import com.wintercogs.beyonddimensions.GUI.NetEnergyGUI;
import com.wintercogs.beyonddimensions.GUI.NetInterfaceBaseGUI;
import com.wintercogs.beyonddimensions.Menu.DimensionsNetMenu;
import com.wintercogs.beyonddimensions.Menu.NetControlMenu;
import com.wintercogs.beyonddimensions.Menu.NetEnergyMenu;
import com.wintercogs.beyonddimensions.Menu.NetInterfaceBaseMenu;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.registries.DeferredRegister;


@Mod.EventBusSubscriber(modid = BeyondDimensions.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class UIRegister
{
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(Registries.MENU,BeyondDimensions.MODID);

    public static void register(IEventBus eventBus)
    {
        MENU_TYPES.register(eventBus);
    }


    public static void registerScreens(FMLClientSetupEvent event)
    {
        event.enqueueWork(

                () -> {
                    MenuScreens.register(DimensionsNetMenu.Dimensions_Net_Menu.get(), DimensionsNetGUI::new);
                    MenuScreens.register(NetControlMenu.Net_Control_Menu.get(), NetControlGUI::new);
                    MenuScreens.register(NetInterfaceBaseMenu.Net_Interface_Menu.get(), NetInterfaceBaseGUI::new);
                    MenuScreens.register(NetEnergyMenu.Net_Energy_Menu.get(), NetEnergyGUI::new);
                }
        );
    }
}
