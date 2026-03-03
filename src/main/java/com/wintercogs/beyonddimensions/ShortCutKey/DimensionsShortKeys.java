package com.wintercogs.beyonddimensions.ShortCutKey;

import com.mojang.blaze3d.platform.InputConstants;
import com.wintercogs.beyonddimensions.Api.DataBase.ButtonState;
import com.wintercogs.beyonddimensions.Api.config.CommonConfigRuntime;
import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.GUI.NetMenuType;
import com.wintercogs.beyonddimensions.Packet.OpenNetGuiPacket;
import com.wintercogs.beyonddimensions.Packet.ToggleMagnetPacket;
import com.wintercogs.beyonddimensions.Registry.ShortCutKeyRegister;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.lwjgl.glfw.GLFW;


@EventBusSubscriber(modid = BeyondDimensions.MODID, value = Dist.CLIENT)
public class DimensionsShortKeys
{
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

    public static void register()
    {
        ShortCutKeyRegister.registerKey(OPEN_GUI_KEY);
        ShortCutKeyRegister.registerKey(OPEN_TERMINAL_QUICK_KEY);
        ShortCutKeyRegister.registerKey(MAIN_HAND_ITEM_TRANSFER_KEY);
        ShortCutKeyRegister.registerKey(TOGGLE_MAGNET_KEY);
    }

    @SubscribeEvent
    public static void onKeyInput(ClientTickEvent.Post event)
    {
        // 用if或者switch，随便什么，反正检查按键就行
        while (OPEN_GUI_KEY.consumeClick())
        {
            LocalPlayer player = Minecraft.getInstance().player;

            if (player == null)
            {
                return;
            }

            if (CommonConfigRuntime.uiCraftButton == ButtonState.ENABLED)
            {
                ClientPacketDistributor.sendToServer(new OpenNetGuiPacket(player.getStringUUID(), NetMenuType.NET_CRAFT_MENU));
            }
            else if (CommonConfigRuntime.uiCraftButton == ButtonState.DISABLED)
            {
                ClientPacketDistributor.sendToServer(new OpenNetGuiPacket(player.getStringUUID(), NetMenuType.NET_MENU));
            }


        }

        while (OPEN_TERMINAL_QUICK_KEY.consumeClick())
        {
            LocalPlayer player = Minecraft.getInstance().player;

            if (player == null)
            {
                return;
            }

            ClientPacketDistributor.sendToServer(new OpenNetGuiPacket(player.getStringUUID(), NetMenuType.NET_CRAFT_TERMINAL));
        }

        while (TOGGLE_MAGNET_KEY.consumeClick())
        {
            LocalPlayer player = Minecraft.getInstance().player;

            if (player == null)
            {
                return;
            }

            ClientPacketDistributor.sendToServer(new ToggleMagnetPacket());
        }

    }

}