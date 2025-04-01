package com.wintercogs.beyonddimensions.Gui;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.DataBase.Handler.IStackTypedHandler;
import com.wintercogs.beyonddimensions.DataBase.Stack.ChemicalStackType;
import com.wintercogs.beyonddimensions.DataBase.Stack.IStackType;
import com.wintercogs.beyonddimensions.DataBase.Stack.ItemStackType;
import com.wintercogs.beyonddimensions.DataBase.Stack.StackCreater;
import com.wintercogs.beyonddimensions.DataBase.StackHandlerWrapper.IStackHandlerWrapper;
import com.wintercogs.beyonddimensions.Unit.CapabilityHelper;
import com.wintercogs.beyonddimensions.Unit.StackHandlerWrapperHelper;
import mekanism.api.gas.GasStack;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public abstract class BDOrderedContainerGUI extends BDBaseGUI
{
    // 处理存储到背包的快速移动
    protected void quickMoveHandle(int slotIndex,IStackType clickStack, int button, boolean isFakeSlot, EntityPlayer player, IStackTypedHandler storage)
    {
        // 目前仅从存储到背包
        if(!clickStack.isEmpty())
        {
            if(clickStack instanceof ItemStackType)
            {
                // 验证存储是否拥有对应物品
                ItemStackType clickedItem = (ItemStackType) storage.getStackBySlot(slotIndex);
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
                        storage.extract(slotIndex,needToRemove,false);
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
                int remaining = (int)storage.insert(slotIndex,StackCreater.Create(ItemStackType.ID, carriedItem.copy(),changedCount),false).getStackAmount();
                int needRemove = changedCount - remaining;
                int newCount = carriedItem.getCount() - needRemove;
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
                    ItemStack takenItem = ((ItemStack) storage.extract(slotIndex,actualChangeNum,false).getStack()).copy();
                    if(takenItem != null)
                    {
                        guiSyncManager.setCursorItem(takenItem);
                        storage.onChange();
                    }
                }
                else if (true)
                {   //槽位物品存在，携带物品存在，物品可以放置，尝试将物品放入
                    int changedCount = button == 0 ? carriedItem.getCount() : 1;
                    int remaining = (int)storage.insert(slotIndex,StackCreater.Create(ItemStackType.ID,carriedItem,changedCount),false).getStackAmount();
                    int needRemove = changedCount - remaining;
                    int newCount = carriedItem.getCount() - needRemove;
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
        if(storage== null)
            return;

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
                    IStackType remainStack = storage.insert(moveIn,false);
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

                if(button==0)
                {
                    ItemStack copy = carriedItem.copy();
                    copy.setCount(1);
                    storage.insert(slotIndex,new ItemStackType(copy),false);
                }
                else if(button==1)
                {
                    // 获取能力
                    CapabilityHelper.ItemCapabilityMap.forEach((typeId, cap) -> {
                        Object handler = carriedItem.getCapability(cap, EnumFacing.DOWN);
                        if(handler != null)
                        {
                            Function handlerGetter = StackHandlerWrapperHelper.stackWrappers.get(typeId);
                            IStackHandlerWrapper stackHandlerWrapper = (IStackHandlerWrapper)handlerGetter.apply(handler);

                            if(stackHandlerWrapper.getSlots()>0)
                            {
                                IStackType stack = StackCreater.Create(typeId,stackHandlerWrapper.getStackInSlot(0),1);
                                if(stack !=null&& !stack.isEmpty())
                                {
                                    storage.insert(slotIndex,stack,false);
                                }
                            }
                        }
                    });

                    // 我不知道为什么Mek在1.12.2时不为他们的物品注册能力系统
                    if(BeyondDimensions.MekLoaded)
                    {
                        if(carriedItem.getItem() instanceof mekanism.common.item.ItemBlockGasTank tank)
                        {
                            NBTTagCompound gasTag = carriedItem.getTagCompound().getCompoundTag("mekData").getCompoundTag("stored");
                            mekanism.api.gas.GasStack gasStack = mekanism.api.gas.GasStack.readFromNBT(gasTag);
                            if(gasStack!=null)
                            {
                                ChemicalStackType stackTyped = new ChemicalStackType(gasStack);
                                if(!stackTyped.isEmpty())
                                {
                                    stackTyped.setStackAmount(1);
                                    storage.insert(slotIndex,stackTyped,false);
                                }
                            }
                        }
                    }
                }

            }
        }
        else
        {
            if (carriedItem.isEmpty())
            {
                //槽位物品存在，携带物品为空，尝试清空标记
                storage.extract(slotIndex,clickStack.getStackAmount(),false);
            }
            else if (true)
            {   //槽位物品存在，携带物品存在，物品可以放置，取消标记

                storage.extract(slotIndex,clickStack.getStackAmount(),false);

            }
            else if (clickStack.isSameTypeSameComponents(new ItemStackType(carriedItem.copy())))
            {   // 槽位物品存在，携带物品存在，物品不可放置，为完全相同的物品

            }

        }
    }

    /**
     * 根据当前的搜索状态、按钮状态对存储进行排序
     * @return 完成排序的索引列表
     */
    // 重写版本不筛选物品，用于只能滚动而不能搜索和筛选的容器
    @Override
    public List<Integer> buildStorageWithCurrentState(ArrayList<IStackType> storage) {
        ArrayList<Integer> cacheIndex = new ArrayList<>();
        for (int i = 0; i < storage.size(); i++) {
            cacheIndex.add(i);
        }

        return cacheIndex;
    }

    /**
     * 客户端专用函数，服务端请勿调用<br>
     * 使用当前客户端的真存储来更新视觉存储，然后重构索引以刷新显示
     * 比起buildIndexList开销较大，仅确定真存储有变化时才调用
     */
    // 重写版本将插入变成按索引指定
    @Override
    public void updateViewerStorage()
    {
        for (IStackType stack : this.viewerStackTypedHandler.getStorage())
        {
            stack.setStackAmount(-1);
        }

        int index = 0;
        for (IStackType stack : this.stackTypedHandler.getStorage())
        {
            this.viewerStackTypedHandler.insert(index,stack.copy(), false);
            index++;
        }
        buildIndexList(new ArrayList<>(viewerStackTypedHandler.getStorage()));
    }

}
