package com.wintercogs.beyonddimensions.client.init;

import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.client.gui.*;
import com.wintercogs.beyonddimensions.client.ui.device.*;
import com.wintercogs.beyonddimensions.client.ui.network.ControlScreen;
import com.wintercogs.beyonddimensions.client.ui.network.PrimaryScreen;
import com.wintercogs.beyonddimensions.client.ui.storage.CraftScreen;
import com.wintercogs.beyonddimensions.client.ui.storage.StorageScreen;
import com.wintercogs.beyonddimensions.common.menu.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = BDConstants.MODID, value = Dist.CLIENT)
public class BDScreens
{
    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event)
    {
        // 显式说明类型，防止gradle无法识别泛型
        event.<DimensionsNetMenu, StorageScreen<DimensionsNetMenu>>register(DimensionsNetMenu.Dimensions_Net_Menu.get(), StorageScreen::new);
        event.register(NetControlMenu.Net_Control_Menu.get(), ControlScreen::new);
        event.register(NetInterfaceBaseMenu.Net_Interface_Menu.get(), InterfaceScreen::new);
        event.register(NetEnergyMenu.Net_Energy_Menu.get(), EnergyScreen::new);
        event.<DimensionsCraftMenu, CraftScreen<DimensionsCraftMenu>>register(DimensionsCraftMenu.Dimensions_Craft_Menu.get(), CraftScreen::new);
        event.<DimensionsCraftMenuTerminal, CraftScreen<DimensionsCraftMenuTerminal>>register(DimensionsCraftMenuTerminal.Dimensions_Craft_Menu_Terminal.get(), CraftScreen::new);
        event.register(NetPumpMenu.Net_Pump_Menu.get(), PumpScreen::new);
        event.register(NetHopperMenu.Net_Hopper_Menu.get(), HopperScreen::new);
        event.register(NetFurnaceMenu.Net_Furnace_Menu.get(), FurnaceScreen::new);
        event.register(NetMagnetMenu.Net_Magnet_Menu.get(), MagnetScreen::new);
        event.register(NetFeederMenu.Net_Feeder_Menu.get(), FeederScreen::new);
        event.register(NetRestockerMenu.Net_Restocker_Menu.get(), RestockerScreen::new);
        event.register(XpExchangeMenu.XP_EXCHANGE_MENU.get(), XpScreen::new);
        event.register(PrimaryNetSwitcherMenu.PRIMARY_NET_SWITCHER_MENU.get(), PrimaryScreen::new);
    }
}
