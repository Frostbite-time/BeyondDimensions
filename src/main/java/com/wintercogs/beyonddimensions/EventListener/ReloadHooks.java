package com.wintercogs.beyonddimensions.EventListener;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.Integration.Botania.Block.ManaPoolPathwayBlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

public final class ReloadHooks {

    // Client 侧
    @Mod.EventBusSubscriber(modid = BeyondDimensions.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class ReloadHooksClient {
        @SubscribeEvent
        public static void onRecipesUpdated(net.minecraftforge.client.event.RecipesUpdatedEvent e) {
            if (BeyondDimensions.Botania_Loaded) {
                ManaPoolPathwayBlockEntity.onRecipesReloaded();
            }
        }
    }

    // 通用（会在服务端/集成服触发）
    @Mod.EventBusSubscriber(modid = BeyondDimensions.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class ReloadHooksCommon {
        @SubscribeEvent
        public static void onDataPackSync(OnDatapackSyncEvent e) {
            if (BeyondDimensions.Botania_Loaded) {
                ManaPoolPathwayBlockEntity.onRecipesReloaded();
            }
        }
    }
}
