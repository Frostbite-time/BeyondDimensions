package com.wintercogs.beyonddimensions.Item.Custom;

import com.wintercogs.beyonddimensions.DataBase.DimensionsNet;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.CompoundTag;
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
        if (usedHand != InteractionHand.MAIN_HAND) {
            return InteractionResultHolder.fail(itemstack);
        }
        if (!level.isClientSide()) {
            DimensionsNet net = DimensionsNet.getNetFromPlayer(player);
            if (net != null) {
                if (validToReWrite(net, player)) {
                    // 改用 NBT 标签存储数据
                    CompoundTag tag = itemstack.getOrCreateTag();
                    int currentNetId = tag.getInt("NetId");

                    if (currentNetId != net.getId()) {
                        tag.putInt("NetId", net.getId());
                    } else {
                        tag.putInt("NetId", -1);
                    }
                } else {
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
    protected boolean validToReWrite(DimensionsNet net, EntityPlayer player) {
        return net.isManager(player);
    }
}
