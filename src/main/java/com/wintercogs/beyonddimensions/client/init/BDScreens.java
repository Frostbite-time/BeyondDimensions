package com.wintercogs.beyonddimensions.client.init;

import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.client.gui.*;
import com.wintercogs.beyonddimensions.common.menu.DimensionsCraftMenu;
import com.wintercogs.beyonddimensions.common.menu.DimensionsCraftMenuTerminal;
import com.wintercogs.beyonddimensions.common.menu.DimensionsNetMenu;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import static com.wintercogs.beyonddimensions.common.init.BDMenus.*;

@Mod.EventBusSubscriber(modid = BDConstants.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class BDScreens
{
    @SubscribeEvent
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
                    MenuScreens.register(Net_Restocker_Menu.get(), NetRestockerGUI::new);
                }
        );
    }
}
