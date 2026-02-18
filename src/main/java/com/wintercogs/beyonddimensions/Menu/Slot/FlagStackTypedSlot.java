package com.wintercogs.beyonddimensions.Menu.Slot;

import com.wintercogs.beyonddimensions.Api.DataBase.Handler.IStackHandler;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.*;
import com.wintercogs.beyonddimensions.Api.DataBase.StackHandlerWrapper.IStackHandlerWrapper;
import com.wintercogs.beyonddimensions.Api.Registry.CapabilityHelper;
import com.wintercogs.beyonddimensions.Api.Registry.StackHandlerWrapperHelper;
import com.wintercogs.beyonddimensions.Api.Registry.StackKeyRegistry;
import com.wintercogs.beyonddimensions.Fluid.ModFluids;
import com.wintercogs.beyonddimensions.Item.Custom.XpExchangeItem;
import com.wintercogs.beyonddimensions.Menu.BDBaseMenu;
import com.wintercogs.beyonddimensions.Packet.OrderedStackTypedSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.function.Function;

// 用于标记性槽位的AbstractStackTypedSlot实现
// 注意，标记性槽位必须用于有序容器
public class FlagStackTypedSlot extends AbstractStackTypedSlot
{

    private KeyAmount lastStack = new KeyAmount(ItemStackKey.EMPTY, 0);

    public FlagStackTypedSlot(BDBaseMenu menu, IStackHandler storage, int slotIndex, int xPosition, int yPosition)
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
    public void setStackDirectly(IStackKey<?> key, long amount)
    {
        storage.setStackDirectly(theSlot, key, amount);
    }

    @Override
    public KeyAmount safeInsert(IStackKey<?> key, long amount)
    {
        if (key != null)
        {
            setStackDirectly(key, amount);
        }
        return new KeyAmount(EmptyStackKey.INSTANCE, amount);
    }

    @Override
    public KeyAmount safeExtract(IStackKey<?> key, long amount)
    {
        setStackDirectly(ItemStackKey.EMPTY, amount);
        return new KeyAmount(ItemStackKey.EMPTY, amount); // 标记槽永远取出空
    }

    @Override
    public void click(KeyAmount clickStack, int button, Player player)
    {
        // 获取光标物品
        ItemStack carriedItem = menu.getCarried().copy();

        if (clickStack.isEmpty())
        {
            if (!carriedItem.isEmpty())
            {   //槽位物品为空，携带物品存在，将携带物品插入标记

                if (button == 0)
                {
                    setStackDirectly(new ItemStackKey(carriedItem), 1);
                }
                else if (button == 1)
                {
                    if (carriedItem.getItem() instanceof XpExchangeItem)
                    {
                        setStackDirectly(new FluidStackKey(new FluidStack(ModFluids.XP_FLUID.source(), 1)), 1);
                    }
                    else
                    {
                        ItemStack copy = carriedItem.copy();
                        copy.setCount(1);
                        // 注: 通用机械物品必须在堆叠数量为1时才暴露能力。
                        // 这种做法看起来是很有益的。可以防止其他模组错误消耗过多的存储资源
                        CapabilityHelper.ItemCapabilityMap.forEach((typeId, cap) -> {
                            Object handler = copy.getCapability(cap);
                            if (handler != null)
                            {
                                Function handlerGetter = StackHandlerWrapperHelper.stackWrappers.get(typeId);
                                IStackHandlerWrapper stackHandlerWrapper = (IStackHandlerWrapper) handlerGetter.apply(handler);

                                if (stackHandlerWrapper.getSlots() > 0)
                                {
                                    for (int index = 0; index < stackHandlerWrapper.getSlots(); index++)
                                    {
                                        IStackKey<?> typeKey = StackKeyRegistry.getType(typeId);
                                        KeyAmount typeStack = typeKey.fromStackObject(stackHandlerWrapper.getStackInSlot(0));
                                        if (typeStack != null)
                                        {
                                            KeyAmount stack = new KeyAmount(typeStack.key(), 1);
                                            if (!stack.isEmpty())
                                            {
                                                setStackDirectly(stack.key(), stack.amount());
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
        }
        else
        {
            if (carriedItem.isEmpty())
            {
                //槽位物品存在，携带物品为空，尝试清空标记
                setStackDirectly(ItemStackKey.EMPTY, 0);
            }
            else if (true)
            {   //槽位物品存在，携带物品存在，物品可以放置，取消标记

                setStackDirectly(ItemStackKey.EMPTY, 0);
            }
            else if (clickStack.key().isSameTypeSameComponents(new ItemStackKey(carriedItem)))
            {   // 槽位物品存在，携带物品存在，物品不可放置，为完全相同的物品

            }

        }
    }

    // 标记性槽位不能进行快速转移
    // 任何快速转移的意图直接移交给click处理
    @Override
    public void quickMove(KeyAmount clickStack, int button, Player player)
    {
        // flag的quickMove和click走统一通道，因此无需额外检查，此处保留注释，防止某一天忘记
        // if(!(quickMoveSlotStartIndex >= 0 && quickMoveSlotEndIndex >= 0 && quickMoveSlotStartIndex < quickMoveSlotEndIndex))
        //   return;
        click(clickStack, button, player);
    }

    @Override
    public void updateChange()
    {
        KeyAmount currentStack = storage.getStackBySlot(this.getSlotIndex());
        if (lastStack.amount() != currentStack.amount()
                || !lastStack.key().getTypeId().equals(currentStack.key().getTypeId())
                || !lastStack.key().isSameTypeSameComponents(currentStack.key()))
        {
            lastStack = currentStack;
            PacketDistributor.sendToPlayer((ServerPlayer) menu.player, new OrderedStackTypedSlotPacket(index, theSlot, lastStack.key(), lastStack.amount()));
        }
    }

    @Override
    public void loadChange(int where, IStackKey<?> newKey, long newAmount)
    {
        // 同步读取仍直接操作storage
        storage.setStackDirectly(where, newKey, newAmount);
    }
}
