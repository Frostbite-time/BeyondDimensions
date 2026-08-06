package com.wintercogs.beyonddimensions.api.dimensionnet;

import com.wintercogs.beyonddimensions.api.dimensionnet.helper.UnifiedStorageBeforeExtractHandler;
import com.wintercogs.beyonddimensions.api.dimensionnet.helper.UnifiedStorageBeforeInsertHandler;
import com.wintercogs.beyonddimensions.api.storage.handler.impl.UnorderedStackHandlerRemoveZero;
import com.wintercogs.beyonddimensions.api.storage.key.IStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.api.storage.key.impl.EmptyStackKey;
import net.minecraft.tags.TagKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;


/**
 * 统一存储（与旧类同名）：
 * - 策略采用“到0即删除”（继承 UnorderedStackHandlerRemoveZero）
 * - 在 onChange() 时对接 DimensionsNet.setDirty()，再广播变更（调用 super.onChange()）
 * - 注意：此存储仅用于维度网络，维度网络也仅使用此存储，以便存放一些维度网络专用特性
 */
public class UnifiedStorage extends UnorderedStackHandlerRemoveZero
{

    /**
     * 对应的维度网络，仅用于持久化脏标记
     */
    private final DimensionsNet net;

    public UnifiedStorage(DimensionsNet net, UiTimestampPolicy uiTimestampPolicy)
    {
        super(uiTimestampPolicy);
        this.net = net;
    }

    public UnifiedStorage(DimensionsNet net, UiTimestampPolicy uiTimestampPolicy, long slotCapacity, int slotMaxSize)
    {
        super(uiTimestampPolicy, slotCapacity, slotMaxSize);
        this.net = net;
    }

    /**
     * 返回一个“完全不可用”的空实现（0容量、0槽位），对外表现为全空容器且不会做脏标记
     */
    public static UnifiedStorage getEmpty()
    {
        return new UnifiedStorage(null, UiTimestampPolicy.NONE, 0, 0)
        {
            @Override
            public void onChange()
            {
                // no-op：空实现不做脏标记也不广播
            }

            @Override
            public long getSlotCapacity(int slot)
            {
                return 0L;
            }

            @Override
            public boolean isEmpty()
            {
                return true;
            }
        };
    }

    @Nullable
    public DimensionsNet getNet()
    {
        return net;
    }

    @Override
    public void onChange()
    {
        if (net != null)
        {
            net.setDirty();
        }
        // 调用基类的广播逻辑（Any/Delta 订阅）
        super.onChange();
    }

    @Override
    public @NotNull KeyAmount insert(IStackKey<?> key, long amount, boolean simulate)
    {
        KeyAmount input = new KeyAmount(Objects.requireNonNullElse(key, EmptyStackKey.INSTANCE), amount);

        var info = UnifiedStorageBeforeInsertHandler.onBeforeInsert(input, net);

        if (info.cancel())
            return input;

        KeyAmount adjusted = info.beforeInsert();

        if (adjusted.isEmpty())
            return adjusted;

        return super.insert(adjusted.key(), adjusted.amount(), simulate);
    }

    @Override
    public @NotNull KeyAmount extract(int slot, long amount, boolean simulate)
    {
        if (amount <= 0L)
            return new KeyAmount(EmptyStackKey.INSTANCE, 0L);

        KeyAmount stack = getStackBySlot(slot);
        if (stack.isEmpty())
            return stack;

        return extract(stack.key(), amount, simulate, false);
    }

    @Override
    public @NotNull KeyAmount extract(IStackKey<?> key, long amount, boolean simulate, boolean fuzzy)
    {
        KeyAmount input = new KeyAmount(Objects.requireNonNullElse(key, EmptyStackKey.INSTANCE), amount);

        var info = UnifiedStorageBeforeExtractHandler.onBeforeExtract(input, net);

        if (info.cancel())
            return new KeyAmount(input.key(), 0);

        KeyAmount adjusted = info.beforeExtract();

        if (adjusted.isEmpty())
            return adjusted;

        return super.extract(adjusted.key(), adjusted.amount(), simulate, fuzzy);
    }

    @Override
    public @NotNull KeyAmount extract(TagKey<?> tagKey, long amount, boolean simulate)
    {
        var key = tag2stackMap.get(tagKey).stream().findFirst();
        if (key.isEmpty() || amount <= 0L)
            return new KeyAmount(EmptyStackKey.INSTANCE, 0L);

        return extract(key.get(), amount, simulate, false);
    }

}
