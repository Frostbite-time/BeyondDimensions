package com.wintercogs.beyonddimensions.client.init;

import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.client.ui.machine.*;
import com.wintercogs.beyonddimensions.client.ui.network.NetControlScreen;
import com.wintercogs.beyonddimensions.client.ui.network.PrimaryNetScreen;
import com.wintercogs.beyonddimensions.client.ui.storage.InterfaceScreen;
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
        // 显式说明类型，防止gradle无法识别泛型；三种存储菜单共用同一个界面
        event.<DimensionsNetMenu, StorageScreen>register(DimensionsNetMenu.Dimensions_Net_Menu.get(), StorageScreen::new);
        event.<DimensionsNetMenu, StorageScreen>register(DimensionsCraftMenu.Dimensions_Craft_Menu.get(), StorageScreen::new);
        event.<DimensionsNetMenu, StorageScreen>register(DimensionsCraftMenuTerminal.Dimensions_Craft_Menu_Terminal.get(), StorageScreen::new);
        event.<NetInterfaceBaseMenu, InterfaceScreen>register(NetInterfaceBaseMenu.Net_Interface_Menu.get(), InterfaceScreen::new);
        event.<NetEnergyMenu, EnergyScreen>register(NetEnergyMenu.Net_Energy_Menu.get(), EnergyScreen::new);
        event.<NetPumpMenu, PumpScreen>register(NetPumpMenu.Net_Pump_Menu.get(), PumpScreen::new);
        event.<NetHopperMenu, HopperScreen>register(NetHopperMenu.Net_Hopper_Menu.get(), HopperScreen::new);
        event.<NetFurnaceMenu, FurnaceScreen>register(NetFurnaceMenu.Net_Furnace_Menu.get(), FurnaceScreen::new);
        event.<NetMagnetMenu, MagnetScreen>register(NetMagnetMenu.Net_Magnet_Menu.get(), MagnetScreen::new);
        event.<NetFeederMenu, FeederScreen>register(NetFeederMenu.Net_Feeder_Menu.get(), FeederScreen::new);
        event.<NetRestockerMenu, RestockerScreen>register(NetRestockerMenu.Net_Restocker_Menu.get(), RestockerScreen::new);
        event.<XpExchangeMenu, XpExchangeScreen>register(XpExchangeMenu.XP_EXCHANGE_MENU.get(), XpExchangeScreen::new);
        event.<NetControlMenu, NetControlScreen>register(NetControlMenu.Net_Control_Menu.get(), NetControlScreen::new);
        event.<PrimaryNetSwitcherMenu, PrimaryNetScreen>register(PrimaryNetSwitcherMenu.PRIMARY_NET_SWITCHER_MENU.get(), PrimaryNetScreen::new);
    }
}
