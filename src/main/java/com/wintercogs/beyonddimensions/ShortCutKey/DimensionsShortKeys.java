package com.wintercogs.beyonddimensions.ShortCutKey;

import com.wintercogs.beyonddimensions.BeyondDimensions;
//import com.wintercogs.beyonddimensions.Network.Packet.toServer.OpenNetGuiPacket;
import com.wintercogs.beyonddimensions.Registry.PacketRegister;
import com.wintercogs.beyonddimensions.Registry.ShortCutKeyRegister;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;


@Mod.EventBusSubscriber(modid = BeyondDimensions.MODID, value = Side.CLIENT)
public class DimensionsShortKeys
{

    private static final KeyBinding OPEN_GUI_KEY = new KeyBinding(
            "key.beyonddimensions.open_gui", // 键位描述
            Keyboard.KEY_O,                 // 默认按键 "O"
            "key.categories.beyonddimensions" // 键位分类
    );

    public static void register()
    {
        ShortCutKeyRegister.registerKey(OPEN_GUI_KEY);
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public static void onKeyInput(TickEvent.ClientTickEvent event)
    {
        // 用if或者switch，随便什么，反正检查按键就行
        if (OPEN_GUI_KEY.isPressed())
        {
            EntityPlayer player = Minecraft.getMinecraft().player;

            if (player == null)
            {
                return;
            }

            //PacketRegister.INSTANCE.sendToServer(new OpenNetGuiPacket(player.getUniqueID().toString()));
        }

    }

}