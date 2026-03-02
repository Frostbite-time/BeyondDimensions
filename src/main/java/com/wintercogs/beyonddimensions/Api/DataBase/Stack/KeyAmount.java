package com.wintercogs.beyonddimensions.Api.DataBase.Stack;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;

/**
 * 一个包含key和amount的记录类，极其轻量
 * <p>
 * 一般仅作于外部的只读视图
 */
public record KeyAmount(@NotNull IStackKey<?> key, long amount)
{
    private static final MapCodec<KeyAmount> TYPE_CODEC = RecordCodecBuilder.mapCodec(inst ->
            inst.group(
                    IStackKey.CODEC.fieldOf("key").forGetter(KeyAmount::key),
                    Codec.LONG.fieldOf("amount").forGetter(KeyAmount::amount)
            ).apply(inst, KeyAmount::new)
    );

    public static final Codec<KeyAmount> CODEC = TYPE_CODEC.codec();

    public static final StreamCodec<RegistryFriendlyByteBuf, KeyAmount> STREAM_CODEC =
            StreamCodec.composite(
                    IStackKey.STREAM_CODEC,
                    KeyAmount::key,
                    ByteBufCodecs.VAR_LONG,
                    KeyAmount::amount,
                    KeyAmount::new
            );

    public boolean isEmpty()
    {
        return amount <= 0L || key.isEmpty();
    }

    /**
     * 给出当前kv对所代表的实际stack副本，不支持long数量的stack可能会被内部实现自动限制到int上限
     */
    public Object toStack()
    {
        return key.copyStackWithCount(amount);
    }
}
