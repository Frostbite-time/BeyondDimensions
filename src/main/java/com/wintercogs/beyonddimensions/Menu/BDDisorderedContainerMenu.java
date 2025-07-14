package com.wintercogs.beyonddimensions.Menu;

import com.wintercogs.beyonddimensions.Api.DataBase.Handler.IStackTypedHandler;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.IStackType;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.ItemStackType;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.StackCreater;
import com.wintercogs.beyonddimensions.Menu.Slot.AbstractStackTypedSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import static com.wintercogs.beyonddimensions.Unit.InventoryHelper.transferToPlayerInventory;

public abstract class BDDisorderedContainerMenu extends BDBaseMenu
{

    protected BDDisorderedContainerMenu(@Nullable MenuType<?> menuType, int containerId, Inventory playerInventory, @Nullable IStackTypedHandler storage)
    {
        super(menuType, containerId, playerInventory, storage);
    }

    @Override
    protected ItemStack quickMoveHandle(Player player, int slotIndex, IStackType clickStack, IStackTypedHandler storage)
    {
        ItemStack cacheStack;
        Slot slot = this.slots.get(slotIndex);
        if (slot != null && !clickStack.isEmpty())
        {
            // 物品从存储移动到背包
            if(slot instanceof AbstractStackTypedSlot)
            {
                if(clickStack instanceof ItemStackType clickedItem)
                {
                    cacheStack = clickedItem.copyStack();
                    int moveCount = checkCanMoveStackCount(cacheStack, inventoryStartIndex, inventoryEndIndex, true);
                    moveCount = Math.min(moveCount,cacheStack.getCount()); // 首先
                    int nowCount = 0;
                    // 数据验证，防止网络包伪造
                    IStackType typedStack = storage.getStackByStack(StackCreater.Create(ItemStackType.ID,cacheStack.copy(),cacheStack.getCount()));
                    ItemStack nowStack;
                    if(typedStack != null)
                    {
                        nowStack = (ItemStack) typedStack.getStack();
                    }
                    else
                    {
                        return ItemStack.EMPTY;
                    }
                    if(nowStack != null)
                    {
                        nowCount = nowStack.getCount();
                    }
                    moveCount = Math.min(moveCount,nowCount);
                    if(moveCount>=0)
                    {
                        cacheStack.setCount(moveCount);
                        if (!this.moveItemStackTo(cacheStack, inventoryStartIndex, inventoryEndIndex, true)) {
                            return ItemStack.EMPTY;
                        }
                        storage.extract(StackCreater.Create(ItemStackType.ID, clickStack.copyStackWithCount(moveCount),moveCount) ,false);
                    }
                }
                else
                {
                    cacheStack = ItemStack.EMPTY;
                }
            }
            else // 物品由背包移动到存储
            {
                // 快速合成
                if(slot instanceof ResultSlot resultSlot)
                {
                    cacheStack = slot.getItem().copy();
                    for(int i = 0;  !slot.getItem().isEmpty() && i< slot.getItem().getMaxStackSize()/slot.getItem().getCount(); i++)
                    {
                        ItemStack craftStack = slot.getItem().copy();
                        if(!ItemStack.isSameItemSameComponents(cacheStack, craftStack))
                            break;

                        // 如果背包放不下再存入存储系统
                        ItemStack remaining = transferToPlayerInventory(player, craftStack);

                        // 处理剩余物品
                        if (!remaining.isEmpty()) {
                            remaining = (ItemStack) storage.insert(StackCreater.Create(ItemStackType.ID, remaining, remaining.getCount()), false).copyStack();
                        }
                        // 恢复cacheStack防止后面检测导致resultSlot被设为空
                        craftStack = slot.getItem().copy();

                        if(remaining.isEmpty())
                        {
                            resultSlot.onTake(player, craftStack);
                        }

                    }

                }
                else
                {
                    // 从槽位获取物品实体 防止网络包伪造
                    cacheStack = slot.getItem().copy();
                    int remaining = (int)storage.insert(StackCreater.Create(ItemStackType.ID, cacheStack.copy(),cacheStack.getCount()),false).getStackAmount();
                    slot.tryRemove(cacheStack.getCount() - remaining,Integer.MAX_VALUE,player);
                }

            }
            slot.setChanged();
        }
        return ItemStack.EMPTY;
    }
}
