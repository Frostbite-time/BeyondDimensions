package com.wintercogs.beyonddimensions.Registry;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.GUI.DimensionsNetGUI;
import com.wintercogs.beyonddimensions.GUI.NetControlGUI;
import com.wintercogs.beyonddimensions.Menu.DimensionsNetMenu;
import com.wintercogs.beyonddimensions.Menu.NetControlMenu;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = BeyondDimensions.MODID, bus = EventBusSubscriber.Bus.MOD)
public class UIRegister
{

    public static void register(IEventBus eventBus)
    {
        DimensionsNetMenu.MENU_TYPES.register(eventBus);
        NetControlMenu.MENU_TYPES.register(eventBus);
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event)
    {
        event.register(DimensionsNetMenu.Dimensions_Net_Menu.get(), DimensionsNetGUI::new);
        event.register(NetControlMenu.Net_Control_Menu.get(), NetControlGUI::new);
    }
}
