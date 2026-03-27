package com.wintercogs.beyonddimensions.common.item;

import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class NetedItem extends Item
{
    public NetedItem(Properties properties)
    {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand)
    {
        ItemStack itemstack = player.getItemInHand(usedHand);
        if (usedHand != InteractionHand.MAIN_HAND || !player.isShiftKeyDown())
        {
            return InteractionResultHolder.fail(itemstack);
        }
        if (!level.isClientSide())
        {
            if (setNet(itemstack, player))
            {
                return InteractionResultHolder.sidedSuccess(itemstack, level.isClientSide());
            }
            else
            {
                return InteractionResultHolder.fail(itemstack);
            }
        }
        return InteractionResultHolder.sidedSuccess(itemstack, level.isClientSide());
    }

    @Override
    public void onCraftedBy(ItemStack stack, Level level, Player player)
    {
        super.onCraftedBy(stack, level, player);

        if (level.isClientSide())
            return;

        DimensionsNet net = DimensionsNet.getNetFromPlayer(player);
        if (net != null)
        {
            setNet(stack, player);
        }
    }

    public static DimensionsNet getNet(ItemStack stack)
    {
        int netId = getNetId(stack);
        if (netId >= 0)
        {
            return DimensionsNet.getNetFromId(netId);
        }
        return null;
    }

    public static boolean setNet(ItemStack itemstack, Player player)
    {
        if (itemstack.getItem() instanceof NetedItem item)
        {
            Level level = player.level();
            int netId = getNetId(itemstack);
            if (netId >= 0)
            {
                DimensionsNet itemNet = DimensionsNet.getNetFromId(netId);
                if (itemNet != null && !item.validToReWrite(itemNet, player))
                {
                    player.sendSystemMessage(Component.translatable("msg.beyonddimensions.no_right_to_bound_item"));
                    return false;
                }

                setNetId(itemstack, -1);
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.8F, 1.0F);
                player.sendSystemMessage(Component.translatable("msg.beyonddimensions.item_net_unbound", netId));
                return true;
            }

            DimensionsNet playerNet = DimensionsNet.getNetFromPlayer(player);
            if (playerNet != null && item.validToReWrite(playerNet, player))
            {
                setNetId(itemstack, playerNet.getId());
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.8F, 1.0F);
                player.sendSystemMessage(Component.translatable("msg.beyonddimensions.item_net_bound", playerNet.getId()));
                return true;
            }

            player.sendSystemMessage(Component.translatable("msg.beyonddimensions.no_right_to_bound_item"));
            return false;
        }
        return false;
    }

    // 可以通过这个方法获取存储的 NetId
    public static int getNetId(ItemStack stack)
    {
        if (stack.hasTag() && stack.getTag().contains("NetId"))
        {
            return stack.getTag().getInt("NetId");
        }
        return -1;
    }

    public static void setNetId(ItemStack stack, int netId)
    {
        stack.getOrCreateTag().putInt("NetId", netId);
    }

    protected boolean validToReWrite(DimensionsNet net, Player player)
    {
        return net.isManager(player);
    }
}
