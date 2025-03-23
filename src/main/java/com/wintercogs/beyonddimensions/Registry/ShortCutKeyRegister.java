package com.wintercogs.beyonddimensions.Registry;


import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.ShortCutKey.DimensionsShortKeys;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = BeyondDimensions.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
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
