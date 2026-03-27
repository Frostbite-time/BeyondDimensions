package com.wintercogs.beyonddimensions.client.init;

import com.mojang.blaze3d.platform.InputConstants;
import com.wintercogs.beyonddimensions.api.ButtonState;
import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.client.gui.MagnetToggleType;
import com.wintercogs.beyonddimensions.client.gui.NetMenuType;
import com.wintercogs.beyonddimensions.common.init.BDPackets;
import com.wintercogs.beyonddimensions.config.CommonConfigRuntime;
import com.wintercogs.beyonddimensions.network.packet.c2s.*;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import oshi.util.tuples.Pair;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = BDConstants.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class BDShortKeys
{
    private static final List<Pair<KeyMapping, Runnable>> KEY_MAPPINGS_WITH_CALLBACK = new ArrayList<>();
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

    public static final KeyMapping TOGGLE_MAGNET_ITEM_KEY = new KeyMapping(
            "key.beyonddimensions.toggle_magnet_item_key",
            InputConstants.UNKNOWN.getValue(),
            "key.categories.beyonddimensions"
    );

    public static final KeyMapping TOGGLE_MAGNET_FLUID_KEY = new KeyMapping(
            "key.beyonddimensions.toggle_magnet_fluid_key",
            InputConstants.UNKNOWN.getValue(),
            "key.categories.beyonddimensions"
    );

    public static final KeyMapping OPEN_MAGNET_GUI_KEY = new KeyMapping(
            "key.beyonddimensions.open_magnet_gui_key",
            InputConstants.UNKNOWN.getValue(),
            "key.categories.beyonddimensions"
    );

    public static final KeyMapping OPEN_PRIMARY_NET_SWITCHER_KEY = new KeyMapping(
            "key.beyonddimensions.open_primary_net_switcher_key",
            GLFW.GLFW_KEY_U,
            "key.categories.beyonddimensions"
    );

    public static final KeyMapping CYCLE_PRIMARY_NET_KEY = new KeyMapping(
            "key.beyonddimensions.cycle_primary_net_key",
            GLFW.GLFW_KEY_RIGHT_BRACKET,
            "key.categories.beyonddimensions"
    );

    public static void processKeyInput()
    {
        for (Pair<KeyMapping, Runnable> pair : KEY_MAPPINGS_WITH_CALLBACK)
        {
            KeyMapping keyMapping = pair.getA();
            Runnable runnable = pair.getB();
            while (keyMapping.consumeClick())
            {
                runnable.run();
            }
        }
    }

    public static void registerKey(KeyMapping keyMapping)
    {
        KEY_MAPPINGS.add(keyMapping);
    }

    public static void registerKey(KeyMapping keyMapping, Runnable runnable)
    {
        KEY_MAPPINGS.add(keyMapping);
        KEY_MAPPINGS_WITH_CALLBACK.add(new Pair<>(keyMapping, runnable));
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event)
    {
        BDShortKeys.registerKey(OPEN_GUI_KEY, () -> {
            LocalPlayer player = Minecraft.getInstance().player;

            if (player == null)
            {
                return;
            }

            if (CommonConfigRuntime.uiCraftButton == ButtonState.ENABLED)
            {
                BDPackets.INSTANCE.sendToServer(new OpenNetGuiPacket(player.getStringUUID(), NetMenuType.NET_CRAFT_MENU));
            }
            else if (CommonConfigRuntime.uiCraftButton == ButtonState.DISABLED)
            {
                BDPackets.INSTANCE.sendToServer(new OpenNetGuiPacket(player.getStringUUID(), NetMenuType.NET_MENU));
            }
        });
        BDShortKeys.registerKey(OPEN_TERMINAL_QUICK_KEY, () -> {
            LocalPlayer player = Minecraft.getInstance().player;

            if (player == null)
            {
                return;
            }

            BDPackets.INSTANCE.sendToServer(new OpenNetGuiPacket(player.getStringUUID(), NetMenuType.NET_CRAFT_TERMINAL));
        });
        BDShortKeys.registerKey(MAIN_HAND_ITEM_TRANSFER_KEY, () -> {
            Player player = Minecraft.getInstance().player;
            if (player == null || player.isCreative()) return;
            if (!player.getMainHandItem().isEmpty())
            {
                if (player.isShiftKeyDown())
                {
                    BDPackets.INSTANCE.sendToServer(new PutHandItemToNetPacket(InteractionHand.MAIN_HAND));
                }
            }
            else
            {
                HitResult hit = Minecraft.getInstance().hitResult;
                if (hit == null || hit.getType() != HitResult.Type.BLOCK) return;
                Block targetBlock = player.level().getBlockState(((BlockHitResult) hit).getBlockPos()).getBlock();
                Item targetBlockItem = targetBlock.asItem();
                ItemStack targetStack = new ItemStack(targetBlockItem);
                BDPackets.INSTANCE.sendToServer(new PickBlockFromNetPacket(targetStack));
            }
        });
        BDShortKeys.registerKey(TOGGLE_MAGNET_KEY, () -> {
            LocalPlayer player = Minecraft.getInstance().player;

            if (player == null)
            {
                return;
            }

            BDPackets.INSTANCE.sendToServer(new ToggleMagnetPacket(MagnetToggleType.ALL));
        });
        BDShortKeys.registerKey(TOGGLE_MAGNET_ITEM_KEY, () -> {
            LocalPlayer player = Minecraft.getInstance().player;

            if (player == null)
            {
                return;
            }

            BDPackets.INSTANCE.sendToServer(new ToggleMagnetPacket(MagnetToggleType.ITEM));
        });
        BDShortKeys.registerKey(TOGGLE_MAGNET_FLUID_KEY, () -> {
            LocalPlayer player = Minecraft.getInstance().player;

            if (player == null)
            {
                return;
            }

            BDPackets.INSTANCE.sendToServer(new ToggleMagnetPacket(MagnetToggleType.FLUID));
        });
        BDShortKeys.registerKey(OPEN_MAGNET_GUI_KEY, () -> {
            LocalPlayer player = Minecraft.getInstance().player;

            if (player == null)
            {
                return;
            }

            BDPackets.INSTANCE.sendToServer(new OpenMagnetGuiPacket());
        });
        BDShortKeys.registerKey(OPEN_PRIMARY_NET_SWITCHER_KEY, () -> {
            LocalPlayer player = Minecraft.getInstance().player;

            if (player == null)
            {
                return;
            }

            BDPackets.INSTANCE.sendToServer(new OpenPrimaryNetSwitcherPacket());
        });
        BDShortKeys.registerKey(CYCLE_PRIMARY_NET_KEY, () -> {
            LocalPlayer player = Minecraft.getInstance().player;

            if (player == null)
            {
                return;
            }

            BDPackets.INSTANCE.sendToServer(new PrimaryNetSwitchActionPacket(com.wintercogs.beyonddimensions.api.dimensionnet.PrimaryNetSwitchAction.CYCLE_NEXT, -1));
        });

        for (KeyMapping keyMapping : KEY_MAPPINGS)
        {
            event.register(keyMapping);
        }
    }
}
