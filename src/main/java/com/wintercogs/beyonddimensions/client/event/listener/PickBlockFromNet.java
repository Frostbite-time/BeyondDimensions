package com.wintercogs.beyonddimensions.client.event.listener;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.network.Packet.c2s.PickBlockFromNetPacket;
import com.wintercogs.beyonddimensions.network.Packet.c2s.PutHandItemToNetPacket;
import com.wintercogs.beyonddimensions.Registry.PacketRegister;
import com.wintercogs.beyonddimensions.ShortCutKey.DimensionsShortKeys;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BeyondDimensions.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class PickBlockFromNet
{
    @SubscribeEvent
    public static void pickOrPutBlockFromNetMouse(TickEvent.ClientTickEvent event)
    {
        while (DimensionsShortKeys.MAIN_HAND_ITEM_TRANSFER_KEY.consumeClick())
        {
            Player player = Minecraft.getInstance().player;
            if (player == null || player.isCreative()) return; // 不影响原版创造模式
            if (!player.getMainHandItem().isEmpty())
            {
                if (player.isShiftKeyDown())
                {
                    PacketRegister.INSTANCE.sendToServer(new PutHandItemToNetPacket(InteractionHand.MAIN_HAND));
                }

            }
            else
            {
                HitResult hit = Minecraft.getInstance().hitResult;
                if (hit == null || hit.getType() != HitResult.Type.BLOCK) return;
                Block targetBlock = player.level().getBlockState(((BlockHitResult) hit).getBlockPos()).getBlock();
                Item targetBlockItem = targetBlock.asItem();
                ItemStack targetStack = new ItemStack(targetBlockItem);
                PacketRegister.INSTANCE.sendToServer(new PickBlockFromNetPacket(targetStack));
            }
        }
    }
}
