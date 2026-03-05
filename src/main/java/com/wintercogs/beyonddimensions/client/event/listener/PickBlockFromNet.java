package com.wintercogs.beyonddimensions.client.event.listener;

import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.client.init.BDShortKeys;
import com.wintercogs.beyonddimensions.network.packet.PickBlockFromNetPacket;
import com.wintercogs.beyonddimensions.network.packet.PutHandItemToNetPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

@EventBusSubscriber(modid = BDConstants.MODID, value = Dist.CLIENT)
public class PickBlockFromNet
{
    @SubscribeEvent
    public static void pickOrPutBlockFromNetMouse(ClientTickEvent.Post event)
    {
        while (BDShortKeys.MAIN_HAND_ITEM_TRANSFER_KEY.consumeClick())
        {
            Player player = Minecraft.getInstance().player;
            if (player == null || player.isCreative()) return; // 不影响原版创造模式
            if (!player.getMainHandItem().isEmpty())
            {
                if (player.isShiftKeyDown())
                {
                    ClientPacketDistributor.sendToServer(new PutHandItemToNetPacket(InteractionHand.MAIN_HAND));
                }

            }
            else
            {
                HitResult hit = Minecraft.getInstance().hitResult;
                if (hit == null || hit.getType() != HitResult.Type.BLOCK) return;
                Block targetBlock = player.level().getBlockState(((BlockHitResult) hit).getBlockPos()).getBlock();
                Item targetBlockItem = targetBlock.asItem();
                ItemStack targetStack = new ItemStack(targetBlockItem);
                ClientPacketDistributor.sendToServer(new PickBlockFromNetPacket(targetStack));
            }
        }
    }
}
