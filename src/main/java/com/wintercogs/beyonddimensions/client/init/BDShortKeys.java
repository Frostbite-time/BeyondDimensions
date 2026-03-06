package com.wintercogs.beyonddimensions.client.init;

import com.mojang.blaze3d.platform.InputConstants;
import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = BDConstants.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class BDShortKeys
{
    private static final List<KeyMapping> KEY_MAPPINGS = new ArrayList<>();

    public static final KeyMapping OPEN_GUI_KEY = new KeyMapping(
            "key.beyonddimensions.open_gui", // 键位描述
            GLFW.GLFW_KEY_O,                 // 默认按键 "O"
            "key.categories.beyonddimensions" // 键位分类
    );

    public static final KeyMapping OPEN_TERMINAL_QUICK_KEY = new KeyMapping(
            "key.beyonddimensions.open_terminal_quick_key",
            GLFW.GLFW_KEY_P,
            "key.categories.beyonddimensions"
    );

    public static final KeyMapping MAIN_HAND_ITEM_TRANSFER_KEY = new KeyMapping(
            "key.beyonddimensions.main_hand_item_transfer_key",
            InputConstants.Type.MOUSE,
            GLFW.GLFW_MOUSE_BUTTON_MIDDLE,
            "key.categories.beyonddimensions"
    );

    public static final KeyMapping TOGGLE_MAGNET_KEY = new KeyMapping(
            "key.beyonddimensions.toggle_magnet_key",
            GLFW.GLFW_KEY_LEFT_BRACKET, // 对应[
            "key.categories.beyonddimensions"
    );

    public static void registerKey(KeyMapping keyMapping)
    {
        KEY_MAPPINGS.add(keyMapping);
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event)
    {
        BDShortKeys.registerKey(OPEN_GUI_KEY);
        BDShortKeys.registerKey(OPEN_TERMINAL_QUICK_KEY);
        BDShortKeys.registerKey(MAIN_HAND_ITEM_TRANSFER_KEY);
        BDShortKeys.registerKey(TOGGLE_MAGNET_KEY);
        for (KeyMapping keyMapping : KEY_MAPPINGS)
        {
            event.register(keyMapping);
        }
    }
}
