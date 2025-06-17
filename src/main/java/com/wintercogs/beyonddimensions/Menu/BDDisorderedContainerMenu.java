package com.wintercogs.beyonddimensions.Menu;

import com.wintercogs.beyonddimensions.Api.DataBase.Handler.IStackTypedHandler;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.FluidStackType;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.IStackType;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.ItemStackType;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.StackCreater;
import com.wintercogs.beyonddimensions.Api.DataBase.StackHandlerWrapper.FluidHandlerWrapper;
import com.wintercogs.beyonddimensions.Api.DataBase.StackHandlerWrapper.IStackHandlerWrapper;
import com.wintercogs.beyonddimensions.Api.Registry.CapabilityHelper;
import com.wintercogs.beyonddimensions.Api.Registry.StackHandlerWrapperHelper;
import com.wintercogs.beyonddimensions.Menu.Slot.StoredStackSlot;
import com.wintercogs.beyonddimensions.Unit.BDMath;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

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
                // 快速合成
                if(slot instanceof ResultSlot resultSlot)
                {
                    cacheStack = slot.getItem().copy();
                    for(int i = 0; !slot.getItem().isEmpty() && i< slot.getItem().getMaxStackSize()/slot.getItem().getCount(); i++)
                    {
                        ItemStack craftStack = slot.getItem().copy();
                        if(!ItemStack.isSameItemSameTags(cacheStack, craftStack))
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
                    cacheStack = slot.getItem().copy();
                    int remaining = (int)storage.insert(StackCreater.Create(ItemStackType.ID, cacheStack.copy(),cacheStack.getCount()),false).getStackAmount();
                    slot.tryRemove(cacheStack.getCount() - remaining,Integer.MAX_VALUE,player);
                }
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


                AtomicBoolean handled = new AtomicBoolean(false);
                // 堆叠数量为1 右键点击 尝试取出内容物并插入
                if(carriedItem.getCount()==1 && button== GLFW.GLFW_MOUSE_BUTTON_RIGHT)
                {
                    if(carriedItem.getItem() instanceof BucketItem bucketItem)
                    {
                        Object handler = carriedItem.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).orElse(null);
                        if(handler != null)
                        {
                            FluidHandlerWrapper stackHandlerWrapper = new FluidHandlerWrapper(handler);

                            if(stackHandlerWrapper.getSlots()>0)
                            {
                                FluidStackType stack = new FluidStackType(stackHandlerWrapper.getStackInSlot(0));
                                if(stack != null && !stack.isEmpty())
                                {
                                    int changedCount = BDMath.clampLongToInt(Math.min(stack.getStackAmount(),stack.getVanillaMaxStackSize()));
                                    // 进行模拟，桶必须完全清空才被允许操作
                                    int remaining = (int)storage.insert(stack.copyWithCount(changedCount),true).getStackAmount();
                                    if(remaining<=0)
                                    {
                                        // 执行实际逻辑
                                        storage.insert(stack.copyWithCount(changedCount),false).getStackAmount();
                                        setCarried(new ItemStack(Items.BUCKET));
                                        handled.set(true);
                                    }
                                }
                            }
                        }
                    }
                    else
                    {
                        CapabilityHelper.ItemCapabilityMap.forEach((typeId, cap)->{
                            Object handler = carriedItem.getCapability(cap).orElse(null);
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
                                            int changedCount = BDMath.clampLongToInt(Math.min(stack.getStackAmount(),stack.getVanillaMaxStackSize()));
                                            int remaining = (int)storage.insert(stack.copyWithCount(changedCount),false).getStackAmount();
                                            int actualInsert = changedCount - remaining;

                                            if(actualInsert>0)
                                            {
                                                long actualExtracts = stackHandlerWrapper.extract(index,actualInsert,false);
                                                if(actualExtracts< actualInsert)
                                                {
                                                    // 对此进行一个回调
                                                    storage.extract(stack.copyWithCount(actualInsert-actualExtracts),false);
                                                }
                                                setCarried(carriedItem.copy()); // 重设持有物以应用修改后的handler
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
                    int changedCount = button == GLFW.GLFW_MOUSE_BUTTON_LEFT ? carriedItem.getCount() : 1;
                    int actualInsert = (int) (changedCount - storage.insert(StackCreater.Create(ItemStackType.ID, carriedItem.copyWithCount(changedCount), changedCount), false).getStackAmount());
                    int newCount = carriedItem.getCount() - actualInsert;
                    if (newCount <= 0)
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
        }
        else if (slot.mayPickup(player))
        {
            if (carriedItem.isEmpty())
            {
                if(clickStack instanceof ItemStackType clickItem)
                {
                    //槽位物品存在，携带物品为空，尝试取出槽位物品
                    // 确保一次取出最大不得超过原版数量
                    int woundChangeNum = BDMath.clampLongToInt(Math.min(clickItem.getStackAmount(), clickItem.getVanillaMaxStackSize()));
                    int actualChangeNum = button == GLFW.GLFW_MOUSE_BUTTON_LEFT ? woundChangeNum : (woundChangeNum + 1) / 2;
                    ItemStack takenItem = ((ItemStack) storage.extract(new ItemStackType(clickItem.copyStackWithCount(actualChangeNum)),false).getStack()).copy();
                    if(takenItem != null)
                    {
                        setCarried(takenItem);
                        storage.onChange();
                    }
                }
            }
            else if (slot.mayPlace(carriedItem))
            {
                // 如果使用一个有存储能力的单个物品，点击右键，
                // 则，尝试将目标抽入到自身。如果抽取失败
                // 则，尝试将自身内容物存入网络。
                // 最后，如果以上两个操作均未进行，则将物品本身存入

                AtomicBoolean handled = new AtomicBoolean(false);

                if(carriedItem.getCount() == 1 && button == GLFW.GLFW_MOUSE_BUTTON_RIGHT)
                {
                    // 对桶物品进行特殊处理
                    if(carriedItem.getItem() instanceof BucketItem bucket)
                    {
                        // 需要分开处理，分别处理
                        // 1.空桶接受
                        // 2.桶向原有区域继续投放
                        if(bucket == Items.BUCKET) // 空桶接受
                        {
                            if(clickStack instanceof FluidStackType fluidStackType)
                            {
                                Item filledBucket = fluidStackType.getStack().getFluid().getBucket();

                                if(filledBucket != null && filledBucket != Items.AIR
                                        && storage.getStackByStack(fluidStackType).getStackAmount()>=1000)
                                {
                                    // 执行操作
                                    storage.extract(fluidStackType.copyWithCount(1000),false);
                                    setCarried(new ItemStack(filledBucket));
                                    handled.set(true);
                                }
                            }
                        }
                        else // 继续投放 insert模拟会自动解决类型不匹配等问题
                        {
                            Object handler = carriedItem.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).orElse(null);
                            if(handler != null)
                            {
                                FluidHandlerWrapper stackHandlerWrapper = new FluidHandlerWrapper(handler);

                                if(stackHandlerWrapper.getSlots()>0)
                                {
                                    FluidStackType stack = new FluidStackType(stackHandlerWrapper.getStackInSlot(0));
                                    if(stack != null && !stack.isEmpty())
                                    {
                                        int changedCount = BDMath.clampLongToInt(Math.min(stack.getStackAmount(),stack.getVanillaMaxStackSize()));
                                        // 进行模拟，桶必须完全清空才被允许操作
                                        int remaining = (int)storage.insert(stack.copyWithCount(changedCount),true).getStackAmount();
                                        if(remaining<=0)
                                        {
                                            // 执行实际逻辑
                                            storage.insert(stack.copyWithCount(changedCount),false).getStackAmount();
                                            setCarried(new ItemStack(Items.BUCKET));
                                            handled.set(true);
                                        }
                                    }
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
                                Object handler = carriedItem.getCapability(cap).orElse(null);
                                if(handler != null)
                                {
                                    Function handlerGetter = StackHandlerWrapperHelper.stackWrappers.get(typeId);
                                    IStackHandlerWrapper stackHandlerWrapper = (IStackHandlerWrapper) handlerGetter.apply(handler);
                                    if(stackHandlerWrapper.getSlots()>0)
                                    {
                                        IStackType actualClickStack = storage.getStackByStack(clickStack);// 防止客户端假消息
                                        if(actualClickStack != null)
                                        {
                                            int changedCount = BDMath.clampLongToInt(Math.min(actualClickStack.getStackAmount(),actualClickStack.getVanillaMaxStackSize()));
                                            int remaining = (int)stackHandlerWrapper.insert(actualClickStack.copyStackWithCount(changedCount),false);
                                            int actualInsert = changedCount - remaining;
                                            if(actualInsert>0)
                                            {
                                                storage.extract(actualClickStack.copyWithCount(actualInsert),false);
                                                setCarried(carriedItem.copy()); // 重设持有物以应用修改后的handler
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
                                Object handler = carriedItem.getCapability(cap).orElse(null);
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
                                                int changedCount = BDMath.clampLongToInt(Math.min(stack.getStackAmount(),stack.getVanillaMaxStackSize()));
                                                int remaining = (int)storage.insert(stack.copyWithCount(changedCount),false).getStackAmount();
                                                int actualInsert = changedCount - remaining;

                                                if(actualInsert>0)
                                                {
                                                    long actualExtracts = stackHandlerWrapper.extract(index,actualInsert,false);
                                                    if(actualExtracts< actualInsert)
                                                    {
                                                        // 对此进行一个回调
                                                        storage.extract(stack.copyWithCount(actualInsert-actualExtracts),false);
                                                    }
                                                    setCarried(carriedItem.copy()); // 重设持有物以应用修改后的handler
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
