package com.wintercogs.beyonddimensions.Gui;

import com.cleanroommc.modularui.factory.GuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.value.sync.GuiSyncManager;
import com.wintercogs.beyonddimensions.DataBase.Handler.IStackTypedHandler;
import com.wintercogs.beyonddimensions.DataBase.Stack.IStackType;
import com.wintercogs.beyonddimensions.DataBase.Stack.ItemStackType;
import com.wintercogs.beyonddimensions.DataBase.Stack.StackCreater;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

public abstract class BDDisorderedContainerGUI extends BDBaseGUI
{

    @Override
    public ModularPanel buildUI(GuiData guiData, GuiSyncManager guiSyncManager)
    {
        return super.buildUI(guiData, guiSyncManager);
    }

    // 处理存储到背包的快速移动
    protected void quickMoveHandle(int slotIndex,IStackType clickStack, int button, boolean isFakeSlot, EntityPlayer player, IStackTypedHandler storage)
    {
        // 目前仅从存储到背包
        if(!clickStack.isEmpty())
        {
            if(clickStack instanceof ItemStackType)
            {
                // 验证存储是否拥有对应物品
                ItemStackType clickedItem = (ItemStackType) storage.getStackByStack(clickStack);
                if(clickedItem != null&&!clickedItem.isEmpty())
                {
                    // 首先获取原版最大数值和存储量的最小值
                    long maxMoveCount = Math.min(clickedItem.getStackAmount(),clickedItem.getVanillaMaxStackSize());
                    if(button==1) //如果鼠标是右键 最大传输数量再减半
                        maxMoveCount = maxMoveCount/2;
                    ItemStack moveIn = clickedItem.copyStackWithCount(maxMoveCount);
                    player.inventory.addItemStackToInventory(moveIn);
                    int remaining = moveIn.getCount(); //addItemStackToInventory会修改原物品堆的数量
                    int needToRemove = (int) (maxMoveCount - remaining);
                    if(needToRemove > 0)
                        storage.extract(clickedItem.copyWithCount(needToRemove),false);
                }

            }
        }
    }


    // 用于处理鼠标事件的函数
    protected void clickHandle(int slotIndex,IStackType clickStack, int button, boolean isFakeSlot, EntityPlayer player, IStackTypedHandler storage)
    {
        // 获取光标物品
        ItemStack carriedItem = guiSyncManager.getCursorItem();

        if (clickStack.isEmpty())
        {
            if (!carriedItem.isEmpty())
            {   //槽位物品为空，携带物品存在，将携带物品插入槽位
                int changedCount = button == 0 ? carriedItem.getCount() : 1;
                storage.insert(StackCreater.Create(ItemStackType.ID, carriedItem.copy(),changedCount),false);
                int newCount = carriedItem.getCount() - changedCount;
                if(newCount <=0)
                {
                    guiSyncManager.setCursorItem(ItemStack.EMPTY);
                }
                else
                {
                    ItemStack newCarriedItem = carriedItem.copy();
                    newCarriedItem.setCount(newCount);
                    guiSyncManager.setCursorItem(newCarriedItem);
                }
            }
        }
        else
        {
            if(clickStack instanceof ItemStackType clickItem)
            {
                if (carriedItem.isEmpty())
                {   //槽位物品存在，携带物品为空，尝试取出槽位物品

                    // 确保一次取出最大不得超过原版数量
                    int woundChangeNum = (int) Math.min(clickItem.getStackAmount(), clickItem.getVanillaMaxStackSize());
                    int actualChangeNum = button == 0 ? woundChangeNum : (woundChangeNum + 1) / 2;
                    ItemStack takenItem = ((ItemStack) storage.extract(new ItemStackType(clickItem.copyStackWithCount(actualChangeNum)),false).getStack()).copy();
                    if(takenItem != null)
                    {
                        guiSyncManager.setCursorItem(takenItem);
                        storage.onChange();
                    }
                }
                else if (true)
                {   //槽位物品存在，携带物品存在，物品可以放置，尝试将物品放入
                    int changedCount = button == 0 ? carriedItem.getCount() : 1;
                    storage.insert(StackCreater.Create(ItemStackType.ID,carriedItem,changedCount),false);
                    int newCount = carriedItem.getCount() - changedCount;
                    if(newCount <=0)
                    {
                        guiSyncManager.setCursorItem(ItemStack.EMPTY);
                    }
                    else
                    {
                        ItemStack newCarriedItem = carriedItem.copy();
                        newCarriedItem.setCount(newCount);
                        guiSyncManager.setCursorItem(newCarriedItem);
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


    // 处理背包到存储的快速移动
    protected void quickMoveHandleInventory(int slotIndex,IStackType clickStack, int button, boolean isFakeSlot, EntityPlayer player, IStackTypedHandler storage)
    {
        // 目前仅从存储到背包
        if(!clickStack.isEmpty())
        {
            if(clickStack instanceof ItemStackType)
            {
                // 根据库存中索引获取对应物品
                ItemStackType clickedItem = new ItemStackType(this.guiSyncManager.getPlayerInventory().getStackInSlot(slotIndex));
                if(clickedItem != null&&!clickedItem.isEmpty())
                {
                    // 首先获取原版最大数值和存储量的最小值
                    long maxMoveCount = Math.min(clickedItem.getStackAmount(),clickedItem.getVanillaMaxStackSize());
                    if(button==1) //如果鼠标是右键 最大传输数量再减半
                        maxMoveCount = maxMoveCount/2;
                    IStackType moveIn = clickedItem.copyWithCount(maxMoveCount);
                    IStackType remainStack = stackTypedHandler.insert(moveIn,false);
                    int needToRemove = (int) (maxMoveCount - remainStack.getStackAmount());
                    if(needToRemove > 0)
                        guiSyncManager.getPlayerInventory().extractItem(slotIndex, needToRemove, false);

                }

            }
        }
    }

    @Override
    protected void FakeClickHandle(int slotIndex, IStackType clickStack, int button, boolean isFakeSlot, EntityPlayer player, IStackTypedHandler storage)
    {
        // 获取光标物品
        ItemStack carriedItem = guiSyncManager.getCursorItem();

        if (clickStack.isEmpty())
        {
            if (!carriedItem.isEmpty())
            {   //槽位物品为空，携带物品存在，将携带物品插入标记
                ItemStack copy = carriedItem.copy();
                copy.setCount(1);
                storage.insert(new ItemStackType(copy),false);
            }
        }
        else
        {
            if (carriedItem.isEmpty())
            {
                //槽位物品存在，携带物品为空，尝试清空标记
                storage.extract(clickStack.copy(),false);
            }
            else if (true)
            {   //槽位物品存在，携带物品存在，物品可以放置，尝试将物品放入

            }
            else if (clickStack.isSameTypeSameComponents(new ItemStackType(carriedItem.copy())))
            {   // 槽位物品存在，携带物品存在，物品不可放置，为完全相同的物品

            }

        }
    }
}
