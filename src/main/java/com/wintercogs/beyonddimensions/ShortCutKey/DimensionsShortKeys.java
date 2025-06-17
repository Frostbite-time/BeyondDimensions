package com.wintercogs.beyonddimensions.ShortCutKey;

import com.wintercogs.beyonddimensions.Api.DataBase.ButtonState;
import com.wintercogs.beyonddimensions.Api.DataBase.NetMenuType;
import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.Config;
import com.wintercogs.beyonddimensions.Network.Packet.toServer.OpenNetGuiPacket;
import com.wintercogs.beyonddimensions.Registry.PacketRegister;
import com.wintercogs.beyonddimensions.Registry.ShortCutKeyRegister;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;


@Mod.EventBusSubscriber(modid = BeyondDimensions.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class DimensionsShortKeys
{

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

    public static void register()
    {
        ShortCutKeyRegister.registerKey(OPEN_GUI_KEY);
        ShortCutKeyRegister.registerKey(OPEN_TERMINAL_QUICK_KEY);
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event)
    {
        // 用if或者switch，随便什么，反正检查按键就行
        if (OPEN_GUI_KEY.isDown())
        {
            LocalPlayer player = Minecraft.getInstance().player;

            if (player == null)
            {
                return;
            }

            if(Config.uiCraftButton == ButtonState.ENABLED)
            {
                PacketRegister.INSTANCE.sendToServer(new OpenNetGuiPacket(player.getStringUUID(), NetMenuType.NET_CRAFT_MENU));
            }
            else if(Config.uiCraftButton == ButtonState.DISABLED)
            {
                PacketRegister.INSTANCE.sendToServer(new OpenNetGuiPacket(player.getStringUUID(),NetMenuType.NET_MENU));
            }

        }
        else if(OPEN_TERMINAL_QUICK_KEY.isDown())
        {
            LocalPlayer player = Minecraft.getInstance().player;

            if (player == null)
            {
                return;
            }

            PacketRegister.INSTANCE.sendToServer(new OpenNetGuiPacket(player.getStringUUID(),NetMenuType.NET_CRAFT_TERMINAL));
        }

    }

}