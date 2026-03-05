package com.wintercogs.beyonddimensions.client.event.listener;

import com.wintercogs.beyonddimensions.api.ButtonState;
import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.client.gui.NetMenuType;
import com.wintercogs.beyonddimensions.config.CommonConfigRuntime;
import com.wintercogs.beyonddimensions.network.packet.c2s.OpenNetGuiPacket;
import com.wintercogs.beyonddimensions.network.packet.c2s.ToggleMagnetPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import static com.wintercogs.beyonddimensions.client.init.BDShortKeys.*;

@EventBusSubscriber(modid = BDConstants.MODID, value = Dist.CLIENT)
public class ShortKeysListener
{
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
