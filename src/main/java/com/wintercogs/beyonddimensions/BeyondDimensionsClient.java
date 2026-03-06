package com.wintercogs.beyonddimensions;

import com.wintercogs.beyonddimensions.client.init.BDBlockRenders;
import com.wintercogs.beyonddimensions.integration.IntegrationManager;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public class BeyondDimensionsClient
{
    public static void clientInit(IEventBus modBus, IEventBus gameBus)
    {
        modBus.addListener(BeyondDimensionsClient::onClientSetup);
        modBus.addListener(BDBlockRenders::onRegisterRenderers);
        IntegrationManager.bootstrapClient(modBus, gameBus);
    }

    public static void onClientSetup(FMLClientSetupEvent event)
    {
        event.enqueueWork(() -> {
            // 一些客户端初始代码
            BeyondDimensions.LOGGER.info("维度网络初始化完成(客户端)");
        });
    }
}
