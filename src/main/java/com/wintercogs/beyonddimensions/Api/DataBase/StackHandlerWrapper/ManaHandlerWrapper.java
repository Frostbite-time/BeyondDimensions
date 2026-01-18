package com.wintercogs.beyonddimensions.Api.DataBase.StackHandlerWrapper;

import com.wintercogs.beyonddimensions.Api.DataBase.LongType.ManaType;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.ManaStackType;
import com.wintercogs.beyonddimensions.Unit.BDMath;
import net.minecraft.resources.ResourceLocation;
import vazkii.botania.api.mana.ManaCollector;
import vazkii.botania.api.mana.ManaItem;
import vazkii.botania.api.mana.ManaPool;
import vazkii.botania.api.mana.ManaReceiver;
import vazkii.botania.api.mana.spark.SparkAttachable;

public class ManaHandlerWrapper implements IStackHandlerWrapper<ManaType>
{
    private final ManaContainerWrapper container;

    public ManaHandlerWrapper(Object mana_item_or_receiver)
    {
        this.container = new ManaContainerWrapper(mana_item_or_receiver);
    }

    @Override
    public ResourceLocation getTypeId()
    {
        return ManaStackType.ID;
    }

    @Override
    public int getSlots()
    {
        return 1;
    }

    @Override
    public ManaType getStackInSlot(int slot)
    {
        return new ManaType(container.getMana());
    }

    @Override
    public long getCapacity(int slot)
    {
        return container.getMaxMana();
    }

    @Override
    public boolean isStackValid(int slot, ManaType stack)
    {
        return true;
    }

    @Override
    public long insert(int slot, ManaType stack, boolean sim)
    {
        long manaInsert = Math.min(stack.getStackCount(), getCapacity(0) - getStackInSlot(0).getStackCount());
        int actInsert = BDMath.clampLongToInt(manaInsert);
        if (!sim)
            container.receiveMana(actInsert);
        return stack.getStackCount() - actInsert;
    }

    @Override
    public long insert(ManaType stack, boolean sim)
    {
        long manaInsert = Math.min(stack.getStackCount(), getCapacity(0) - getStackInSlot(0).getStackCount());
        int actInsert = BDMath.clampLongToInt(manaInsert);
        if (!sim)
            container.receiveMana(actInsert);
        return stack.getStackCount() - actInsert; // 我也不知道插入了多少，但总之，就算有浪费也反不回来
    }

    @Override
    public long extract(int slot, long amount, boolean sim)
    {
        int actExtract = BDMath.clampLongToInt(Math.min(amount, getStackInSlot(0).getStackCount()));
        if (!sim)
            container.receiveMana(-actExtract);
        return actExtract;
    }

    @Override
    public long extract(ManaType stack, boolean sim)
    {
        int actExtract = BDMath.clampLongToInt(Math.min(stack.getStackCount(), getStackInSlot(0).getStackCount()));
        if (!sim)
            container.receiveMana(-actExtract);
        return actExtract;
    }

    private static class ManaContainerWrapper
    {
        private final ManaReceiver receiver;
        private final ManaItem itemReceiver;

        public ManaContainerWrapper(Object handler)
        {
            if (handler instanceof ManaReceiver)
            {
                receiver = (ManaReceiver) handler;
                itemReceiver = null;
            }
            else if (handler instanceof ManaItem)
            {
                receiver = null;
                itemReceiver = (ManaItem) handler;
            }
            else
            {
                receiver = null;
                itemReceiver = null;
            }
        }

        public int getMana()
        {
            if (receiver != null)
                return receiver.getCurrentMana();
            else if (itemReceiver != null)
                return itemReceiver.getMana();
            else
                return 0;
        }

        public int getMaxMana()
        {
            if (itemReceiver != null)
                return itemReceiver.getMaxMana();
            else if (receiver != null)
            {
                if (receiver instanceof ManaPool pool) // 魔力池
                {
                    return pool.getMaxMana();
                }
                else if (receiver instanceof ManaCollector collector) // 魔力收集器
                {
                    return collector.getMaxMana();
                }
                else if (receiver instanceof SparkAttachable sparkAttachable) // 火花附着
                {
                    return receiver.getCurrentMana() + sparkAttachable.getAvailableSpaceForMana();
                }
                else if (!receiver.isFull()) // 什么都不是！但是确定目前没有满
                {
                    return Math.max(1000, receiver.getCurrentMana());
                }
            }
            return 0;
        }

        public void receiveMana(int mana)
        {
            if (receiver != null)
                receiver.receiveMana(mana);
            else if (itemReceiver != null)
                itemReceiver.addMana(mana);
        }
    }
}
