package com.wintercogs.beyonddimensions.Api.DataBase.Stack;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.NotNull;

/**
 * 一个包含key和amount的记录类，极其轻量
 * <p>
 * 一般仅作于外部的只读视图
 */
public record KeyAmount(@NotNull IStackKey<?> key, long amount)
{

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

    static void serialize(FriendlyByteBuf buf, KeyAmount ka)
    {
        IStackKey.serializeCommon(buf, ka.key());
        buf.writeVarLong(ka.amount());
    }

    @NotNull
    static KeyAmount deserialize(FriendlyByteBuf buf)
    {
        IStackKey<?> key = IStackKey.deserializeCommon(buf);
        long amount = buf.readVarLong();
        return new KeyAmount(key, amount);
    }

    static CompoundTag serializeNBT(KeyAmount ka)
    {
        CompoundTag nbt = new CompoundTag();
        nbt.put("key", IStackKey.serializeNBTCommon(ka.key()));
        nbt.putLong("amount", ka.amount());
        return nbt;
    }

    @NotNull
    static KeyAmount deserializeNBT(CompoundTag nbt)
    {
        IStackKey<?> key = IStackKey.deserializeNBTCommon(nbt.getCompound("key"));
        long amount = nbt.getLong("amount");
        return new KeyAmount(key, amount);
    }
}
