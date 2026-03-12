package com.wintercogs.beyonddimensions.client.init;

import com.mojang.blaze3d.platform.InputConstants;
import com.wintercogs.beyonddimensions.api.ButtonState;
import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.client.gui.MagnetToggleType;
import com.wintercogs.beyonddimensions.client.gui.NetMenuType;
import com.wintercogs.beyonddimensions.config.CommonConfigRuntime;
import com.wintercogs.beyonddimensions.network.packet.c2s.OpenMagnetGuiPacket;
import com.wintercogs.beyonddimensions.network.packet.c2s.OpenNetGuiPacket;
import com.wintercogs.beyonddimensions.network.packet.c2s.PickBlockFromNetPacket;
import com.wintercogs.beyonddimensions.network.packet.c2s.PutHandItemToNetPacket;
import com.wintercogs.beyonddimensions.network.packet.c2s.ToggleMagnetPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = BDConstants.MODID, value = Dist.CLIENT)
public class BDShortKeys
{
    private record KeyMappingHandler(KeyMapping keyMapping, Runnable runnable)
    {
    }

    private static final List<KeyMappingHandler> KEY_MAPPINGS_WITH_CALLBACK = new ArrayList<>();
    private static final List<KeyMapping> KEY_MAPPINGS = new ArrayList<>();
    public static final KeyMapping.Category BDShortCutKeyCategory = new KeyMapping.Category(Identifier.fromNamespaceAndPath(BDConstants.MODID, "short_keys"));

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

    public static final KeyMapping TOGGLE_MAGNET_ITEM_KEY = new KeyMapping(
            "key.beyonddimensions.toggle_magnet_item_key",
            InputConstants.UNKNOWN.getValue(),
            BDShortCutKeyCategory
    );

    public static final KeyMapping TOGGLE_MAGNET_FLUID_KEY = new KeyMapping(
            "key.beyonddimensions.toggle_magnet_fluid_key",
            InputConstants.UNKNOWN.getValue(),
            BDShortCutKeyCategory
    );

    public static final KeyMapping OPEN_MAGNET_GUI_KEY = new KeyMapping(
            "key.beyonddimensions.open_magnet_gui_key",
            InputConstants.UNKNOWN.getValue(),
            BDShortCutKeyCategory
    );

    public static void processKeyInput()
    {
        for (KeyMappingHandler handler : KEY_MAPPINGS_WITH_CALLBACK)
        {
            while (handler.keyMapping().consumeClick())
            {
                handler.runnable().run();
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
        KEY_MAPPINGS_WITH_CALLBACK.add(new KeyMappingHandler(keyMapping, runnable));
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
                ClientPacketDistributor.sendToServer(new OpenNetGuiPacket(player.getStringUUID(), NetMenuType.NET_CRAFT_MENU));
            }
            else if (CommonConfigRuntime.uiCraftButton == ButtonState.DISABLED)
            {
                ClientPacketDistributor.sendToServer(new OpenNetGuiPacket(player.getStringUUID(), NetMenuType.NET_MENU));
            }
        });
        BDShortKeys.registerKey(OPEN_TERMINAL_QUICK_KEY, () -> {
            LocalPlayer player = Minecraft.getInstance().player;

            if (player == null)
            {
                return;
            }

            ClientPacketDistributor.sendToServer(new OpenNetGuiPacket(player.getStringUUID(), NetMenuType.NET_CRAFT_TERMINAL));
        });
        BDShortKeys.registerKey(MAIN_HAND_ITEM_TRANSFER_KEY, () -> {
            Player player = Minecraft.getInstance().player;
            if (player == null || player.isCreative())
            {
                return;
            }

            if (!player.getMainHandItem().isEmpty())
            {
                if (player.isShiftKeyDown())
                {
                    ClientPacketDistributor.sendToServer(new PutHandItemToNetPacket(InteractionHand.MAIN_HAND));
                }
                return;
            }

            HitResult hit = Minecraft.getInstance().hitResult;
            if (hit == null || hit.getType() != HitResult.Type.BLOCK)
            {
                return;
            }

            Block targetBlock = player.level().getBlockState(((BlockHitResult) hit).getBlockPos()).getBlock();
            Item targetBlockItem = targetBlock.asItem();
            ItemStack targetStack = new ItemStack(targetBlockItem);
            ClientPacketDistributor.sendToServer(new PickBlockFromNetPacket(targetStack));
        });
        BDShortKeys.registerKey(TOGGLE_MAGNET_KEY, () -> {
            LocalPlayer player = Minecraft.getInstance().player;

            if (player == null)
            {
                return;
            }

            ClientPacketDistributor.sendToServer(new ToggleMagnetPacket(MagnetToggleType.ALL));
        });
        BDShortKeys.registerKey(TOGGLE_MAGNET_ITEM_KEY, () -> {
            LocalPlayer player = Minecraft.getInstance().player;

            if (player == null)
            {
                return;
            }

            ClientPacketDistributor.sendToServer(new ToggleMagnetPacket(MagnetToggleType.ITEM));
        });
        BDShortKeys.registerKey(TOGGLE_MAGNET_FLUID_KEY, () -> {
            LocalPlayer player = Minecraft.getInstance().player;

            if (player == null)
            {
                return;
            }

            ClientPacketDistributor.sendToServer(new ToggleMagnetPacket(MagnetToggleType.FLUID));
        });
        BDShortKeys.registerKey(OPEN_MAGNET_GUI_KEY, () -> {
            LocalPlayer player = Minecraft.getInstance().player;

            if (player == null)
            {
                return;
            }

            ClientPacketDistributor.sendToServer(new OpenMagnetGuiPacket());
        });
        for (KeyMapping keyMapping : KEY_MAPPINGS)
        {
            event.register(keyMapping);
        }
    }
}
