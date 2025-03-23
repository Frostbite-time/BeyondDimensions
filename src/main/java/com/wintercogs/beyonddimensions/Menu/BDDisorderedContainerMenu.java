package com.wintercogs.beyonddimensions.Menu;

import com.wintercogs.beyonddimensions.DataBase.Handler.IStackTypedHandler;
import com.wintercogs.beyonddimensions.DataBase.Stack.IStackType;
import com.wintercogs.beyonddimensions.DataBase.Stack.ItemStackType;
import com.wintercogs.beyonddimensions.DataBase.Stack.StackCreater;
import com.wintercogs.beyonddimensions.Menu.Slot.StoredStackSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.lwjgl.glfw.GLFW;

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
            if(slot instanceof StoredStackSlot)
            {
                if(clickStack instanceof ItemStackType clickedItem)
                {
                    cacheStack = clickedItem.copyStack();
                    int moveCount = checkCanMoveStackCount(cacheStack, inventoryStartIndex, inventoryEndIndex, true);
                    moveCount = Math.min(moveCount,cacheStack.getCount()); // 首先
                    int nowCount = 0;
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
                cacheStack = slot.getItem().copy();
                storage.insert(StackCreater.Create(ItemStackType.ID, cacheStack.copy(),cacheStack.getCount()),false);
                slot.tryRemove(cacheStack.getCount(),Integer.MAX_VALUE-1,player);
            }
            if (cacheStack.isEmpty()) {
                // 对于维度网络通过玩家设置一个EMPTY无影响
                // 对于背包槽位可以用于清空当前槽位物品
                // 对于双方，都可以设置脏数据请求保存
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    protected void clickHandle(int slotIndex, IStackType clickStack, int button, Player player, IStackTypedHandler storage)
    {
        ItemStack carriedItem = this.getCarried().copy();// getCarried方法获取直接引用，所以需要copy防止误操作
        StoredStackSlot slot = (StoredStackSlot) this.slots.get(slotIndex);// clickHandle仅用于处理点击维度槽位的逻辑，如果转换失败，则证明调用逻辑出错

        if (clickStack.isEmpty())
        {
            if (!carriedItem.isEmpty())
            {   //槽位物品为空，携带物品存在，将携带物品插入槽位
                int changedCount = button == GLFW.GLFW_MOUSE_BUTTON_LEFT ? carriedItem.getCount() : 1;
                storage.insert(StackCreater.Create(ItemStackType.ID, carriedItem.copyWithCount(changedCount),changedCount),false);
                int newCount = carriedItem.getCount() - changedCount;
                if(newCount <=0)
                {
                    setCarried(ItemStack.EMPTY);
                }
                else
                {
                    ItemStack newCarriedItem = carriedItem.copy();
                    newCarriedItem.setCount(newCount);
                    setCarried(newCarriedItem);
                }
            }
        }
        else if (slot.mayPickup(player))
        {
            if(clickStack instanceof ItemStackType clickItem)
            {
                if (carriedItem.isEmpty())
                {   //槽位物品存在，携带物品为空，尝试取出槽位物品

                    // 确保一次取出最大不得超过原版数量
                    int woundChangeNum = (int) Math.min(clickItem.getStackAmount(), clickItem.getVanillaMaxStackSize());
                    int actualChangeNum = button == GLFW.GLFW_MOUSE_BUTTON_LEFT ? woundChangeNum : (woundChangeNum + 1) / 2;
                    ItemStack takenItem = ((ItemStack) storage.extract(new ItemStackType(clickItem.copyStackWithCount(actualChangeNum)),false).getStack()).copy();
                    if(takenItem != null)
                    {
                        setCarried(takenItem);
                        storage.onChange();
                    }
                }
                else if (slot.mayPlace(carriedItem))
                {   //槽位物品存在，携带物品存在，物品可以放置，尝试将物品放入
                    int changedCount = button == GLFW.GLFW_MOUSE_BUTTON_LEFT ? carriedItem.getCount() : 1;
                    storage.insert(StackCreater.Create(ItemStackType.ID,carriedItem.copyWithCount(changedCount),changedCount),false);
                    int newCount = carriedItem.getCount() - changedCount;
                    if(newCount <=0)
                    {
                        setCarried(ItemStack.EMPTY);
                    }
                    else
                    {
                        ItemStack newCarriedItem = carriedItem.copy();
                        newCarriedItem.setCount(newCount);
                        setCarried(newCarriedItem);
                    }
                }
                else if (clickStack.isSameTypeSameComponents(new ItemStackType(carriedItem.copy())))
                {   // 槽位物品存在，携带物品存在，物品不可放置，为完全相同的物品
                    // 此情况在点击维度存储槽时永远不可能发生，如果发生，无需处理
                    // 原版逻辑为取出物品到最大上限
                    // 保留此情况以便后续使用
                }
            }
        }
    }
}
