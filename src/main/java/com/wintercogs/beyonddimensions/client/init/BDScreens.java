package com.wintercogs.beyonddimensions.client.init;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.client.gui.*;
import com.wintercogs.beyonddimensions.common.menu.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = BeyondDimensions.MODID, value = Dist.CLIENT)
public class BDScreens
{
    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event)
    {
        // 显式说明类型，防止gradle无法识别泛型
        event.<DimensionsNetMenu, DimensionsNetGUI<DimensionsNetMenu>>register(DimensionsNetMenu.Dimensions_Net_Menu.get(), DimensionsNetGUI::new);
        event.register(NetControlMenu.Net_Control_Menu.get(), NetControlGUI::new);
        event.register(NetInterfaceBaseMenu.Net_Interface_Menu.get(), NetInterfaceBaseGUI::new);
        event.register(NetEnergyMenu.Net_Energy_Menu.get(), NetEnergyGUI::new);
        event.<DimensionsCraftMenu, DimensionsCraftGUI<DimensionsCraftMenu>>register(DimensionsCraftMenu.Dimensions_Craft_Menu.get(), DimensionsCraftGUI::new);
        event.register(DimensionsCraftMenuTerminal.Dimensions_Craft_Menu_Terminal.get(), DimensionsTerminalCraftGUI::new);
        event.register(NetPumpMenu.Net_Pump_Menu.get(), NetPumpGUI::new);
        event.register(NetHopperMenu.Net_Hopper_Menu.get(), NetHopperGUI::new);
        event.register(NetFurnaceMenu.Net_Furnace_Menu.get(), NetFurnaceGUI::new);
        event.register(NetMagnetMenu.Net_Magnet_Menu.get(), NetMagnetGUI::new);
        event.register(NetFeederMenu.Net_Feeder_Menu.get(), NetFeederGUI::new);
        event.register(NetRestockerMenu.Net_Restocker_Menu.get(), NetRestockerGUI::new);
    }
}
