package com.wintercogs.beyonddimensions.EventListener;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.Integration.Botania.Block.ManaPoolPathwayBlockEntity;
import net.minecraftforge.client.event.RecipesUpdatedEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BeyondDimensions.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
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
