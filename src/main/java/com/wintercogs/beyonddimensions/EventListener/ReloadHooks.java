package com.wintercogs.beyonddimensions.EventListener;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.Integration.Botania.Block.ManaPoolPathwayBlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RecipesUpdatedEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;

@EventBusSubscriber(modid = BeyondDimensions.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class ReloadHooks {

    // 配方或者数据包加载时递增版本
    @SubscribeEvent
    public static void onRecipesUpdated(RecipesUpdatedEvent e)
    {
        if(BeyondDimensions.Botania_Loaded)
            ManaPoolPathwayBlockEntity.onRecipesReloaded();
    }

    @SubscribeEvent
    public static void onDataPackSync(OnDatapackSyncEvent e)
    {
        if(BeyondDimensions.Botania_Loaded)
            ManaPoolPathwayBlockEntity.onRecipesReloaded();
    }
}