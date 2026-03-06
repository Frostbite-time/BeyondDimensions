package com.wintercogs.beyonddimensions.integration.module.botania.eventlistener;

import com.wintercogs.beyonddimensions.integration.module.botania.block.entity.ManaPoolPathwayBlockEntity;
import net.minecraftforge.client.event.RecipesUpdatedEvent;

public class BotaniaModuleRecipesUpdatedListener
{
    public static void onRecipesUpdated(RecipesUpdatedEvent event)
    {
        ManaPoolPathwayBlockEntity.onRecipesReloaded();
    }
}
