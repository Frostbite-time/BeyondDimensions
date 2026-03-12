package com.wintercogs.beyonddimensions.client.event.listener;

import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.client.init.BDShortKeys;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BDConstants.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ShortKeysListener
{
    @SubscribeEvent
    public static void onKeyInput(TickEvent.ClientTickEvent event)
    {
        BDShortKeys.processKeyInput();
    }

}
