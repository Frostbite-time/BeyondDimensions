package com.wintercogs.beyonddimensions;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public class BeyondDimensionsClient
{
    public static void clientInit(IEventBus modBus, IEventBus gameBus)
    {
        modBus.addListener(BeyondDimensionsClient::onClientSetup);
    }

    public static void onClientSetup(FMLClientSetupEvent event)
    {
        event.enqueueWork(() -> {
            BeyondDimensions.LOGGER.info("维度网络初始化完成(客户端)");
        });
    }
}
