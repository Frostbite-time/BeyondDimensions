package com.wintercogs.beyonddimensions;

import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.client.init.BDBlockRenders;
import com.wintercogs.beyonddimensions.integration.IntegrationManager;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = BDConstants.MODID, dist = Dist.CLIENT)
public class BeyondDimensionsClient
{

    public BeyondDimensionsClient(IEventBus modEventBus, ModContainer container)
    {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        modEventBus.register(this);
        modEventBus.addListener(BDBlockRenders::onRegisterRenderers);

        // 分发集成模块
        IntegrationManager.bootstrapClient(modEventBus, NeoForge.EVENT_BUS);
    }

    @SubscribeEvent
    public void onClientSetup(FMLClientSetupEvent event)
    {
        event.enqueueWork(() -> {
            BeyondDimensions.LOGGER.info("维度网络初始化完成(客户端)");
        });
    }
}
