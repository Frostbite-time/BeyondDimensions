package com.wintercogs.beyonddimensions.EventListener;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;

public final class ReloadHooks
{

    @EventBusSubscriber(modid = BeyondDimensions.MODID, value = Dist.CLIENT)
    public static final class ReloadHooksClient
    {
        @SubscribeEvent
        public static void onRecipesUpdated(net.neoforged.neoforge.client.event.RecipesUpdatedEvent e)
        {
            if (BeyondDimensions.Botania_Loaded)
            {
//                ManaPoolPathwayBlockEntity.onRecipesReloaded();
            }
        }
    }

    @EventBusSubscriber(modid = BeyondDimensions.MODID)
    public static final class ReloadHooksCommon
    {
        @SubscribeEvent
        public static void onDataPackSync(OnDatapackSyncEvent e)
        {
            if (BeyondDimensions.Botania_Loaded)
            {
//                ManaPoolPathwayBlockEntity.onRecipesReloaded();
            }
        }
    }
}