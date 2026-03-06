package com.wintercogs.beyonddimensions.integration.module.botania.eventlistener;

import com.wintercogs.beyonddimensions.integration.module.botania.block.entity.ManaPoolPathwayBlockEntity;
import net.minecraftforge.event.OnDatapackSyncEvent;

public class BotaniaModuleDataPackSyncListener
{
    public static void onDataPackSync(OnDatapackSyncEvent event)
    {
        ManaPoolPathwayBlockEntity.onRecipesReloaded();
    }
}
