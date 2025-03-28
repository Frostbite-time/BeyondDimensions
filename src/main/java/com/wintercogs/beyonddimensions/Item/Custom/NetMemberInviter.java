package com.wintercogs.beyonddimensions.Item.Custom;

import com.wintercogs.beyonddimensions.DataBase.DimensionsNet;
import com.wintercogs.beyonddimensions.Item.Interface.IAddNetMemberHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;

public class NetMemberInviter extends NetedItem implements IAddNetMemberHandler {

    // 1.12.2 物品属性配置
    public NetMemberInviter() {
        this.setMaxStackSize(1);        // 限制堆叠数量
        this.setMaxDamage(0);           // 设置不可损坏
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack itemstack = player.getHeldItem(hand);

        // 仅响应主手操作
        if (hand != EnumHand.MAIN_HAND) {
            return new ActionResult<>(EnumActionResult.FAIL, itemstack);
        }

        // 服务端逻辑处理
        if (!world.isRemote) {
            // 获取目标网络ID
            int netId = NetedItem.getNetId(itemstack);

            // 有效性验证
            if (netId >= 0) {
                // 获取主世界维度实例
                WorldServer mainWorld = DimensionManager.getWorld(0);
                DimensionsNet targetNet = DimensionsNet.getNetFromId(netId, mainWorld);

                // 执行添加操作
                if (AddPlayerToNet(targetNet, player)) {
                    itemstack.shrink(1); // 消耗物品
                    return new ActionResult<>(EnumActionResult.SUCCESS, itemstack);
                }
            }
        }

        return new ActionResult<>(EnumActionResult.PASS, itemstack);
    }

    @Override
    public boolean AddPlayerToNet(DimensionsNet net, EntityPlayer player) {
        // 空值防御
        if (net == null || player == null) return false;

        // 执行添加逻辑
        net.addPlayer(player.getUniqueID());
        net.markDirty(); // 标记数据需要保存
        return true;
    }

}
