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
            BeyondDimensions.LOGGER.info("Beyond Dimensions initialized (client)");
        });
    }
}
