package com.wintercogs.beyonddimensions.client.init;

import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.client.ui.machine.*;
import com.wintercogs.beyonddimensions.client.ui.network.NetControlScreen;
import com.wintercogs.beyonddimensions.client.ui.network.PrimaryNetScreen;
import com.wintercogs.beyonddimensions.client.ui.storage.InterfaceScreen;
import com.wintercogs.beyonddimensions.client.ui.storage.StorageScreen;
import com.wintercogs.beyonddimensions.common.init.BDMenus;
import com.wintercogs.beyonddimensions.common.menu.*;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = BDConstants.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class BDScreens
{
    @SubscribeEvent
    public static void registerScreens(FMLClientSetupEvent event)
    {
        event.enqueueWork(() -> {
            // 显式说明类型，防止gradle无法识别泛型；三种存储菜单共用同一个界面
            MenuScreens.<DimensionsNetMenu, StorageScreen>register(BDMenus.Dimensions_Net_Menu.get(), StorageScreen::new);
            MenuScreens.<DimensionsNetMenu, StorageScreen>register(BDMenus.Dimensions_Craft_Menu.get(), StorageScreen::new);
            MenuScreens.<DimensionsNetMenu, StorageScreen>register(BDMenus.Dimensions_Craft_Menu_Terminal.get(), StorageScreen::new);
            MenuScreens.<NetInterfaceBaseMenu, InterfaceScreen>register(BDMenus.Net_Interface_Menu.get(), InterfaceScreen::new);
            MenuScreens.<NetEnergyMenu, EnergyScreen>register(BDMenus.Net_Energy_Menu.get(), EnergyScreen::new);
            MenuScreens.<NetPumpMenu, PumpScreen>register(BDMenus.Net_Pump_Menu.get(), PumpScreen::new);
            MenuScreens.<NetHopperMenu, HopperScreen>register(BDMenus.Net_Hopper_Menu.get(), HopperScreen::new);
            MenuScreens.<NetFurnaceMenu, FurnaceScreen>register(BDMenus.Net_Furnace_Menu.get(), FurnaceScreen::new);
            MenuScreens.<NetMagnetMenu, MagnetScreen>register(BDMenus.Net_Magnet_Menu.get(), MagnetScreen::new);
            MenuScreens.<NetFeederMenu, FeederScreen>register(BDMenus.Net_Feeder_Menu.get(), FeederScreen::new);
            MenuScreens.<NetRestockerMenu, RestockerScreen>register(BDMenus.Net_Restocker_Menu.get(), RestockerScreen::new);
            MenuScreens.<NetStockEmitterMenu, StockEmitterScreen>register(BDMenus.Net_Stock_Emitter_Menu.get(), StockEmitterScreen::new);
            MenuScreens.<XpExchangeMenu, XpExchangeScreen>register(BDMenus.Xp_Exchange_Menu.get(), XpExchangeScreen::new);
            MenuScreens.<NetControlMenu, NetControlScreen>register(BDMenus.Net_Control_Menu.get(), NetControlScreen::new);
            MenuScreens.<PrimaryNetSwitcherMenu, PrimaryNetScreen>register(BDMenus.Primary_Net_Switcher_Menu.get(), PrimaryNetScreen::new);
        });
    }
}
