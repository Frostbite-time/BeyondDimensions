package com.wintercogs.beyonddimensions.Item.Custom;

import com.wintercogs.beyonddimensions.DataBase.DimensionsNet;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;

public class NetedItem extends Item {

    // 1.12.2 物品属性通过构造函数直接设置
    public NetedItem() {
        this.setMaxStackSize(1); // 示例：设置最大堆叠数为1
        // this.setMaxDamage(64);  // 如果需要耐久可以这样设置
    }

    // 修改方法签名为 1.12.2 的 onItemRightClick
    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack itemstack = player.getHeldItem(hand);

        // 1.12.2 使用 EnumHand 判断手持位置
        if (hand != EnumHand.MAIN_HAND) {
            return new ActionResult<>(EnumActionResult.FAIL, itemstack);
        }

        // 服务端逻辑判断改为 !world.isRemote
        if (!world.isRemote) {
            DimensionsNet net = DimensionsNet.getNetFromPlayer(player);
            if (net != null) {
                if (validToReWrite(net, player)) {
                    // 1.12.2 的 NBT 操作方式
                    NBTTagCompound tag = itemstack.getTagCompound();
                    if (tag == null) {
                        tag = new NBTTagCompound();
                        itemstack.setTagCompound(tag);
                    }

                    int currentNetId = tag.getInteger("NetId");

                    if (currentNetId != net.getId()) {
                        tag.setInteger("NetId", net.getId());
                    } else {
                        tag.setInteger("NetId", -1);
                    }
                } else {
                    return new ActionResult<>(EnumActionResult.FAIL, itemstack);
                }
            } else {
                return new ActionResult<>(EnumActionResult.FAIL, itemstack);
            }
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, itemstack);
    }

    // 1.12.2 的 NBT 数据读取方法
    public static int getNetId(ItemStack stack) {
        if (stack.hasTagCompound()) {
            NBTTagCompound tag = stack.getTagCompound();
            if (tag.hasKey("NetId")) {
                return tag.getInteger("NetId");
            }
        }
        return -1;
    }

    // 参数类型改为 1.12.2 的 EntityPlayer
    protected boolean validToReWrite(DimensionsNet net, EntityPlayer player) {
        return net.isManager(player);
    }
}

