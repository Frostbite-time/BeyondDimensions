package com.wintercogs.beyonddimensions.Item.Custom;

import com.wintercogs.beyonddimensions.Api.DataBase.DimensionsNet;
import net.minecraft.nbt.CompoundTag;
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
    public NetedItem(Properties properties) {
        super(properties);
    }
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack itemstack = player.getItemInHand(usedHand);
        if (usedHand != InteractionHand.MAIN_HAND || !player.isShiftKeyDown()) {
            return InteractionResultHolder.fail(itemstack);
        }
        if (!level.isClientSide()) {
            DimensionsNet net = DimensionsNet.getNetFromPlayer(player);
            if (net != null) {
                if (validToReWrite(net, player)) {
                    // 改用 NBT 标签存储数据
                    CompoundTag tag = itemstack.getOrCreateTag();

                    if (getNetId(itemstack) != net.getId()) {
                        tag.putInt("NetId", net.getId());
                        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS,0.8F,1.0F);
                        player.sendSystemMessage(Component.translatable("msg.beyonddimensions.item_net_bound",net.getId()));
                    } else {
                        tag.putInt("NetId", -1);
                        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS,0.8F,1.0F);
                        player.sendSystemMessage(Component.translatable("msg.beyonddimensions.item_net_unbound",net.getId()));
                    }
                } else {
                    player.sendSystemMessage(Component.translatable("msg.beyonddimensions.no_right_to_bound_item"));
                    return InteractionResultHolder.fail(itemstack);
                }
            } else {
                return InteractionResultHolder.fail(itemstack);
            }
        }
        return InteractionResultHolder.sidedSuccess(itemstack, level.isClientSide());
    }
    // 可以通过这个方法获取存储的 NetId
    public static int getNetId(ItemStack stack) {
        if (stack.hasTag() && stack.getTag().contains("NetId")) {
            return stack.getTag().getInt("NetId");
        }
        return -1;
    }
    protected boolean validToReWrite(DimensionsNet net, Player player) {
        return net.isManager(player);
    }
}
