package com.wintercogs.beyonddimensions.integration.module.ae2lt.storage;

import com.moakiee.ae2lt.api.lightning.ILightningEnergyHandler;
import com.moakiee.ae2lt.api.lightning.LightningTier;
import com.wintercogs.beyonddimensions.api.capability.helper.wrapper.IStackHandlerWrapper;
import net.minecraft.resources.ResourceLocation;

public final class LightningHandlerWrapper implements IStackHandlerWrapper<LightningType>
{
    private final ILightningEnergyHandler handler;

    public LightningHandlerWrapper(Object handler)
    {
        this.handler = (ILightningEnergyHandler) handler;
    }

    private static LightningTier tier(int slot)
    {
        return switch (slot)
        {
            case 0 -> LightningTier.HIGH_VOLTAGE;
            case 1 -> LightningTier.EXTREME_HIGH_VOLTAGE;
            default -> throw new IndexOutOfBoundsException("Invalid lightning slot: " + slot);
        };
    }

    @Override
    public ResourceLocation getTypeId()
    {
        return LightningStackKey.ID;
    }

    @Override
    public int getSlots()
    {
        return 2;
    }

    @Override
    public LightningType getStackInSlot(int slot)
    {
        LightningTier tier = tier(slot);
        return new LightningType(tier, handler.getStored(tier));
    }

    @Override
    public long getCapacity(int slot)
    {
        return handler.getCapacity(tier(slot));
    }

    @Override
    public boolean isStackValid(int slot, LightningType stack)
    {
        return stack.tier() == tier(slot) && handler.canInsert(stack.tier());
    }

    @Override
    public long insert(int slot, LightningType stack, boolean simulate)
    {
        long amount = Math.max(0, stack.getStackCount());
        if (stack.tier() != tier(slot)) return amount;
        return amount - handler.insert(stack.tier(), amount, simulate);
    }

    @Override
    public long insert(LightningType stack, boolean simulate)
    {
        long amount = Math.max(0, stack.getStackCount());
        return amount - handler.insert(stack.tier(), amount, simulate);
    }

    @Override
    public long extract(int slot, long amount, boolean simulate)
    {
        return handler.extract(tier(slot), Math.max(0, amount), simulate);
    }

    @Override
    public long extract(LightningType stack, boolean simulate)
    {
        return handler.extract(stack.tier(), Math.max(0, stack.getStackCount()), simulate);
    }
}
