package com.wintercogs.beyonddimensions.Registry;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.GUI.*;
import com.wintercogs.beyonddimensions.Menu.*;
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
        NetInterfaceBaseMenu.MENU_TYPES.register(eventBus);
        NetEnergyMenu.MENU_TYPES.register(eventBus);
        DimensionsCraftMenu.MENU_TYPES.register(eventBus);
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event)
    {
        // 显式说明类型，防止gradle无法识别泛型
        event.<DimensionsNetMenu, DimensionsNetGUI<DimensionsNetMenu>>register(DimensionsNetMenu.Dimensions_Net_Menu.get(), DimensionsNetGUI::new);
        event.register(NetControlMenu.Net_Control_Menu.get(), NetControlGUI::new);
        event.register(NetInterfaceBaseMenu.Net_Interface_Menu.get(), NetInterfaceBaseGUI::new);
        event.register(NetEnergyMenu.Net_Energy_Menu.get(), NetEnergyGUI::new);
        event.register(DimensionsCraftMenu.Dimensions_Craft_Menu.get(), DimensionsCraftGUI::new);
    }
}
