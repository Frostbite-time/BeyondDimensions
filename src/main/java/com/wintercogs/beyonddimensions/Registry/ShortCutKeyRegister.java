package com.wintercogs.beyonddimensions.Registry;


import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.ShortCutKey.DimensionsShortKeys;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = BeyondDimensions.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ShortCutKeyRegister
{
    private static final List<KeyMapping> KEY_MAPPINGS = new ArrayList<>();

    public static void registerKey(KeyMapping keyMapping)
    {
        KEY_MAPPINGS.add(keyMapping);
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event)
    {
        DimensionsShortKeys.register();
        for (KeyMapping keyMapping : KEY_MAPPINGS)
        {
            event.register(keyMapping);
        }
    }
}
