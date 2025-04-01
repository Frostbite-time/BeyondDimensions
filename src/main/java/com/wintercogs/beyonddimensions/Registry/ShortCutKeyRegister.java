package com.wintercogs.beyonddimensions.Registry;


import com.wintercogs.beyonddimensions.ShortCutKey.DimensionsShortKeys;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.List;

@SideOnly(Side.CLIENT)
public class ShortCutKeyRegister
{
    public static final List<KeyBinding> KEY_MAPPINGS = new ArrayList<>();

    public static void registerKey(KeyBinding keyMapping)
    {
        KEY_MAPPINGS.add(keyMapping);
    }

    public static void registerKeys()
    {

        DimensionsShortKeys.register();
        // 批量注册所有按键
        ShortCutKeyRegister.KEY_MAPPINGS.forEach(ClientRegistry::registerKeyBinding);
    }


}
