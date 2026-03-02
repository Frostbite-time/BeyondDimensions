package com.wintercogs.beyonddimensions.Item.Custom;

import com.mojang.logging.LogUtils;
import com.wintercogs.beyonddimensions.Api.DataBase.DimensionsNet;
import com.wintercogs.beyonddimensions.common.init.BDDataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

public class NetedItem extends Item
{
    public NetedItem(Properties properties)
    {
        super(properties.component(BDDataComponents.NET_ID_DATA, -1));
    }

    public static final Logger LOGGER = LogUtils.getLogger();


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

    public static @Nullable DimensionsNet getNet(ItemStack stack)
    {
        int netId = stack.getOrDefault(BDDataComponents.NET_ID_DATA, -1);
        if (netId >= 0)
        {
            return DimensionsNet.getNetFromId(netId);
        }
        return null;
    }

    // 返回值表示是否成功进行修改操作
    public static boolean setNet(ItemStack itemstack, DimensionsNet net, Player player)
    {
        // 确保仅对网络化物品赋值
        if (itemstack.getItem() instanceof NetedItem item)
        {
            Level level = player.level();
            if (item.validToReWrite(net, player))
            {
                if (itemstack.getOrDefault(BDDataComponents.NET_ID_DATA, -1) != net.getId())
                {
                    itemstack.set(BDDataComponents.NET_ID_DATA, net.getId());
                    level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.8F, 1.0F);
                    player.sendSystemMessage(Component.translatable("msg.beyonddimensions.item_net_bound", net.getId()));
                }
                else
                {
                    itemstack.set(BDDataComponents.NET_ID_DATA, -1);
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

    // 覆写此方法以实现自定义网络覆写规则
    protected boolean validToReWrite(DimensionsNet net, Player player)
    {
        return net.isManager(player);
    }
}
