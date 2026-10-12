package com.wintercogs.beyonddimensions.api.storage.handler.impl;

import com.wintercogs.beyonddimensions.api.storage.key.IStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.api.storage.key.impl.EmptyStackKey;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * 另一个 {@link StackHandler} 的视图：读取原样转发，可以禁止放入或取出，用于按面限制方块的能力。
 * 它本身不存任何东西，StackHandler 的每个公开方法都要在这里转发，新增方法时同样
 */
public class RestrictedStackHandler extends StackHandler
{
    private final StackHandler delegate;
    private final boolean allowsInput;
    private final boolean allowsOutput;

    public RestrictedStackHandler(StackHandler delegate, boolean allowsInput, boolean allowsOutput)
    {
        super(0);
        this.delegate = delegate;
        this.allowsInput = allowsInput;
        this.allowsOutput = allowsOutput;
    }

    // 直接改写会绕过放入与取出的限制，只在两者都允许时转发
    private boolean unrestricted()
    {
        return allowsInput && allowsOutput;
    }

    private static KeyAmount refused(@Nullable IStackKey<?> key, long amount)
    {
        return key == null || key == EmptyStackKey.INSTANCE || amount <= 0L
                ? new KeyAmount(EmptyStackKey.INSTANCE, 0L)
                : new KeyAmount(key, amount);
    }

    @Override
    public Optional<SlotBucket> getBucket(ResourceLocation typeId)
    {
        return delegate.getBucket(typeId);
    }

    @Override
    public Optional<SlotBucket> getBucket(IStackKey<?> key)
    {
        return delegate.getBucket(key);
    }

    @Override
    public List<KeyAmount> getStorage()
    {
        return delegate.getStorage();
    }

    @Override
    public void onChange()
    {
        delegate.onChange();
    }

    @Override
    public int getSlots()
    {
        return delegate.getSlots();
    }

    @Override
    public void clearStorage()
    {
        if (unrestricted())
            delegate.clearStorage();
    }

    @Override
    public @NotNull KeyAmount getStackBySlot(int slot)
    {
        return delegate.getStackBySlot(slot);
    }

    @Override
    public @NotNull KeyAmount getStackByKey(IStackKey<?> key)
    {
        return delegate.getStackByKey(key);
    }

    @Override
    public boolean hasStack(IStackKey<?> key)
    {
        return delegate.hasStack(key);
    }

    @Override
    public void setStackDirectly(int slot, IStackKey<?> key, long amount)
    {
        if (unrestricted())
            delegate.setStackDirectly(slot, key, amount);
    }

    @Override
    public void addStackDirectly(IStackKey<?> key, long amount)
    {
        if (unrestricted())
            delegate.addStackDirectly(key, amount);
    }

    @Override
    public @NotNull KeyAmount insert(int slot, IStackKey<?> key, long amount, boolean simulate)
    {
        return allowsInput ? delegate.insert(slot, key, amount, simulate) : refused(key, amount);
    }

    @Override
    public @NotNull KeyAmount insert(IStackKey<?> key, long amount, boolean simulate)
    {
        return allowsInput ? delegate.insert(key, amount, simulate) : refused(key, amount);
    }

    @Override
    public @NotNull KeyAmount extract(int slot, long count, boolean simulate)
    {
        return allowsOutput ? delegate.extract(slot, count, simulate) : new KeyAmount(EmptyStackKey.INSTANCE, 0L);
    }

    @Override
    public @NotNull KeyAmount extract(IStackKey<?> key, long amount, boolean simulate, boolean fuzzy)
    {
        return allowsOutput ? delegate.extract(key, amount, simulate, fuzzy) : new KeyAmount(EmptyStackKey.INSTANCE, 0L);
    }

    @Override
    public long getSlotCapacity(int slot)
    {
        return delegate.getSlotCapacity(slot);
    }

    @Override
    public boolean isStackValid(int slot, IStackKey<?> key)
    {
        return allowsInput && delegate.isStackValid(slot, key);
    }

    @Override
    public boolean isEmpty()
    {
        return delegate.isEmpty();
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider)
    {
        return delegate.serializeNBT(provider);
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag)
    {
        if (unrestricted())
            delegate.deserializeNBT(provider, tag);
    }

    @Override
    public @Nullable Object getOutStackByKey(IStackKey<?> key)
    {
        return delegate.getOutStackByKey(key);
    }
}
