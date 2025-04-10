package com.wintercogs.beyonddimensions.Gui;

import com.cleanroommc.modularui.factory.GuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.value.sync.GuiSyncManager;
import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.DataBase.Handler.IStackTypedHandler;
import com.wintercogs.beyonddimensions.DataBase.Stack.*;
import com.wintercogs.beyonddimensions.DataBase.StackHandlerWrapper.FluidHandlerWrapper;
import com.wintercogs.beyonddimensions.DataBase.StackHandlerWrapper.IStackHandlerWrapper;
import com.wintercogs.beyonddimensions.Unit.CapabilityHelper;
import com.wintercogs.beyonddimensions.Unit.StackHandlerWrapperHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemBucket;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

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


                AtomicBoolean handled = new AtomicBoolean(false);
                // 堆叠数量为1 右键点击 尝试取出内容物并插入
                if(carriedItem.getCount()==1 && button== 1)
                {
                    if(carriedItem.getItem() instanceof ItemBucket bucketItem)
                    {
                        Object handler = carriedItem.getCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY,EnumFacing.DOWN);
                        if(handler != null)
                        {
                            FluidHandlerWrapper stackHandlerWrapper = new FluidHandlerWrapper(handler);

                            if(stackHandlerWrapper.getSlots()>0)
                            {
                                FluidStackType stack = new FluidStackType(stackHandlerWrapper.getStackInSlot(0));
                                if(stack != null && !stack.isEmpty())
                                {
                                    int changedCount = (int) Math.min(stack.getStackAmount(),stack.getVanillaMaxStackSize());
                                    // 进行模拟，桶必须完全清空才被允许操作
                                    int remaining = (int)storage.insert(stack.copyWithCount(changedCount),true).getStackAmount();
                                    if(remaining<=0)
                                    {
                                        // 执行实际逻辑
                                        storage.insert(stack.copyWithCount(changedCount),false).getStackAmount();
                                        guiSyncManager.setCursorItem(new ItemStack(Items.BUCKET));
                                        handled.set(true);
                                    }
                                }
                            }
                        }
                    }
                    else if(BeyondDimensions.MekLoaded && carriedItem.getItem() instanceof mekanism.common.item.ItemBlockGasTank tank)
                    {
                        NBTTagCompound gasTag = carriedItem.getTagCompound().getCompoundTag("mekData").getCompoundTag("stored");
                        mekanism.api.gas.GasStack gasStack = mekanism.api.gas.GasStack.readFromNBT(gasTag);
                        if(gasStack!=null)
                        {
                            ChemicalStackType stackTyped = new ChemicalStackType(gasStack);
                            if(!stackTyped.isEmpty())
                            {
                                int changedCount = (int) Math.min(stackTyped.getStackAmount(),stackTyped.getVanillaMaxStackSize());
                                int remaining = (int)storage.insert(stackTyped.copyWithCount(changedCount),false).getStackAmount();
                                int actualInsert = changedCount - remaining;
                                if(actualInsert>0)
                                {
                                    stackTyped.shrink(actualInsert);
                                    ItemStack newCarried = carriedItem.copy();
                                    tank.setGas(newCarried, stackTyped.getStack());
                                    guiSyncManager.setCursorItem(newCarried);
                                    handled.set(true);
                                }
                            }
                        }
                    }
                    else
                    {
                        CapabilityHelper.ItemCapabilityMap.forEach((typeId, cap)->{
                            Object handler = carriedItem.getCapability(cap,EnumFacing.DOWN);
                            if(handler != null)
                            {
                                Function handlerGetter = StackHandlerWrapperHelper.stackWrappers.get(typeId);
                                IStackHandlerWrapper stackHandlerWrapper = (IStackHandlerWrapper) handlerGetter.apply(handler);

                                if(stackHandlerWrapper.getSlots()>0)
                                {
                                    // 一次操作只操作其第一个有效槽位，然后break
                                    for(int index=0;index<stackHandlerWrapper.getSlots();index++)
                                    {
                                        IStackType stack = StackCreater.Create(typeId,stackHandlerWrapper.getStackInSlot(index));
                                        if(stack !=null&& !stack.isEmpty())
                                        {
                                            int changedCount = (int) Math.min(stack.getStackAmount(),stack.getVanillaMaxStackSize());
                                            int remaining = (int)storage.insert(stack.copyWithCount(changedCount),false).getStackAmount();
                                            int actualInsert = changedCount - remaining;

                                            if(actualInsert>0)
                                            {
                                                long actualExtracts = stackHandlerWrapper.extract(EnumFacing.DOWN,index,actualInsert,false);
                                                if(actualExtracts<actualInsert)
                                                {
                                                    // 如果实际消耗量与插入存储的量不符合，进行一次回调
                                                    storage.extract(stack.copyWithCount(actualInsert - actualExtracts),false);
                                                }
                                                guiSyncManager.setCursorItem(carriedItem.copy()); // 重设持有物以应用修改后的handler
                                                handled.set(true);
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                        });
                    }
                }

                if(!handled.get())
                {
                    int changedCount = button == 0 ? carriedItem.getCount() : 1;
                    ItemStack copy = carriedItem.copy();
                    copy.setCount(changedCount);
                    storage.insert(StackCreater.Create(ItemStackType.ID, copy, changedCount), false);
                    int newCount = carriedItem.getCount() - changedCount;
                    if (newCount <= 0)
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
        }
        else
        {

            if (carriedItem.isEmpty())
            {
                if(clickStack instanceof ItemStackType clickItem)
                {
                    //槽位物品存在，携带物品为空，尝试取出槽位物品
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
            }
            else if (true)
            {
                // 如果使用一个有存储能力的单个物品，点击右键，
                // 则，尝试将目标抽入到自身。如果抽取失败
                // 则，尝试将自身内容物存入网络。
                // 最后，如果以上两个操作均未进行，则将物品本身存入

                AtomicBoolean handled = new AtomicBoolean(false);

                if(carriedItem.getCount() == 1 && button == 1)
                {
                    // 对桶物品进行特殊处理
                    if(carriedItem.getItem() instanceof ItemBucket bucket)
                    {
                        // 需要分开处理，分别处理
                        // 1.空桶接受
                        // 2.桶向原有区域继续投放
                        if(bucket == Items.BUCKET) // 空桶接受
                        {
                            if(clickStack instanceof FluidStackType fluidStackType)
                            {
                                ItemStack filledBucket = FluidUtil.getFilledBucket(fluidStackType.getStack());

                                if(filledBucket != null && filledBucket != ItemStack.EMPTY
                                        && storage.getStackByStack(fluidStackType).getStackAmount()>=1000)
                                {
                                    // 执行操作
                                    storage.extract(fluidStackType.copyWithCount(1000),false);
                                    guiSyncManager.setCursorItem(filledBucket.copy());
                                    handled.set(true);
                                }
                            }
                        }
                        else // 继续投放 insert模拟会自动解决类型不匹配等问题
                        {
                            Object handler = carriedItem.getCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY,EnumFacing.DOWN);
                            if(handler != null)
                            {
                                FluidHandlerWrapper stackHandlerWrapper = new FluidHandlerWrapper(handler);

                                if(stackHandlerWrapper.getSlots()>0)
                                {
                                    FluidStackType stack = new FluidStackType(stackHandlerWrapper.getStackInSlot(0));
                                    if(stack != null && !stack.isEmpty())
                                    {
                                        int changedCount = (int) Math.min(stack.getStackAmount(),stack.getVanillaMaxStackSize());
                                        // 进行模拟，桶必须完全清空才被允许操作
                                        int remaining = (int)storage.insert(stack.copyWithCount(changedCount),true).getStackAmount();
                                        if(remaining<=0)
                                        {
                                            // 执行实际逻辑
                                            storage.insert(stack.copyWithCount(changedCount),false).getStackAmount();
                                            guiSyncManager.setCursorItem(new ItemStack(Items.BUCKET));
                                            handled.set(true);
                                        }
                                    }
                                }
                            }
                        }
                    }
                    else if(BeyondDimensions.MekLoaded && carriedItem.getItem() instanceof mekanism.common.item.ItemBlockGasTank tank)
                    {
                        // 对化学品储罐的特殊处理

                        NBTTagCompound gasTag = carriedItem.getTagCompound().getCompoundTag("mekData").getCompoundTag("stored");
                        mekanism.api.gas.GasStack gasStack = mekanism.api.gas.GasStack.readFromNBT(gasTag);
                        // 抽入
                        if(gasStack == null || (clickStack instanceof ChemicalStackType clickChemical && gasStack.isGasEqual(clickChemical.getStack())))
                        {
                            ChemicalStackType chemicalStackType = (ChemicalStackType)storage.getStackByStack(clickStack); // 获取真实存储
                            if(chemicalStackType != null && !chemicalStackType.isEmpty())
                            {
                                int changedCount = (int) Math.min(chemicalStackType.getStackAmount(),chemicalStackType.getVanillaMaxStackSize());
                                int sourceAmount = 0;
                                if(gasStack != null)
                                    sourceAmount = gasStack.amount;
                                changedCount = Math.min(changedCount, tank.getMaxGas(carriedItem) - sourceAmount);
                                // 此时changedCount已经变为最大可插入量，并能作为实际插入量使用
                                ItemStack newCarried = carriedItem.copy();
                                int newAmount = sourceAmount + changedCount;
                                tank.setGas(newCarried, chemicalStackType.copyStackWithCount(newAmount));
                                if(changedCount>0)
                                {
                                    storage.extract(chemicalStackType.copyWithCount(changedCount),false);
                                    guiSyncManager.setCursorItem(newCarried);
                                    handled.set(true);
                                }
                            }
                        }
                        // 存入
                        else if(gasStack != null && !handled.get())
                        {
                            ChemicalStackType stackTyped = new ChemicalStackType(gasStack);
                            if(!stackTyped.isEmpty())
                            {
                                int changedCount = (int) Math.min(stackTyped.getStackAmount(),stackTyped.getVanillaMaxStackSize());
                                int remaining = (int)storage.insert(stackTyped.copyWithCount(changedCount),false).getStackAmount();
                                int actualInsert = changedCount - remaining;
                                if(actualInsert>0)
                                {
                                    stackTyped.shrink(actualInsert);
                                    ItemStack newCarried = carriedItem.copy();
                                    tank.setGas(newCarried, stackTyped.getStack());
                                    guiSyncManager.setCursorItem(newCarried);
                                    handled.set(true);
                                }
                            }
                        }

                    }
                    else
                    {
                        // 抽入
                        CapabilityHelper.ItemCapabilityMap.forEach((typeId,cap) -> {
                            // 先查看被点击物品的种类和对应能力种类
                            if(clickStack.getTypeId().equals(typeId))
                            {
                                // 尝试获取对应能力
                                Object handler = carriedItem.getCapability(cap,EnumFacing.DOWN);
                                if(handler != null)
                                {
                                    Function handlerGetter = StackHandlerWrapperHelper.stackWrappers.get(typeId);
                                    IStackHandlerWrapper stackHandlerWrapper = (IStackHandlerWrapper) handlerGetter.apply(handler);
                                    if(stackHandlerWrapper.getSlots()>0)
                                    {
                                        IStackType actualClickStack = storage.getStackByStack(clickStack);// 防止客户端假消息
                                        if(actualClickStack != null)
                                        {
                                            int changedCount = (int) Math.min(actualClickStack.getStackAmount(),actualClickStack.getVanillaMaxStackSize());
                                            int remaining = (int)stackHandlerWrapper.insert(EnumFacing.DOWN,actualClickStack.copyStackWithCount(changedCount),false);
                                            int actualInsert = changedCount - remaining;
                                            if(actualInsert>0)
                                            {
                                                storage.extract(actualClickStack.copyWithCount(actualInsert),false);
                                                guiSyncManager.setCursorItem(carriedItem.copy()); // 重设持有物以应用修改后的handler
                                                handled.set(true);
                                            }
                                        }

                                    }
                                }
                            }
                        });

                        //存入
                        if(!handled.get())
                        {
                            CapabilityHelper.ItemCapabilityMap.forEach((typeId,cap) -> {
                                Object handler = carriedItem.getCapability(cap,EnumFacing.DOWN);
                                if(handler != null)
                                {
                                    Function handlerGetter = StackHandlerWrapperHelper.stackWrappers.get(typeId);
                                    IStackHandlerWrapper stackHandlerWrapper = (IStackHandlerWrapper) handlerGetter.apply(handler);

                                    if(stackHandlerWrapper.getSlots()>0)
                                    {
                                        // 一次操作只操作其第一个有效槽位，然后break
                                        for(int index=0;index<stackHandlerWrapper.getSlots();index++)
                                        {
                                            IStackType stack = StackCreater.Create(typeId,stackHandlerWrapper.getStackInSlot(index));
                                            if(stack !=null&& !stack.isEmpty())
                                            {
                                                int changedCount = (int) Math.min(stack.getStackAmount(),stack.getVanillaMaxStackSize());
                                                int remaining = (int)storage.insert(stack.copyWithCount(changedCount),false).getStackAmount();
                                                int actualInsert = changedCount - remaining;

                                                if(actualInsert>0)
                                                {
                                                    long actualExtracts = stackHandlerWrapper.extract(EnumFacing.DOWN,index,actualInsert,false);
                                                    if(actualExtracts<actualInsert)
                                                    {
                                                        // 如果实际消耗量与插入存储的量不符合，进行一次回调
                                                        storage.extract(stack.copyWithCount(actualInsert - actualExtracts),false);
                                                    }
                                                    guiSyncManager.setCursorItem(carriedItem.copy()); // 重设持有物以应用修改后的handler
                                                    handled.set(true);
                                                    break;
                                                }

                                            }
                                        }
                                    }
                                }
                            });
                        }
                    }
                }

                if(!handled.get())
                {
                    //槽位物品存在，携带物品存在，物品可以放置，尝试将物品放入
                    int changedCount = button == 0 ? carriedItem.getCount() : 1;
                    ItemStack copy = carriedItem.copy();
                    copy.setCount(changedCount);
                    storage.insert(StackCreater.Create(ItemStackType.ID,copy,changedCount),false);
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
            else if (clickStack.isSameTypeSameComponents(new ItemStackType(carriedItem.copy())))
            {   // 槽位物品存在，携带物品存在，物品不可放置，为完全相同的物品
                // 此情况在点击维度存储槽时永远不可能发生，如果发生，无需处理
                // 原版逻辑为取出物品到最大上限
                // 保留此情况以便后续使用
            }
        }

    }


    // 处理背包到存储的快速移动
    protected void quickMoveHandleInventory(int slotIndex,IStackType clickStack, int button, boolean isFakeSlot, EntityPlayer player, IStackTypedHandler storage)
    {
        if(storage== null)
            return;

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

        // 以下代码正确性暂时难以证明
        if (clickStack.isEmpty())
        {
            if(button==0)
            {
                ItemStack copy = carriedItem.copy();
                copy.setCount(1);
                storage.insert(new ItemStackType(copy),false);
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
                                storage.insert(stack,false);
                            }
                        }
                    }
                });

                // 我不知道为什么Mek在1.12.2时不为他们的气体储罐注册物品能力？毕竟这是唯一能存储气体的容器
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
                                storage.insert(stackTyped,false);
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
                storage.extract(clickStack.copy(),false);
            }
            else if (true)
            {   //槽位物品存在，携带物品存在，物品可以放置，尝试将物品放入
                storage.extract(clickStack.copy(),false);
            }
            else if (clickStack.isSameTypeSameComponents(new ItemStackType(carriedItem.copy())))
            {   // 槽位物品存在，携带物品存在，物品不可放置，为完全相同的物品

            }

        }
    }
}
