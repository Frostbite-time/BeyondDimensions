package com.wintercogs.beyonddimensions.Item.Custom;

import com.wintercogs.beyonddimensions.Api.DataBase.DimensionsNet;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
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
            DimensionsNet net = DimensionsNet.getNetFromPlayer(player);
            if (net != null)
            {
                setNet(itemstack, net, player);
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
            setNet(stack, net, player);
        }
    }

    public static DimensionsNet getNet(ItemStack stack, MinecraftServer dataProvider)
    {
        int netId = getNetId(stack);
        if (netId >= 0)
        {
            return DimensionsNet.getNetFromId(netId, dataProvider);
        }
        return null;
    }

    public static boolean setNet(ItemStack itemstack, DimensionsNet net, Player player)
    {
        // 确保仅对网络化物品赋值
        if (itemstack.getItem() instanceof NetedItem item)
        {
            Level level = player.level();
            if (item.validToReWrite(net, player))
            {
                int netId = getNetId(itemstack);
                if (netId != net.getId())
                {
                    setNetId(itemstack, net.getId());
                    level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.8F, 1.0F);
                    player.sendSystemMessage(Component.translatable("msg.beyonddimensions.item_net_bound", net.getId()));
                }
                else
                {
                    setNetId(itemstack, -1);
                    level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.8F, 1.0F);
                    player.sendSystemMessage(Component.translatable("msg.beyonddimensions.item_net_unbound", net.getId()));
                }
                return true;
            }
            else
            {
                player.sendSystemMessage(Component.translatable("msg.beyonddimensions.no_right_to_bound_item"));
                return false;
            }
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
