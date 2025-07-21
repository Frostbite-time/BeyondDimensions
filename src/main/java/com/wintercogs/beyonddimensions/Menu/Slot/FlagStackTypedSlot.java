package com.wintercogs.beyonddimensions.Menu.Slot;

import com.wintercogs.beyonddimensions.Api.DataBase.Handler.IStackTypedHandler;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.IStackType;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.ItemStackType;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.StackCreater;
import com.wintercogs.beyonddimensions.Api.DataBase.StackHandlerWrapper.IStackHandlerWrapper;
import com.wintercogs.beyonddimensions.Api.Registry.CapabilityHelper;
import com.wintercogs.beyonddimensions.Api.Registry.StackHandlerWrapperHelper;
import com.wintercogs.beyonddimensions.Menu.BDBaseMenu;
import com.wintercogs.beyonddimensions.Network.Packet.toClient.OrderedStackTypedSlotPacket;
import com.wintercogs.beyonddimensions.Registry.PacketRegister;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Function;

public class FlagStackTypedSlot extends AbstractStackTypedSlot
{

    private IStackType lastStack = new ItemStackType();

    public FlagStackTypedSlot(BDBaseMenu menu, IStackTypedHandler storage, int slotIndex, int xPosition, int yPosition)
    {
        super(menu, storage, slotIndex, xPosition, yPosition);
        setFake(true); // 标记性槽位为假槽位
    }

    @Override
    public boolean isOrdered()
    {
        return true;
    }


    // 内部会copy这个stack，因此无需再次操作
    // Flag实际上会通过insert插入，这样能考虑内部的isStackValid，从而限制标记类型
    @Override
    public void setStackDirectly(IStackType stack)
    {
        storage.setStackDirectly(theSlot, new ItemStackType());
        storage.insert(theSlot,stack,false);
    }

    @Override
    public IStackType safeInsert(IStackType stack)
    {
        if(stack != null)
        {
            setStackDirectly(stack);
        }
        return stack;
    }

    @Override
    public IStackType safeExtract(IStackType stack)
    {
        setStackDirectly(new ItemStackType());
        return stack;
    }

    @Override
    public void click(IStackType clickStack, int button, Player player)
    {
        // 获取光标物品
        ItemStack carriedItem = menu.getCarried().copy();

        if (clickStack.isEmpty())
        {
            if (!carriedItem.isEmpty())
            {   //槽位物品为空，携带物品存在，将携带物品插入标记

                if(button==0)
                {
                    ItemStack copy = carriedItem.copy();
                    copy.setCount(1);
                    setStackDirectly(new ItemStackType(copy));
                }
                else if(button==1)
                {
                    ItemStack copy = carriedItem.copy();
                    copy.setCount(1);
                    // 注: 通用机械物品必须在堆叠数量为1时才暴露能力。
                    // 这种做法看起来是很有益的。可以防止其他模组错误消耗过多的存储资源
                    CapabilityHelper.ItemCapabilityMap.forEach((typeId, cap)->{
                        LazyOptional<?> handler = copy.getCapability(cap);
                        if(handler.isPresent())
                        {
                            Function handlerGetter = StackHandlerWrapperHelper.stackWrappers.get(typeId);
                            IStackHandlerWrapper stackHandlerWrapper = (IStackHandlerWrapper) handlerGetter.apply(handler.resolve().get());

                            if(stackHandlerWrapper.getSlots()>0)
                            {
                                for(int index=0;index<stackHandlerWrapper.getSlots();index++)
                                {
                                    IStackType stack = StackCreater.Create(typeId,stackHandlerWrapper.getStackInSlot(0),1);
                                    if(stack!=null&& !stack.isEmpty())
                                    {
                                        setStackDirectly(stack);
                                        break;
                                    }
                                }
                            }
                        }
                    });

                }

            }
        }
        else
        {
            if (carriedItem.isEmpty())
            {
                //槽位物品存在，携带物品为空，尝试清空标记
                setStackDirectly(new ItemStackType());
            }
            else if (true)
            {   //槽位物品存在，携带物品存在，物品可以放置，取消标记
                setStackDirectly(new ItemStackType());
            }
            else if (clickStack.isSameTypeSameComponents(new ItemStackType(carriedItem.copy())))
            {   // 槽位物品存在，携带物品存在，物品不可放置，为完全相同的物品

            }

        }
    }

    // 标记性槽位不能进行快速转移
    // 任何快速转移的意图直接移交给click处理
    @Override
    public void quickMove(IStackType clickStack, int button, Player player)
    {
        click(clickStack, button, player);
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
        // 同步读取仍直接操作storage
        storage.setStackDirectly(where, newStack);
    }
}
