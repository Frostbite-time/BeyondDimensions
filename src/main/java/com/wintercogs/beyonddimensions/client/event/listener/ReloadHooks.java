package com.wintercogs.beyonddimensions.client.event.listener;

import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

public final class ReloadHooks
{

    // Client 侧
    @Mod.EventBusSubscriber(modid = BDConstants.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class ReloadHooksClient
    {
        @SubscribeEvent
        public static void onRecipesUpdated(net.minecraftforge.client.event.RecipesUpdatedEvent e)
        {
        }
    }

    // 通用（会在服务端/集成服触发）
    @Mod.EventBusSubscriber(modid = BDConstants.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class ReloadHooksCommon
    {
        @SubscribeEvent
        public static void onDataPackSync(OnDatapackSyncEvent e)
        {
        }
    }
}
