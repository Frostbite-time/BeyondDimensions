package com.wintercogs.beyonddimensions.Menu.Slot;

import com.wintercogs.beyonddimensions.Api.DataBase.Handler.IStackTypedHandler;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.FluidStackType;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.IStackType;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.ItemStackType;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.StackCreater;
import com.wintercogs.beyonddimensions.Api.DataBase.StackHandlerWrapper.FluidHandlerWrapper;
import com.wintercogs.beyonddimensions.Api.DataBase.StackHandlerWrapper.IStackHandlerWrapper;
import com.wintercogs.beyonddimensions.Api.Registry.CapabilityHelper;
import com.wintercogs.beyonddimensions.Api.Registry.StackHandlerWrapperHelper;
import com.wintercogs.beyonddimensions.Menu.BDBaseMenu;
import com.wintercogs.beyonddimensions.Network.Packet.toClient.OrderedStackTypedSlotPacket;
import com.wintercogs.beyonddimensions.Registry.PacketRegister;
import com.wintercogs.beyonddimensions.Unit.BDMath;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

public class OrderedStackTypedSlot extends AbstractStackTypedSlot
{
    private IStackType lastStack = new ItemStackType();

    public OrderedStackTypedSlot(BDBaseMenu menu, IStackTypedHandler stackTypedHandler, int slotIndex, int xPosition, int yPosition)
    {
        super(menu, stackTypedHandler, slotIndex, xPosition, yPosition);
    }

    public OrderedStackTypedSlot(BDBaseMenu menu, IStackTypedHandler stackTypedHandler, int slotIndex, int quickMoveSlotStartIndex, int quickMoveSlotEndIndex, int xPosition, int yPosition)
    {
        super(menu, stackTypedHandler, slotIndex, quickMoveSlotStartIndex, quickMoveSlotEndIndex, xPosition, yPosition);
    }

    @Override
    public boolean isOrdered()
    {
        return true;
    }

    @Override
    public void click(IStackType clickStack, int button, Player player)
    {
        ItemStack carriedItem = menu.getCarried().copy();// getCarried方法获取直接引用，所以需要copy防止误操作

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
                        Object handler = carriedItem.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM);
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
                                    int remaining = (int)storage.insert(getSlotIndex(),stack.copyWithCount(changedCount),true).getStackAmount();
                                    if(remaining<=0)
                                    {
                                        // 执行实际逻辑
                                        storage.insert(getSlotIndex(),stack.copyWithCount(changedCount),false).getStackAmount();
                                        menu.setCarried(new ItemStack(Items.BUCKET));
                                        handled.set(true);
                                    }
                                }
                            }
                        }
                    }
                    else
                    {
                        CapabilityHelper.ItemCapabilityMap.forEach((typeId, cap)->{
                            Object handler = carriedItem.getCapability(cap);
                            if(handler != null)
                            {
                                Function handlerGetter = StackHandlerWrapperHelper.stackWrappers.get(typeId);
                                IStackHandlerWrapper stackHandlerWrapper = (IStackHandlerWrapper) handlerGetter.apply(handler);

                                if(stackHandlerWrapper.getSlots()>0)
                                {
                                    for(int index=0;index<stackHandlerWrapper.getSlots();index++)
                                    {
                                        IStackType stack = StackCreater.Create(typeId,stackHandlerWrapper.getStackInSlot(index));
                                        if(stack !=null&& !stack.isEmpty())
                                        {
                                            int changedCount = BDMath.clampLongToInt(Math.min(stack.getStackAmount(),stack.getVanillaMaxStackSize()));
                                            int remaining = (int)storage.insert(getSlotIndex(),stack.copyWithCount(changedCount),false).getStackAmount();
                                            int actualInsert = changedCount - remaining;

                                            if(actualInsert>0)
                                            {
                                                long actualExtracts = stackHandlerWrapper.extract(index,actualInsert,false);
                                                if(actualExtracts< actualInsert)
                                                {
                                                    // 对此进行一个回调
                                                    storage.extract(getSlotIndex(), actualInsert-actualExtracts,false);
                                                }
                                                menu.setCarried(carriedItem.copy()); // 重设持有物以应用修改后的handler
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
                    int remaining = (int)storage.insert(getSlotIndex(),StackCreater.Create(ItemStackType.ID, carriedItem.copyWithCount(changedCount),changedCount),false).getStackAmount();
                    int actualInsert = changedCount - remaining; // 实际被插入的物品数量

                    int newCount = carriedItem.getCount() - actualInsert; // 实际剩余物品数
                    if(newCount <=0)
                    {
                        menu.setCarried(ItemStack.EMPTY);
                    }
                    else
                    {
                        ItemStack newCarriedItem = carriedItem.copy();
                        newCarriedItem.setCount(newCount);
                        menu.setCarried(newCarriedItem);
                    }
                }

            }
        }
        else if (mayPickup(player))
        {
            if (carriedItem.isEmpty())
            {   //槽位物品存在，携带物品为空，尝试取出槽位物品
                if(clickStack instanceof ItemStackType clickItem)
                {
                    // 确保一次取出最大不得超过原版数量
                    int woundChangeNum = BDMath.clampLongToInt(Math.min(clickItem.getStackAmount(), clickItem.getVanillaMaxStackSize()));
                    int actualChangeNum = button == GLFW.GLFW_MOUSE_BUTTON_LEFT ? woundChangeNum : (woundChangeNum + 1) / 2;
                    ItemStack takenItem = ((ItemStack) storage.extract(getSlotIndex(),actualChangeNum,false).getStack()).copy();
                    if(takenItem != null)
                    {
                        menu.setCarried(takenItem);
                        storage.onChange();
                    }
                }
            }
            else if (mayPlace(carriedItem))
            {
                // 槽位物品存在，携带物品存在，当物品为相同类型，尝试插入物品
                if(clickStack.isSameTypeSameComponents(new ItemStackType(carriedItem.copy())))
                {
                    int changedCount = button == GLFW.GLFW_MOUSE_BUTTON_LEFT ? carriedItem.getCount() : 1;
                    int remaining =  (int)storage.insert(getSlotIndex(),StackCreater.Create(ItemStackType.ID,carriedItem.copyWithCount(changedCount),changedCount),false).getStackAmount();
                    int actualInsert = changedCount - remaining; // 实际被插入的物品数量
                    int newCount = carriedItem.getCount() - actualInsert; // 实际剩余物品数
                    if(newCount <=0)
                    {
                        menu.setCarried(ItemStack.EMPTY);
                    }
                    else
                    {
                        ItemStack newCarriedItem = carriedItem.copy();
                        newCarriedItem.setCount(newCount);
                        menu.setCarried(newCarriedItem);
                    }
                }
                else
                {
                    // 槽位物品存在，携带物品存在，不为相同类型
                    // 尝试遍历能力，将槽位物品送入携带物品的存储
                    if(carriedItem.getCount() == 1 && button == GLFW.GLFW_MOUSE_BUTTON_RIGHT)
                    {
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
                                            && fluidStackType.getStackAmount()>=1000)
                                    {
                                        // 执行操作
                                        storage.extract(getSlotIndex(),1000,false);
                                        menu.setCarried(new ItemStack(filledBucket));
                                    }
                                }
                            }
                            else // 继续投放 insert模拟会自动解决类型不匹配等问题
                            {
                                Object handler = carriedItem.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM);
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
                                            int remaining = (int)storage.insert(getSlotIndex(),stack.copyWithCount(changedCount),true).getStackAmount();
                                            if(remaining<=0)
                                            {
                                                // 执行实际逻辑
                                                storage.insert(getSlotIndex(),stack.copyWithCount(changedCount),false).getStackAmount();
                                                menu.setCarried(new ItemStack(Items.BUCKET));
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        else
                        {
                            CapabilityHelper.ItemCapabilityMap.forEach((typeId,cap) -> {
                                // 先查看被点击物品的种类和对应能力种类
                                if(clickStack.getTypeId().equals(typeId))
                                {
                                    // 尝试获取对应能力
                                    Object handler = carriedItem.getCapability(cap);
                                    if(handler != null)
                                    {
                                        Function handlerGetter = StackHandlerWrapperHelper.stackWrappers.get(typeId);
                                        IStackHandlerWrapper stackHandlerWrapper = (IStackHandlerWrapper) handlerGetter.apply(handler);
                                        if(stackHandlerWrapper.getSlots()>0)
                                        {
                                            // 获取真实最大值 防止数据包伪造
                                            IStackType trueStack = storage.getStackBySlot(getSlotIndex());
                                            long tureCount = 0;
                                            if(trueStack.isSameTypeSameComponents(clickStack))
                                            {
                                                tureCount = trueStack.getStackAmount();
                                            }
                                            int changedCount = BDMath.clampLongToInt(Math.min(tureCount,clickStack.getVanillaMaxStackSize()));
                                            int remaining = (int)stackHandlerWrapper.insert(clickStack.copyStack(),false);
                                            int actualInsert = changedCount - remaining;
                                            storage.extract(getSlotIndex(),actualInsert,false);
                                            menu.setCarried(carriedItem.copy()); // 重设持有物以应用修改后的handler
                                        }
                                    }
                                }
                            });
                        }
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

    @Override
    public void quickMove(IStackType clickStack, int button, Player player)
    {
        if (!clickStack.isEmpty())
        {
            // 防止数据包伪造，然后赋予trueStack需要提取的数量
            IStackType trueStack = storage.getStackBySlot(theSlot).copyWithCount(clickStack.getStackAmount());

            // 遍历目标槽位
            for(int targetSlotIndex=quickMoveSlotStartIndex;targetSlotIndex<quickMoveSlotEndIndex && !trueStack.isEmpty();targetSlotIndex++)
            {
                Slot slot = menu.slots.get(targetSlotIndex);
                if(slot instanceof AbstractStackTypedSlot aSlot)
                {
                    // aSlot处理任何情况

                    //首先尝试从存储提取指定堆叠
                    IStackType extract = safeExtract(trueStack);
                    IStackType remaining = aSlot.safeInsert(extract); // 然后插入到其他堆叠并获取余量
                    if(!remaining.isEmpty())
                        safeInsert(remaining); // 最后将余量返回
                    trueStack = remaining.copy();

                }
                else
                {
                    // 普通Slot将只处理物品转移
                    if(trueStack instanceof ItemStackType trueItemTypedStack)
                    {
                        ItemStack extract = (ItemStack) safeExtract(trueItemTypedStack).getStack();
                        ItemStack remaining = slot.safeInsert(extract);
                        if(!remaining.isEmpty())
                            safeInsert(new ItemStackType(remaining));
                        trueStack = new ItemStackType(remaining.copy());
                    }
                }

            }
            setChanged();
        }
    }

    @Override
    public IStackType safeInsert(IStackType stack)
    {
        if(stack != null)
        {
            // storage的insert应当考虑到一切情况
            return storage.insert(theSlot,stack,false);
        }
        return new ItemStackType();
    }

    @Override
    public IStackType safeExtract(IStackType stack)
    {
        if(stack != null && getStack() != null && stack.getTypeId().equals(getStack().getTypeId()) &&  stack.isSameTypeSameComponents(getStack()))
        {
            return storage.extract(theSlot,stack.getStackAmount(),false);
        }
        return stack;
    }

    @Override
    public void updateChange()
    {
        IStackType currentStack = storage.getStackBySlot(this.getSlotIndex());
        if(currentStack == null)
        {
            lastStack = new ItemStackType();
            PacketRegister.INSTANCE.send(PacketDistributor.PLAYER.with(()->(ServerPlayer)menu.player), new OrderedStackTypedSlotPacket(index,theSlot,lastStack,0));
        }
        else if(currentStack.isEmpty() && lastStack.isEmpty())
        {
        }
        else if(lastStack.getStackAmount() != currentStack.getStackAmount()
                ||!lastStack.getTypeId().equals(currentStack.getTypeId())
                ||!lastStack.isSameTypeSameComponents(currentStack))
        {
            lastStack = currentStack;
            PacketRegister.INSTANCE.send(PacketDistributor.PLAYER.with(()->(ServerPlayer)menu.player), new OrderedStackTypedSlotPacket(index,theSlot,lastStack,lastStack.getStackAmount()));
        }
    }

    @Override
    public void loadChange(int where ,IStackType newStack, long newAmount)
    {
        storage.setStackDirectly(where, newStack);
    }
}
