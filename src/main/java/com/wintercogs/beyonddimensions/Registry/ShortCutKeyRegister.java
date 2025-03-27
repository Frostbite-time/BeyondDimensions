package com.wintercogs.beyonddimensions.Registry;


import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.ShortCutKey.DimensionsShortKeys;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = BeyondDimensions.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ShortCutKeyRegister
{
    private static final List<KeyBinding> KEY_MAPPINGS = new ArrayList<>();

    public static void registerKey(KeyBinding keyMapping)
    {
        KEY_MAPPINGS.add(keyMapping);
    }


    public static void registerKeys()
    {
        DimensionsShortKeys.register();
        for (KeyBinding key : KEY_MAPPINGS)
        {
            ClientRegistry.registerKeyBinding(key);
        }
    }
}
