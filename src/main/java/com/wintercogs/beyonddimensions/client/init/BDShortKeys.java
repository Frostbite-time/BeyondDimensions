package com.wintercogs.beyonddimensions.client.init;

import com.mojang.blaze3d.platform.InputConstants;
import com.wintercogs.beyonddimensions.BeyondDimensions;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = BeyondDimensions.MODID, value = Dist.CLIENT)
public class BDShortKeys
{
    private static final List<KeyMapping> KEY_MAPPINGS = new ArrayList<>();
    public static final KeyMapping.Category BDShortCutKeyCategory = new KeyMapping.Category(Identifier.fromNamespaceAndPath(BeyondDimensions.MODID, "short_keys"));

    public static final KeyMapping OPEN_GUI_KEY = new KeyMapping(
            "key.beyonddimensions.open_gui", // 键位描述
            GLFW.GLFW_KEY_O,                 // 默认按键 "O"
            BDShortCutKeyCategory
    );

    public static final KeyMapping OPEN_TERMINAL_QUICK_KEY = new KeyMapping(
            "key.beyonddimensions.open_terminal_quick_key",
            GLFW.GLFW_KEY_P,
            BDShortCutKeyCategory
    );

    public static final KeyMapping MAIN_HAND_ITEM_TRANSFER_KEY = new KeyMapping(
            "key.beyonddimensions.main_hand_item_transfer_key",
            InputConstants.Type.MOUSE,
            GLFW.GLFW_MOUSE_BUTTON_MIDDLE,
            BDShortCutKeyCategory
    );

    public static final KeyMapping TOGGLE_MAGNET_KEY = new KeyMapping(
            "key.beyonddimensions.toggle_magnet_key",
            GLFW.GLFW_KEY_LEFT_BRACKET, // 对应[
            BDShortCutKeyCategory
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
