package com.wintercogs.beyonddimensions;

import com.wintercogs.beyonddimensions.integration.IntegrationManager;
import com.wintercogs.beyonddimensions.integration.module.botania.overlay.ManaPoolPathwayOverlay;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public class BeyondDimensionsClient
{
    public static void clientInit(IEventBus modBus, IEventBus gameBus)
    {
        modBus.addListener(BeyondDimensionsClient::onClientSetup);
        IntegrationManager.bootstrapClient(modBus, gameBus);
    }

    public static void onClientSetup(FMLClientSetupEvent event)
    {
        event.enqueueWork(() -> {
            // 一些客户端初始代码
            BeyondDimensions.LOGGER.info("维度网络初始化完成(客户端)");

            if (BeyondDimensions.Botania_Loaded)
            {
                MinecraftForge.EVENT_BUS.register(ManaPoolPathwayOverlay.class);
            }
        });
    }
}
