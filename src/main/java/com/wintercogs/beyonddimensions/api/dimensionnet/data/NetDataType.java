package com.wintercogs.beyonddimensions.api.dimensionnet.data;

import com.mojang.serialization.Codec;
import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet;
import com.wintercogs.beyonddimensions.api.registry.BDRegistries;

import java.util.Objects;
import java.util.function.BinaryOperator;
import java.util.function.Supplier;

/**
 * 挂接在维度网络上的一种数据。附属注册到 {@link BDRegistries#NET_DATA_TYPE_KEY}，数据随网络一起保存：
 * <pre>{@code
 * public static final DeferredRegister<NetDataType<?>> NET_DATA =
 *         DeferredRegister.create(BDRegistries.NET_DATA_TYPE_KEY, "myaddon");
 * public static final Supplier<NetDataType<Upgrades>> UPGRADES = NET_DATA.register("upgrades",
 *         () -> NetDataType.builder(Upgrades::empty)
 *                 .codec(Upgrades.CODEC)
 *                 .merge(Upgrades::combine)
 *                 .build());
 *
 * Upgrades upgrades = net.getData(UPGRADES.get());
 * net.updateData(UPGRADES.get(), u -> u.withSpeed(3));
 * }</pre>
 * 值宜为不可变对象，通过 {@link DimensionsNet#setData} 或 {@link DimensionsNet#updateData} 修改；可变对象改动后需调用
 * {@link DimensionsNet#setDirty()}。数据只在服务端，需要在客户端显示时由附属自行同步。
 * <p>
 * 存档里类型未注册（附属未安装）的数据原样保留，附属装回后恢复；类型已注册但解析失败的数据隔离保存，不会被覆盖。
 */
public final class NetDataType<T>
{
    private final Supplier<T> defaultValue;
    private final Codec<T> codec;
    private final BinaryOperator<T> merge;

    private NetDataType(Supplier<T> defaultValue, Codec<T> codec, BinaryOperator<T> merge)
    {
        this.defaultValue = defaultValue;
        this.codec = codec;
        this.merge = merge;
    }

    /**
     * @param defaultValue 网络还没有这种数据时的初始值
     */
    public static <T> Builder<T> builder(Supplier<T> defaultValue)
    {
        return new Builder<>(Objects.requireNonNull(defaultValue));
    }

    public T createDefault()
    {
        return Objects.requireNonNull(defaultValue.get(), "NetDataType default value");
    }

    public Codec<T> codec()
    {
        return codec;
    }

    /**
     * 合并两个网络时调用
     *
     * @param kept     留下的网络的值
     * @param absorbed 被合并、随后销毁的网络的值
     * @return 合并后留下的值
     */
    public T merge(T kept, T absorbed)
    {
        return Objects.requireNonNull(merge.apply(kept, absorbed), "NetDataType merge result");
    }

    public static final class Builder<T>
    {
        private final Supplier<T> defaultValue;
        private Codec<T> codec;
        private BinaryOperator<T> merge;

        private Builder(Supplier<T> defaultValue)
        {
            this.defaultValue = defaultValue;
        }

        /**
         * 保存与读取的编解码器，必须提供
         */
        public Builder<T> codec(Codec<T> codec)
        {
            this.codec = Objects.requireNonNull(codec);
            return this;
        }

        /**
         * 两个网络合并且都有这种数据时如何合并，必须提供
         */
        public Builder<T> merge(BinaryOperator<T> merge)
        {
            this.merge = Objects.requireNonNull(merge);
            return this;
        }

        /**
         * @throws IllegalStateException 没有提供编解码器或合并方式
         */
        public NetDataType<T> build()
        {
            if (codec == null)
                throw new IllegalStateException("NetDataType needs a codec");
            if (merge == null)
                throw new IllegalStateException("NetDataType needs a merge function");
            return new NetDataType<>(defaultValue, codec, merge);
        }
    }
}
