package com.wintercogs.beyonddimensions.integration.module.botania;

import com.wintercogs.beyonddimensions.integration.BDIntegrationClientModule;
import com.wintercogs.beyonddimensions.integration.IIntegrationClientModule;
import com.wintercogs.beyonddimensions.integration.OtherModIds;
import com.wintercogs.beyonddimensions.integration.module.botania.eventlistener.BotaniaModuleRecipesUpdatedListener;
import com.wintercogs.beyonddimensions.integration.module.botania.init.BotaniaModuleBlockRenders;
import com.wintercogs.beyonddimensions.integration.module.botania.overlay.ManaPoolPathwayOverlay;
import net.minecraftforge.client.event.RecipesUpdatedEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@BDIntegrationClientModule(modId = OtherModIds.BOTANIA)
public class BotaniaClientModule implements IIntegrationClientModule
{
    @Override
    public String modId()
    {
        return OtherModIds.BOTANIA;
    }

    @Override
    public void onBootstrapClient(IEventBus modBus, IEventBus gameBus)
    {
        modBus.addListener(BotaniaModuleBlockRenders::onRegisterRenderers);
        gameBus.addListener(BotaniaModuleRecipesUpdatedListener::onRecipesUpdated);
        gameBus.addListener(ManaPoolPathwayOverlay::onRenderGui);
    }

    @Override
    public void onClientSetup(FMLClientSetupEvent event)
    {
    }
}
