package com.wintercogs.beyonddimensions.Item.Custom;

import com.wintercogs.beyonddimensions.DataBase.DimensionsNet;
import com.wintercogs.beyonddimensions.Item.Interface.IAddNetMemberHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;

public class NetManagerInviter extends NetedItem implements IAddNetMemberHandler {

    // 1.12.2 使用无参构造 + 属性设置方法
    public NetManagerInviter() {
        this.setMaxStackSize(1); // 设置最大堆叠数
        this.setMaxDamage(0);    // 设置不可损坏
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        super.onItemRightClick(world, player, hand);
        ItemStack itemstack = player.getHeldItem(hand);

        // 只响应主手操作
        if (hand != EnumHand.MAIN_HAND) {
            return new ActionResult<>(EnumActionResult.FAIL, itemstack);
        }

        // 服务端逻辑
        if (!world.isRemote) {
            // 获取玩家当前网络
            DimensionsNet currentNet = DimensionsNet.getNetFromPlayer(player);

            // 验证玩家是否没有网络
            if (currentNet == null) {
                int netId = NetedItem.getNetId(itemstack);
                if (netId >= 0) {
                    // 1.12.2 需要显式获取主世界
                    DimensionsNet targetNet = DimensionsNet.getNetFromId(
                            netId,
                            DimensionManager.getWorld(0) // 主世界维度
                    );

                    if (AddPlayerToNet(targetNet, player)) {
                        itemstack.shrink(1); // 消耗物品
                        return new ActionResult<>(EnumActionResult.SUCCESS, itemstack);
                    }
                }
            }
        }
        return new ActionResult<>(EnumActionResult.PASS, itemstack);
    }

    // 权限验证方法
    @Override
    protected boolean validToReWrite(DimensionsNet net, EntityPlayer player) {
        return net != null && net.isOwner(player.getUniqueID()); // 1.12.2 使用 UUID 验证
    }

    // 添加成员实现
    @Override
    public boolean AddPlayerToNet(DimensionsNet net, EntityPlayer player) {
        if (net != null && validToReWrite(net, player)) {
            // 1.12.2 需要验证玩家UUID有效性
            if (!net.getManagers().contains(player.getUniqueID())) {
                net.addManager(player.getUniqueID());
                net.markDirty(); // 标记数据需要保存
                return true;
            }
        }
        return false;
    }
}
