package com.wintercogs.beyonddimensions.Menu;

import com.wintercogs.beyonddimensions.Api.DataBase.DimensionsNet;
import com.wintercogs.beyonddimensions.Item.Custom.NetTerminalItem;
import com.wintercogs.beyonddimensions.Registry.UIRegister;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class DimensionsCraftMenuTerminal extends DimensionsCraftMenu
{
    private ItemStack terminalStack = null;
    private BlockPos entityPos = null;


    public DimensionsCraftMenuTerminal(int id, Inventory playerInventory)
    {
        this(id, playerInventory,  new DimensionsNet(true), null, null, null);
    }

    public DimensionsCraftMenuTerminal(int id, Inventory playerInventory, DimensionsNet data, NonNullList<ItemStack> craftItems, ItemStack terminalItem, BlockPos entityPos)
    {
        super(UIRegister.Dimensions_Craft_Menu_Terminal.get(), id,playerInventory,data, craftItems,entityPos);
        if(!player.level().isClientSide)
        {
            this.terminalStack = terminalItem;
            this.entityPos = entityPos;
        }
    }

    @Override
    protected void initCraftSlots(Inventory playerInventory, TransientCraftingContainer craftSlots)
    {
        super.initCraftSlots(playerInventory, craftSlots);
        // 父函数处理完毕后更新一次结果槽
        DimensionsCraftMenu.slotChangedCraftingGrid(this,player.level(),player,craftSlots,resultSlots,resultSlotIndex);
    }

    @Override
    public void removed(Player player)
    {
        // 处理光标物品
        if (player instanceof ServerPlayer) {
            ItemStack itemstack = this.getCarried();
            if (!itemstack.isEmpty()) {
                if (player.isAlive() && !((ServerPlayer)player).hasDisconnected()) {
                    player.getInventory().placeItemBackInInventory(itemstack);
                } else {
                    player.drop(itemstack, false);
                }

                this.setCarried(ItemStack.EMPTY);
            }
        }

        if(player instanceof ServerPlayer)
        {
            // 处理合成槽物品
            NonNullList<ItemStack> nonNullList = NonNullList.withSize(9, ItemStack.EMPTY);
            for (int i = 0; i < craftSlots.getItems().size(); i++) {
                ItemStack stack = craftSlots.getItems().get(i);
                nonNullList.set(i, stack);
            }
            if(terminalStack != null && terminalStack.getItem() instanceof NetTerminalItem)
            {
                // 将数据写入物品的 NBT
                CompoundTag tag = terminalStack.getOrCreateTag();
                ListTag slotsTag = new ListTag();
                for (ItemStack stack : nonNullList) {
                    CompoundTag itemTag = new CompoundTag();
                    if (!stack.isEmpty()) {
                        stack.save(itemTag); // 非空物品序列化为 CompoundTag
                    }
                    slotsTag.add(itemTag); // 空物品也会保存为空的 CompoundTag
                }
                tag.put("craft_slots", slotsTag); // 存储到 NBT
                terminalStack.setTag(tag); // 回写至 ItemStack
                // 同步更新玩家手中的物品
                player.getInventory().setChanged();
            }


        }

    }

    @Override
    public boolean stillValid(Player player)
    {
        if(entityPos != null)
        {
            BlockEntity be = player.level().getBlockEntity(entityPos);
            return be != null && !be.isRemoved();
        }
        else
        {
            return terminalStack != null && !terminalStack.isEmpty();
        }
    }
}
