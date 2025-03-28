package com.wintercogs.beyonddimensions.Item.Custom;


import com.wintercogs.beyonddimensions.DataBase.DimensionsNet;
import net.minecraft.item.Item;


import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;

public class NetCreater extends Item {

    // 1.12.2 通过构造函数直接设置物品属性
    public NetCreater() {
        this.setMaxStackSize(1); // 设置最大堆叠数
        // this.setMaxDamage(64);  // 可选的耐久设置
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack itemstack = player.getHeldItem(hand);

        // 确保只响应主手操作
        if (hand != EnumHand.MAIN_HAND) {
            return new ActionResult<>(EnumActionResult.FAIL, itemstack);
        }

        // 仅在服务端执行逻辑
        if (!world.isRemote) {
            // 获取玩家现有网络（需要确保 DimensionsNet 已适配 1.12.2）
            DimensionsNet net = DimensionsNet.getNetFromPlayer(player);

            // 如果玩家已有网络则返回失败
            if (net != null) {
                return new ActionResult<>(EnumActionResult.FAIL, itemstack);
            }

            // 生成新网络 ID
            String netId = DimensionsNet.buildNewNetName(player);
            String numId = netId.replace("BDNet_", "");

            // 获取或创建世界存储数据（重要改动！）
            DimensionsNet newNet = (DimensionsNet) world.getMapStorage().getOrLoadData(DimensionsNet.class, netId);

            // 如果数据不存在则创建新实例
            if (newNet == null) {
                newNet = new DimensionsNet(netId);
                world.getMapStorage().setData(netId, newNet);
            }

            // 初始化网络属性
            newNet.setId(Integer.parseInt(numId));
            newNet.setOwner(player.getUniqueID()); // 1.12.2 使用 getUniqueID
            newNet.addManager(player.getUniqueID());
            newNet.addPlayer(player.getUniqueID());
            newNet.markDirty(); // 1.12.2 标记数据需要保存

            // 消耗物品
            itemstack.shrink(1); // 1.12.2 支持 shrink 方法
        }

        return new ActionResult<>(EnumActionResult.SUCCESS, itemstack);
    }
}
