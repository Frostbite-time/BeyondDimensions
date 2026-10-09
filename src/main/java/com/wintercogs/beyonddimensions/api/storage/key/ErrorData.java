package com.wintercogs.beyonddimensions.api.storage.key;

import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.wintercogs.beyonddimensions.api.storage.key.impl.ItemStackKey;
import com.wintercogs.beyonddimensions.common.init.BDDataComponents;
import com.wintercogs.beyonddimensions.common.init.BDItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * 错误数据：存档里解析不了的资源键（类型未注册、物品或组件来自已移除的模组、数据损坏）原样封装进
 * {@code beyonddimensions:error_data} 物品，以普通物品的身份留在存储里，不会被丢弃，玩家也能取出或清理。
 * <p>
 * 只在读档时特判：解析到错误数据会先尝试重新解析封装的内容，成功就还原成原来的资源，所以对应模组装回后，
 * 留在维度网络或设备里的错误数据会在下次载入时自动还原。
 */
public final class ErrorData
{
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String RAW = "raw";
    private static final ThreadLocal<Report> REPORT = new ThreadLocal<>();

    private ErrorData()
    {
    }

    /**
     * 把一份原始数据封装成错误数据的资源键
     */
    public static ItemStackKey wrap(Tag raw)
    {
        CompoundTag holder = new CompoundTag();
        holder.put(RAW, raw.copy());
        ItemStack stack = new ItemStack(BDItems.ERROR_DATA.get());
        stack.set(BDDataComponents.ERROR_DATA.get(), holder);
        return new ItemStackKey(stack);
    }

    /**
     * 错误数据封装的原始数据；其他资源返回 null
     */
    public static @Nullable Tag raw(IStackKey<?> key)
    {
        if (!(key instanceof ItemStackKey item) || item.getSource() != BDItems.ERROR_DATA.get())
            return null;
        return raw(item.getReadOnlyStack());
    }

    /**
     * 错误数据物品封装的原始数据；其他物品返回 null
     */
    public static @Nullable Tag raw(ItemStack stack)
    {
        CompoundTag holder = stack.get(BDDataComponents.ERROR_DATA.get());
        return holder == null ? null : holder.get(RAW);
    }

    /**
     * 原始数据的资源类型与内容 id，用于提示与日志；认不出时返回 null
     */
    public static @Nullable String type(Tag raw)
    {
        return raw instanceof CompoundTag tag && tag.contains("type", Tag.TAG_STRING) ? tag.getString("type") : null;
    }

    public static @Nullable String content(Tag raw)
    {
        if (!(raw instanceof CompoundTag tag))
            return null;
        for (String field : new String[]{"item", "fluid", "chemical", "id", "Item", "Fluid"})
            if (tag.contains(field, Tag.TAG_STRING))
                return tag.getString(field);
        return null;
    }

    /**
     * 把严格的资源键编解码器包装成读档时不会失败的版本：解析失败或抛出异常时封装成错误数据，
     * 解析到错误数据时尝试还原。编码不变，错误数据按普通物品写出，封装的原始数据随之原样保存
     */
    static Codec<IStackKey<?>> lenient(Codec<IStackKey<?>> strict)
    {
        return new Codec<>()
        {
            @Override
            public <T> DataResult<Pair<IStackKey<?>, T>> decode(DynamicOps<T> ops, T input)
            {
                String reason;
                try
                {
                    DataResult<Pair<IStackKey<?>, T>> result = strict.decode(ops, input);
                    Optional<Pair<IStackKey<?>, T>> decoded = result.result();
                    if (decoded.isPresent())
                    {
                        IStackKey<?> key = decoded.get().getFirst();
                        Tag raw = raw(key);
                        return raw == null ? result : DataResult.success(Pair.of(reread(strict, ops, raw, key), decoded.get().getSecond()));
                    }
                    reason = result.error().map(DataResult.Error::message).orElse("unknown");
                }
                catch (RuntimeException e)
                {
                    reason = e.toString();
                }
                Tag raw = ops.convertTo(NbtOps.INSTANCE, input);
                Report report = REPORT.get();
                if (report != null)
                    report.failed(raw, reason);
                else
                    LOGGER.warn("无法解析的存储资源已封装为错误数据：{}，原因：{}", describe(raw), reason);
                return DataResult.success(Pair.of(wrap(raw), ops.empty()));
            }

            @Override
            public <T> DataResult<T> encode(IStackKey<?> input, DynamicOps<T> ops, T prefix)
            {
                return strict.encode(input, ops, prefix);
            }
        };
    }

    private static <T> IStackKey<?> reread(Codec<IStackKey<?>> strict, DynamicOps<T> ops, Tag raw, IStackKey<?> wrapped)
    {
        Report report = REPORT.get();
        try
        {
            Optional<IStackKey<?>> key = strict.parse(ops, NbtOps.INSTANCE.convertTo(ops, raw)).result();
            if (key.isPresent() && raw(key.get()) == null)
            {
                if (report != null)
                    report.restored++;
                else
                    LOGGER.info("错误数据已还原：{}", describe(raw));
                return key.get();
            }
        }
        catch (RuntimeException ignored)
        {
            // 仍然无法解析，保持封装
        }
        if (report != null)
            report.unresolved(raw);
        return wrapped;
    }

    private static String describe(Tag raw)
    {
        String type = type(raw);
        String content = content(raw);
        return (type == null ? "未知类型" : type) + (content == null ? "" : " " + content);
    }

    /**
     * 在当前线程上汇总一次读档中的封装与还原，关闭时打一条日志；嵌套打开时只有最外层汇总
     *
     * @param what 读档的对象，例如“维度网络 #3”
     */
    public static Report report(String what)
    {
        Report outer = REPORT.get();
        if (outer != null)
            return outer.nested();
        Report report = new Report(what);
        REPORT.set(report);
        return report;
    }

    public static final class Report implements AutoCloseable
    {
        private final String what;
        private final boolean outermost;
        private final Report owner;
        private int failed;
        private int restored;
        private int stillWrapped;
        private final Map<String, Integer> byContent = new TreeMap<>();
        private String firstReason;

        private Report(String what)
        {
            this.what = what;
            this.outermost = true;
            this.owner = this;
        }

        private Report(Report owner)
        {
            this.what = owner.what;
            this.outermost = false;
            this.owner = owner;
        }

        private Report nested()
        {
            return new Report(this);
        }

        private void failed(Tag raw, String reason)
        {
            failed++;
            byContent.merge(describe(raw), 1, Integer::sum);
            if (firstReason == null)
                firstReason = reason;
        }

        private void unresolved(Tag raw)
        {
            stillWrapped++;
            byContent.merge(describe(raw), 1, Integer::sum);
        }

        @Override
        public void close()
        {
            if (!outermost || REPORT.get() != owner)
                return;
            REPORT.remove();
            if (failed > 0)
                LOGGER.warn("{}：{} 项资源无法解析，已原样封装为错误数据（首个原因：{}）", what, failed, firstReason);
            if (stillWrapped > 0)
                LOGGER.warn("{}：{} 项错误数据仍无法解析，继续保留", what, stillWrapped);
            if (failed > 0 || stillWrapped > 0)
                LOGGER.warn("{}：错误数据内容 {}", what, byContent);
            if (restored > 0)
                LOGGER.info("{}：{} 项错误数据已还原", what, restored);
        }
    }
}
