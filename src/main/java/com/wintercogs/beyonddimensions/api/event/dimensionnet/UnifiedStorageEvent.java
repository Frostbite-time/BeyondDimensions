package com.wintercogs.beyonddimensions.api.event.dimensionnet;

import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet;
import com.wintercogs.beyonddimensions.api.storage.key.IStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import net.neoforged.bus.api.ICancellableEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public abstract class UnifiedStorageEvent extends DimensionsNetEvent
{
    public UnifiedStorageEvent(DimensionsNet net)
    {
        super(net);
    }

    /**
     * 在插入操作实际开始之前执行，可以在此对待插入内容进行流式预处理。
     * <p>
     * 每个监听器都可以通过 {@link #setCurrentInsert(KeyAmount)} 修改本次待插入内容，
     * 后续监听器将继续处理修改后的值；{@link #getOriginalInsert()} 始终返回最初的请求。
     * 取消事件表示拒绝整个原始插入请求。
     */
    public static class BeforeInsert extends UnifiedStorageEvent implements ICancellableEvent
    {
        private final @NotNull KeyAmount originalInsert;
        private @NotNull KeyAmount currentInsert;
        private final boolean simulate;

        public BeforeInsert(DimensionsNet net, @NotNull KeyAmount originalInsert, boolean simulate)
        {
            super(net);
            this.originalInsert = Objects.requireNonNull(originalInsert);
            this.currentInsert = originalInsert;
            this.simulate = simulate;
        }

        /**
         * 获取未经任何监听器处理的原始插入请求。
         */
        public @NotNull KeyAmount getOriginalInsert()
        {
            return originalInsert;
        }

        /**
         * 获取当前处理链准备交给存储执行的插入请求。
         */
        public @NotNull KeyAmount getCurrentInsert()
        {
            return currentInsert;
        }

        /**
         * 修改当前待插入内容，修改后的值会继续传递给后续监听器。
         */
        public void setCurrentInsert(@NotNull KeyAmount currentInsert)
        {
            this.currentInsert = Objects.requireNonNull(currentInsert);
        }

        /**
         * 本次调用是否仅为模拟操作。
         */
        public boolean isSimulate()
        {
            return simulate;
        }
    }

    /**
     * 插入完成后发送，用于对外通知。
     * <p>
     * {@link #getAmount()} 表示本次实际插入的增量，
     * {@link #getCurrentAmount()} 表示操作完成后该资源在存储中的绝对数量。
     */
    public static class onInsert extends UnifiedStorageEvent
    {
        private final @NotNull IStackKey<?> key;
        private final long amount;
        private final long currentAmount;

        public onInsert(DimensionsNet net, @NotNull IStackKey<?> key, long amount, long currentAmount)
        {
            super(net);
            this.key = Objects.requireNonNull(key);
            this.amount = amount;
            this.currentAmount = currentAmount;
        }

        public @NotNull IStackKey<?> getKey()
        {
            return key;
        }

        /**
         * 获取本次实际插入的数量。
         */
        public long getAmount()
        {
            return amount;
        }

        /**
         * 获取插入完成后该资源在存储中的绝对数量。
         */
        public long getCurrentAmount()
        {
            return currentAmount;
        }
    }

    /**
     * 在提取操作实际开始之前执行，可以在此对待提取内容进行流式预处理。
     * <p>
     * 每个监听器都可以通过 {@link #setCurrentExtract(KeyAmount)} 修改本次待提取内容，
     * 后续监听器将继续处理修改后的值；{@link #getOriginalExtract()} 始终返回最初的请求。
     * 取消事件表示拒绝整个提取请求。
     */
    public static class BeforeExtract extends UnifiedStorageEvent implements ICancellableEvent
    {
        private final @NotNull KeyAmount originalExtract;
        private @NotNull KeyAmount currentExtract;
        private final boolean simulate;

        public BeforeExtract(DimensionsNet net, @NotNull KeyAmount originalExtract, boolean simulate)
        {
            super(net);
            this.originalExtract = Objects.requireNonNull(originalExtract);
            this.currentExtract = originalExtract;
            this.simulate = simulate;
        }

        /**
         * 获取未经任何监听器处理的原始提取请求。
         */
        public @NotNull KeyAmount getOriginalExtract()
        {
            return originalExtract;
        }

        /**
         * 获取当前处理链准备交给存储执行的提取请求。
         */
        public @NotNull KeyAmount getCurrentExtract()
        {
            return currentExtract;
        }

        /**
         * 修改当前待提取内容，修改后的值会继续传递给后续监听器。
         */
        public void setCurrentExtract(@NotNull KeyAmount currentExtract)
        {
            this.currentExtract = Objects.requireNonNull(currentExtract);
        }

        /**
         * 本次调用是否仅为模拟操作。
         */
        public boolean isSimulate()
        {
            return simulate;
        }
    }

    /**
     * 提取完成后发送，用于对外通知。
     * <p>
     * {@link #getAmount()} 表示本次实际提取的增量，
     * {@link #getCurrentAmount()} 表示操作完成后该资源在存储中的绝对数量。
     */
    public static class onExtract extends UnifiedStorageEvent
    {
        private final @NotNull IStackKey<?> key;
        private final long amount;
        private final long currentAmount;

        public onExtract(DimensionsNet net, @NotNull IStackKey<?> key, long amount, long currentAmount)
        {
            super(net);
            this.key = Objects.requireNonNull(key);
            this.amount = amount;
            this.currentAmount = currentAmount;
        }

        public @NotNull IStackKey<?> getKey()
        {
            return key;
        }

        /**
         * 获取本次实际提取的数量。
         */
        public long getAmount()
        {
            return amount;
        }

        /**
         * 获取提取完成后该资源在存储中的绝对数量。
         */
        public long getCurrentAmount()
        {
            return currentAmount;
        }
    }
}
