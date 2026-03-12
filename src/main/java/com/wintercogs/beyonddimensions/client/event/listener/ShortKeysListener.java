package com.wintercogs.beyonddimensions.client.event.listener;

import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.client.init.BDShortKeys;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = BDConstants.MODID, value = Dist.CLIENT)
public class ShortKeysListener
{
    @SubscribeEvent
    public static void onKeyInput(ClientTickEvent.Post event)
    {
        BDShortKeys.processKeyInput();
    }
}
