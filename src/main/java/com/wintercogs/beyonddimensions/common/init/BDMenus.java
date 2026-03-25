package com.wintercogs.beyonddimensions.common.init;

import com.wintercogs.beyonddimensions.common.menu.*;
import net.neoforged.bus.api.IEventBus;

public class BDMenus
{
    public static void register(IEventBus eventBus)
    {
        DimensionsNetMenu.MENU_TYPES.register(eventBus);
        NetControlMenu.MENU_TYPES.register(eventBus);
        NetInterfaceBaseMenu.MENU_TYPES.register(eventBus);
        NetEnergyMenu.MENU_TYPES.register(eventBus);
        DimensionsCraftMenu.MENU_TYPES.register(eventBus);
        DimensionsCraftMenuTerminal.MENU_TYPES.register(eventBus);
        NetPumpMenu.MENU_TYPES.register(eventBus);
        NetHopperMenu.MENU_TYPES.register(eventBus);
        NetFurnaceMenu.MENU_TYPES.register(eventBus);
        NetMagnetMenu.MENU_TYPES.register(eventBus);
        NetFeederMenu.MENU_TYPES.register(eventBus);
        NetRestockerMenu.MENU_TYPES.register(eventBus);
        XpExchangeMenu.MENU_TYPES.register(eventBus);
    }
}
