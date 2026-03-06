package com.wintercogs.beyonddimensions.api.storage.key;

import com.wintercogs.beyonddimensions.api.storage.key.impl.ItemStackKey;
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
        // 新
        if (nbt.contains("key", Tag.TAG_COMPOUND))
        {
            CompoundTag keyTag = nbt.getCompound("key");
            IStackKey<?> key = IStackKey.deserializeNBTCommon(keyTag);

            long amount = readAmountCompat(nbt, keyTag);
            return new KeyAmount(key, amount);
        }

        // 旧
        if (nbt.contains("Type", Tag.TAG_STRING))
        {
            String typeStr = nbt.getString("Type");

            if ("Empty".equals(typeStr))
            {
                return new KeyAmount(ItemStackKey.EMPTY, 0L);
            }

            CompoundTag compatKey = nbt.copy();
            compatKey.putString("type", typeStr);

            aliasLegacyStackKey(compatKey);

            IStackKey<?> key = IStackKey.deserializeNBTCommon(compatKey);

            long amount = readAmountCompat(nbt, compatKey);
            return new KeyAmount(key, amount);
        }

        // 兜底
        return new KeyAmount(ItemStackKey.EMPTY, 0L);
    }

    // ─────────────────────────────────────────────────────────────
    // Compat helpers
    // ─────────────────────────────────────────────────────────────

    /**
     * 数量兼容读取：
     * 1) 外层 amount / Amount
     * 2) 从 key 内部完整栈 internal_stack / Stack 提取 count/Count/amount/Amount
     * 3) 最后 0
     */
    private static long readAmountCompat(CompoundTag root, @NotNull CompoundTag keyTag)
    {
        // 1) 外层
        if (root.contains("amount", Tag.TAG_ANY_NUMERIC)) return root.getLong("amount");
        if (root.contains("Amount", Tag.TAG_ANY_NUMERIC)) return root.getLong("Amount");

        // 2) key 内部完整栈
        long inner = readAmountFromInternalStack(keyTag);
        if (inner != 0L) return inner;

        // 3) 有些更老的数据，internal_stack/Stack 可能在 root 顶层
        inner = readAmountFromInternalStack(root);
        return inner;
    }

    /**
     * 从 keyTag（或旧 TypedStack）里找到 internal_stack / Stack，并尽力读取数量字段。
     */
    private static long readAmountFromInternalStack(CompoundTag tag)
    {
        CompoundTag stack = null;

        if (tag.contains("internal_stack", Tag.TAG_COMPOUND))
            stack = tag.getCompound("internal_stack");
        else if (tag.contains("Stack", Tag.TAG_COMPOUND))
            stack = tag.getCompound("Stack");

        if (stack == null) return 0L;

        // 候选键：count/Count/amount/Amount（覆盖多数旧实现）
        if (stack.contains("count", Tag.TAG_ANY_NUMERIC)) return stack.getLong("count");
        if (stack.contains("Count", Tag.TAG_ANY_NUMERIC)) return stack.getLong("Count");
        if (stack.contains("amount", Tag.TAG_ANY_NUMERIC)) return stack.getLong("amount");
        if (stack.contains("Amount", Tag.TAG_ANY_NUMERIC)) return stack.getLong("Amount");

        return 0L;
    }

    private static void aliasLegacyStackKey(CompoundTag tag)
    {
        if (tag == null) return;

        if (tag.contains("stack")) return;

        if (tag.contains("Stack"))
        {
            Tag legacy = tag.get("Stack");
            CompoundTag wrapper = new CompoundTag();
            if (legacy != null)
            {
                wrapper.put("Stack", legacy);
            }
            tag.put("stack", wrapper);
            return;
        }

        tag.put("stack", new CompoundTag());
    }

}