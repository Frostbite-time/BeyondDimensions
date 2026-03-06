package com.wintercogs.beyonddimensions.api.capability.helper.wrapper;

import com.buuz135.industrialforegoingsouls.capabilities.ISoulHandler;
import com.wintercogs.beyonddimensions.api.longtype.WardenSoulType;
import com.wintercogs.beyonddimensions.api.storage.key.impl.WardenSoulStackKey;
import net.minecraft.resources.ResourceLocation;

public class WardenSoulHandlerWrapper implements IStackHandlerWrapper<WardenSoulType>
{
    private final ISoulHandler soulHandler;

    public WardenSoulHandlerWrapper(Object soulHandler)
    {
        this.soulHandler = (ISoulHandler) soulHandler;
    }


    @Override
    public ResourceLocation getTypeId()
    {
        return WardenSoulStackKey.ID;
    }

    @Override
    public int getSlots()
    {
        return soulHandler.getSoulTanks();
    }

    @Override
    public WardenSoulType getStackInSlot(int slot)
    {
        return new WardenSoulType(soulHandler.getSoulInTank(slot));
    }

    @Override
    public long getCapacity(int slot)
    {
        return soulHandler.getTankCapacity(slot);
    }

    @Override
    public boolean isStackValid(int slot, WardenSoulType stack)
    {
        return true;
    }

    @Override
    public long insert(int slot, WardenSoulType stack, boolean sim)
    {
        long amount = stack.getStackCount();
        // 确保请求的插入量在int范围内（Max: 2,147,483,647）
        int insertAmount = (amount > Integer.MAX_VALUE) ? Integer.MAX_VALUE : (int) amount;
        ISoulHandler.Action action = sim ? ISoulHandler.Action.SIMULATE : ISoulHandler.Action.EXECUTE;

        // 获取实际接受量
        int accepted = soulHandler.fill(insertAmount, action);

        // 计算未接收的余量 = 请求总量 - 实际接受量
        return amount - accepted;
    }

    @Override
    public long insert(WardenSoulType stack, boolean sim)
    {
        long amount = stack.getStackCount();
        // 确保请求的插入量在int范围内（Max: 2,147,483,647）
        int insertAmount = (amount > Integer.MAX_VALUE) ? Integer.MAX_VALUE : (int) amount;
        ISoulHandler.Action action = sim ? ISoulHandler.Action.SIMULATE : ISoulHandler.Action.EXECUTE;

        // 获取实际接受量
        int accepted = soulHandler.fill(insertAmount, action);

        // 计算未接收的余量 = 请求总量 - 实际接受量
        return amount - accepted;
    }

    @Override
    public long extract(int slot, long amount, boolean sim)
    {
        int extractAmount = (amount > Integer.MAX_VALUE) ? Integer.MAX_VALUE : (int) amount;
        if (extractAmount < 0) extractAmount = 0;
        ISoulHandler.Action action = sim ? ISoulHandler.Action.SIMULATE : ISoulHandler.Action.EXECUTE;
        return soulHandler.drain(extractAmount, action);
    }

    @Override
    public long extract(WardenSoulType stack, boolean sim)
    {
        int extractAmount = (stack.getStackCount() > Integer.MAX_VALUE) ? Integer.MAX_VALUE : (int) stack.getStackCount();
        if (extractAmount < 0) extractAmount = 0;
        ISoulHandler.Action action = sim ? ISoulHandler.Action.SIMULATE : ISoulHandler.Action.EXECUTE;
        return soulHandler.drain(extractAmount, action);
    }
}
