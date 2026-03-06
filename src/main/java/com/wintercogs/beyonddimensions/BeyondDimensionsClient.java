package com.wintercogs.beyonddimensions;

import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.client.init.BDBlockRenders;
import com.wintercogs.beyonddimensions.integration.IntegrationManager;
import com.wintercogs.beyonddimensions.integration.module.botania.HudOverlay.ManaPoolPathwayOverlay;
import com.wintercogs.beyonddimensions.integration.module.polymorph.PolymorphPlug;
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

        IntegrationManager.bootstrapClient(modEventBus, NeoForge.EVENT_BUS);
    }

    @SubscribeEvent
    public void onClientSetup(FMLClientSetupEvent event)
    {
        event.enqueueWork(() -> {
            // 一些客户端初始代码
            BeyondDimensions.LOGGER.info("维度网络初始化完成(客户端)");


            if (BeyondDimensions.PolymorphLoaded)
            {
                PolymorphPlug.register();
            }
            if (BeyondDimensions.Botania_Loaded)
            {
                NeoForge.EVENT_BUS.register(ManaPoolPathwayOverlay.class);
            }
        });
    }
}
