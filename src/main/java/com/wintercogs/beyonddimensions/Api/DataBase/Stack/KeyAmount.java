package com.wintercogs.beyonddimensions.Api.DataBase.Stack;

import com.mojang.serialization.*;
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

    // 用于兼容旧版本的数量
    public static final MapCodec<Long> AMOUNT_COMPAT = new MapCodec<>()
    {
        // 外层键
        private static final String K_AMOUNT = "amount";
        private static final String K_AMOUNT_OLD = "Amount";
        private static final String K_KEY = "key";
        // key 内部旧形态：完整栈位置
        private static final String K_INTERNAL = "internal_stack";
        private static final String K_STACK = "Stack";
        // 完整栈内部的数量候选键
        private static final String[] INNER_NUM_KEYS = {"count", "Count", "amount", "Amount"};

        @Override
        public <T> DataResult<Long> decode(DynamicOps<T> ops, MapLike<T> input)
        {
            final T kAmount = ops.createString(K_AMOUNT);
            final T kAmountOld = ops.createString(K_AMOUNT_OLD);
            final T kKey = ops.createString(K_KEY);
            final T kInternal = ops.createString(K_INTERNAL);
            final T kStack = ops.createString(K_STACK);

            // 1) 外部 amount
            T v = input.get(kAmount);
            if (v != null) return Codec.LONG.parse(ops, v);

            // 2) 外部 Amount
            v = input.get(kAmountOld);
            if (v != null) return Codec.LONG.parse(ops, v);

            // 3) 从 key 的内部完整栈提取数量，查节点，不看具体类型
            T keyNode = input.get(kKey);
            if (keyNode != null)
            {
                var keyMapDR = ops.getMap(keyNode);
                if (keyMapDR.result().isPresent())
                {
                    MapLike<T> keyMap = keyMapDR.result().get();
                    T stackNode = keyMap.get(kInternal);
                    if (stackNode == null) stackNode = keyMap.get(kStack);
                    if (stackNode != null)
                    {
                        var stackMapDR = ops.getMap(stackNode);
                        if (stackMapDR.result().isPresent())
                        {
                            MapLike<T> stackMap = stackMapDR.result().get();
                            for (String inner : INNER_NUM_KEYS)
                            {
                                T innerKey = ops.createString(inner);
                                T numNode = stackMap.get(innerKey);
                                if (numNode != null)
                                {
                                    // 优先用 getNumberValue；不行再尝试 Codec.LONG 解析
                                    var numDR = ops.getNumberValue(numNode).map(n -> n.longValue());
                                    if (numDR.result().isPresent()) return numDR;
                                    var asLong = Codec.LONG.parse(ops, numNode);
                                    if (asLong.result().isPresent()) return asLong;
                                }
                            }
                        }
                    }
                }
            }

            // 全部失败：视为空（0）
            return DataResult.success(0L);
        }

        @Override
        public <T> RecordBuilder<T> encode(Long value, DynamicOps<T> ops, RecordBuilder<T> prefix)
        {
            // 仅写小写 amount
            return prefix.add(ops.createString(K_AMOUNT), ops.createLong(value));
        }

        @Override
        public <T> java.util.stream.Stream<T> keys(DynamicOps<T> ops)
        {
            return java.util.stream.Stream.of(ops.createString(K_AMOUNT));
        }
    };

    // ─────────────────────────────────────────────────────────────
    // 新格式（纯粹）：{ key, amount } —— 仅在 encode 或新数据 decode 时使用
    // ─────────────────────────────────────────────────────────────
    private static final MapCodec<KeyAmount> NEW_FMT = RecordCodecBuilder.mapCodec(inst ->
            inst.group(
                    IStackKey.CODEC.fieldOf("key").forGetter(KeyAmount::key),
                    AMOUNT_COMPAT.forGetter(KeyAmount::amount)
            ).apply(inst, KeyAmount::new)
    );

    // ─────────────────────────────────────────────────────────────
    // 兼容型 MapCodec：写新；读新优先；缺 key 时按“旧 IStackType 形状”解
    // 支持旧字段：
    //   - 顶层 "Type"（资源路径）→ 注入为小写 "type" 以便 IStackKey.CODEC 分发
    //   - 旧占位：Type == "Empty" → KeyAmount(ItemStackKey.EMPTY, 0)
    //   - 数量：外层 amount/Amount → 若无再从 key 内部完整栈的 count/Count/amount/Amount
    // ─────────────────────────────────────────────────────────────
    public static final MapCodec<KeyAmount> TYPE_CODEC = new MapCodec<>()
    {
        private static final String K_KEY = "key";
        private static final String K_type = "type";   // 新：IStackKey 分发字段
        private static final String K_Type = "Type";   // 旧：大写
        private static final String K_amt = "amount";
        private static final String K_Amt = "Amount";

        @Override
        public <T> DataResult<KeyAmount> decode(DynamicOps<T> ops, MapLike<T> input)
        {
            final T kKey = ops.createString(K_KEY);

            // 1) 新格式：{ key, amount }
            if (input.get(kKey) != null)
            {
                return NEW_FMT.decode(ops, input);
            }

            // 2) 旧元素（IStackType 形状）：顶层包含 Type/内部 old 字段
            final T kTypeOld = ops.createString(K_Type);
            T typeNode = input.get(kTypeOld);
            String typeStr = (typeNode == null) ? null : ops.getStringValue(typeNode).result().orElse(null);

            // 2.1 占位 "Empty"
            if ("Empty".equals(typeStr))
            {
                return DataResult.success(new KeyAmount(ItemStackKey.EMPTY, 0L));
            }

            // 2.2 构造“兼容 key 对象”：把旧 Type -> 新 type，其余字段原样保留
            java.util.Map<T, T> compatKeyMap = new java.util.LinkedHashMap<>();
            input.entries().forEach(p -> compatKeyMap.put(p.getFirst(), p.getSecond()));
            if (typeNode != null)
            {
                compatKeyMap.put(ops.createString(K_type), typeNode); // 注入小写 type 以便 IStackKey 分发
            }
            T compatKeyNode = ops.createMap(compatKeyMap);

            // 2.3 解出 key
            DataResult<IStackKey<?>> keyDR = IStackKey.CODEC.parse(ops, compatKeyNode)
                    .mapError(err -> "KeyAmount(old IStackType) -> key decode failed: " + err);

            // 2.4 让 AMOUNT_COMPAT 解出 amount（外层/内部兼容）
            java.util.Map<T, T> probe = new java.util.LinkedHashMap<>();
            probe.put(ops.createString(K_KEY), compatKeyNode);
            // 把旧条目外层可能存在的 amount/Amount 一并提供
            T vAmt = input.get(ops.createString(K_amt));
            if (vAmt != null) probe.put(ops.createString(K_amt), vAmt);
            T vAmtOld = input.get(ops.createString(K_Amt));
            if (vAmtOld != null) probe.put(ops.createString(K_Amt), vAmtOld);

            T probeNode = ops.createMap(probe);
            DataResult<Long> amtDR = AMOUNT_COMPAT.codec().decode(ops, probeNode)
                    .map(com.mojang.datafixers.util.Pair::getFirst)
                    .mapError(err -> "KeyAmount(old IStackType) -> amount decode failed: " + err);

            return keyDR.flatMap(k -> amtDR.map(a -> new KeyAmount(k, a)));
        }

        @Override
        public <T> RecordBuilder<T> encode(KeyAmount value, DynamicOps<T> ops, RecordBuilder<T> prefix)
        {
            // 写：始终写新格式（key + amount）
            return NEW_FMT.encode(value, ops, prefix);
        }

        @Override
        public <T> java.util.stream.Stream<T> keys(DynamicOps<T> ops)
        {
            return java.util.stream.Stream.of(ops.createString(K_KEY), ops.createString("amount"));
        }
    };

    // 对外暴露的 Codec：使用兼容型 MapCodec
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
