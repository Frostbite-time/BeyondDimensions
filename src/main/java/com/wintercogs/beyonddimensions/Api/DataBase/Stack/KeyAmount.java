package com.wintercogs.beyonddimensions.Api.DataBase.Stack;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
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

    public static void serialize(FriendlyByteBuf buf, KeyAmount ka)
    {
        IStackKey.serializeCommon(buf, ka.key());
        buf.writeVarLong(ka.amount());
    }

    @NotNull
    public static KeyAmount deserialize(FriendlyByteBuf buf)
    {
        IStackKey<?> key = IStackKey.deserializeCommon(buf);
        long amount = buf.readVarLong();
        return new KeyAmount(key, amount);
    }

    public static CompoundTag serializeNBT(KeyAmount ka)
    {
        CompoundTag nbt = new CompoundTag();
        nbt.put("key", IStackKey.serializeNBTCommon(ka.key()));
        nbt.putLong("amount", ka.amount());
        return nbt;
    }

    @NotNull
    public static KeyAmount deserializeNBT(CompoundTag nbt)
    {
        IStackKey<?> key = IStackKey.deserializeNBTCommon(nbt.getCompound("key"));
        long amount = nbt.getLong("amount");
        return new KeyAmount(key, amount);
    }



    /**
     * 尽力从旧 TypedStack NBT 中读取数量。
     */
    private static long readLegacyAmount(CompoundTag typedStack)
    {
        if (typedStack.contains("amount", Tag.TAG_LONG)) return typedStack.getLong("amount");
        if (typedStack.contains("amount", Tag.TAG_INT)) return typedStack.getInt("amount");
        if (typedStack.contains("Amount", Tag.TAG_LONG)) return typedStack.getLong("Amount");
        if (typedStack.contains("Amount", Tag.TAG_INT)) return typedStack.getInt("Amount");
        if (typedStack.contains("count", Tag.TAG_LONG)) return typedStack.getLong("count");
        if (typedStack.contains("count", Tag.TAG_INT)) return typedStack.getInt("count");
        if (typedStack.contains("Count", Tag.TAG_BYTE)) return typedStack.getByte("Count") & 0xFF;
        if (typedStack.contains("Count", Tag.TAG_INT)) return typedStack.getInt("Count");

        return 0L;
    }

    /**
     * 从旧 TypedStack 中剔除可能的数量字段，避免当前版本 key 反序列化被旧字段污染。
     */
    private static void stripLegacyAmountFields(CompoundTag typedStack)
    {
        typedStack.remove("amount");
        typedStack.remove("Amount");
        typedStack.remove("count");
        typedStack.remove("Count");
    }
}
