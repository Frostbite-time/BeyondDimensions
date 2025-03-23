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
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;


@Mod.EventBusSubscriber(modid = BeyondDimensions.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class UIRegister
{

    public static void register(IEventBus eventBus)
    {
        DimensionsNetMenu.MENU_TYPES.register(eventBus);
        NetControlMenu.MENU_TYPES.register(eventBus);
        NetInterfaceBaseMenu.MENU_TYPES.register(eventBus);
        NetEnergyMenu.MENU_TYPES.register(eventBus);
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event)
    {
        event.register(DimensionsNetMenu.Dimensions_Net_Menu.get(), DimensionsNetGUI::new);
        event.register(NetControlMenu.Net_Control_Menu.get(), NetControlGUI::new);
        event.register(NetInterfaceBaseMenu.Net_Interface_Menu.get(), NetInterfaceBaseGUI::new);
        event.register(NetEnergyMenu.Net_Energy_Menu.get(), NetEnergyGUI::new);
    }
}
