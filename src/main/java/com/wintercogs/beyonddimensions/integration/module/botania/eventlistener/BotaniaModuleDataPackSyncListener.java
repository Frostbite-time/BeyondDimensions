package com.wintercogs.beyonddimensions.integration.module.botania.eventlistener;

import com.wintercogs.beyonddimensions.integration.module.botania.block.entity.ManaPoolPathwayBlockEntity;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;

public class BotaniaModuleDataPackSyncListener
{
    public static void onDataPackSync(OnDatapackSyncEvent e)
    {
        ManaPoolPathwayBlockEntity.onRecipesReloaded();
    }
}
