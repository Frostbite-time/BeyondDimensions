package com.wintercogs.beyonddimensions.client.init;

import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.client.ui.kit.BdBackdrop;
import com.wintercogs.beyonddimensions.client.ui.machine.*;
import com.wintercogs.beyonddimensions.client.ui.network.NetControlScreen;
import com.wintercogs.beyonddimensions.client.ui.network.PrimaryNetScreen;
import com.wintercogs.beyonddimensions.client.ui.storage.InterfaceScreen;
import com.wintercogs.beyonddimensions.client.ui.storage.StorageScreen;
import com.wintercogs.beyonddimensions.common.init.BDMenus;
import com.wintercogs.beyonddimensions.common.menu.*;
import dev.compixel.forge.constants.CompixelGuiLayers;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = BDConstants.MODID, value = Dist.CLIENT)
public class BDScreens
{
    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event)
    {
        // 显式说明类型，防止gradle无法识别泛型；三种存储菜单共用同一个界面
        event.<DimensionsNetMenu, StorageScreen>register(BDMenus.Dimensions_Net_Menu.get(), StorageScreen::new);
        event.<DimensionsNetMenu, StorageScreen>register(BDMenus.Dimensions_Craft_Menu.get(), StorageScreen::new);
        event.<DimensionsNetMenu, StorageScreen>register(BDMenus.Dimensions_Craft_Menu_Terminal.get(), StorageScreen::new);
        event.<NetInterfaceBaseMenu, InterfaceScreen>register(BDMenus.Net_Interface_Menu.get(), InterfaceScreen::new);
        event.<NetEnergyMenu, EnergyScreen>register(BDMenus.Net_Energy_Menu.get(), EnergyScreen::new);
        event.<NetPumpMenu, PumpScreen>register(BDMenus.Net_Pump_Menu.get(), PumpScreen::new);
        event.<NetHopperMenu, HopperScreen>register(BDMenus.Net_Hopper_Menu.get(), HopperScreen::new);
        event.<NetFurnaceMenu, FurnaceScreen>register(BDMenus.Net_Furnace_Menu.get(), FurnaceScreen::new);
        event.<NetMagnetMenu, MagnetScreen>register(BDMenus.Net_Magnet_Menu.get(), MagnetScreen::new);
        event.<NetFeederMenu, FeederScreen>register(BDMenus.Net_Feeder_Menu.get(), FeederScreen::new);
        event.<NetRestockerMenu, RestockerScreen>register(BDMenus.Net_Restocker_Menu.get(), RestockerScreen::new);
        event.<XpExchangeMenu, XpExchangeScreen>register(BDMenus.Xp_Exchange_Menu.get(), XpExchangeScreen::new);
        event.<NetControlMenu, NetControlScreen>register(BDMenus.Net_Control_Menu.get(), NetControlScreen::new);
        event.<PrimaryNetSwitcherMenu, PrimaryNetScreen>register(BDMenus.Primary_Net_Switcher_Menu.get(), PrimaryNetScreen::new);
    }

    @SubscribeEvent
    public static void registerLayers(RegisterGuiLayersEvent event)
    {
        // 在 CompixelUI 的退场层之下：BD 界面关闭后、退场播放期间继续模糊背后的画面
        event.registerBelow(
                CompixelGuiLayers.SCREEN_EXITS,
                ResourceLocation.fromNamespaceAndPath(BDConstants.MODID, "backdrop_blur"),
                BdBackdrop.INSTANCE.getLayer()
        );
    }
}
