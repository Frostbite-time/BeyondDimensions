package com.wintercogs.beyonddimensions.api.storage.key;

import com.mojang.logging.LogUtils;
import com.wintercogs.beyonddimensions.api.storage.key.impl.ItemStackKey;
import com.wintercogs.beyonddimensions.common.init.BDItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

/**
 * 错误数据：存档里解析不了的资源键原样封装进 {@code beyonddimensions:error_data} 物品，留在存储里不会被丢弃。
 * 读档时会尝试重新解析封装的内容，成功就还原成原来的资源
 */
public final class ErrorData
{
    private static final Logger LOGGER = LogUtils.getLogger();
    // 错误数据物品 NBT 中保存封装内容的键
    private static final String TAG = "error_data";
    private static final String RAW = "raw";
    private static final String SOURCE = "source";
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
        stack.getOrCreateTag().put(TAG, holder);
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
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG, Tag.TAG_COMPOUND))
            return null;
        return tag.getCompound(TAG).get(RAW);
    }

    /**
     * 原始数据的资源类型，用于提示与日志；认不出时返回 null
     */
    public static @Nullable String type(Tag raw)
    {
        return raw instanceof CompoundTag tag && tag.contains("type", Tag.TAG_STRING) ? tag.getString("type") : null;
    }

    /**
     * 保存时封装的错误数据记下的资源 id，用于提示与日志；读档时解析不了的原样数据没有这一项，返回 null
     */
    public static @Nullable String source(Tag raw)
    {
        return raw instanceof CompoundTag tag && tag.contains(SOURCE, Tag.TAG_STRING) ? tag.getString(SOURCE) : null;
    }

    /**
     * 保存时编码一个资源键，捕获一切错误，一个资源编码不了不影响其余资源的保存。
     * 编码不了时写回它读档时的原样数据 {@code loaded}；本次新放入、没有原样数据的资源记下类型、资源 id 与描述，封装成错误数据；
     * 连错误数据都封装不了时打一条日志，返回 null，由调用方跳过
     */
    public static @Nullable Saved encodeForSave(IStackKey<?> key, @Nullable CompoundTag loaded)
    {
        String reason;
        try
        {
            return new Saved(IStackKey.serializeNBTCommon(key), true);
        }
        catch (Throwable e)
        {
            reason = e.toString();
        }
        if (loaded != null)
        {
            LOGGER.error("Could not save stored resource {}, wrote back the data it was loaded from: {}", key, reason);
            return new Saved(loaded, false);
        }
        try
        {
            CompoundTag described = new CompoundTag();
            described.putString("type", key.getTypeId().toString());
            // 资源 id 显示在错误数据的提示里，让玩家知道丢了什么
            described.putString(SOURCE, key.getSourceId().toString());
            described.putString("unsaved", key.toString());
            CompoundTag tag = IStackKey.serializeNBTCommon(wrap(described));
            LOGGER.error("Could not save stored resource {}, kept it as error data: {}", key, reason);
            return new Saved(tag, false);
        }
        catch (Throwable e)
        {
            LOGGER.error("Could not save stored resource {} even as error data, skipped it: {}", key, reason, e);
            return null;
        }
    }

    /**
     * 保存时编码的结果；encoded 为 false 时 tag 是读档时的原样数据或错误数据
     */
    public record Saved(CompoundTag tag, boolean encoded)
    {
    }

    /**
     * 读档时不会失败的解析：用严格的解析读 input，失败时封装成错误数据，解析到错误数据时尝试还原
     */
    static IStackKey<?> lenient(Function<CompoundTag, IStackKey<?>> strict, CompoundTag input)
    {
        String reason;
        try
        {
            IStackKey<?> key = strict.apply(input);
            Tag raw = raw(key);
            return raw == null ? key : reread(strict, raw, key);
        }
        catch (Throwable e)
        {
            reason = e.toString();
        }
        Report report = REPORT.get();
        if (report != null)
            report.failed(input, reason);
        else
            LOGGER.warn("Wrapped an unparsable stored resource as error data: {}, reason: {}", describe(input), reason);
        return wrap(input);
    }

    private static IStackKey<?> reread(Function<CompoundTag, IStackKey<?>> strict, Tag raw, IStackKey<?> wrapped)
    {
        Report report = REPORT.get();
        try
        {
            if (raw instanceof CompoundTag tag)
            {
                IStackKey<?> key = strict.apply(tag);
                if (raw(key) == null)
                {
                    if (report != null)
                        report.restored++;
                    else
                        LOGGER.info("Restored error data: {}", describe(raw));
                    return key;
                }
            }
        }
        catch (Throwable ignored)
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
        String source = source(raw);
        return (type == null ? "unknown type" : type) + (source == null ? "" : " " + source);
    }

    /**
     * 在当前线程上汇总一次读档中的封装与还原，关闭时打一条日志；嵌套打开时只有最外层汇总
     *
     * @param what 读档的对象，会写进日志，例如 "dimensional network #3"
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
                LOGGER.warn("{}: {} resources could not be parsed and were wrapped as error data as is (first reason: {})", what, failed, firstReason);
            if (stillWrapped > 0)
                LOGGER.warn("{}: {} error data entries still cannot be parsed and stay wrapped", what, stillWrapped);
            if (failed > 0 || stillWrapped > 0)
                LOGGER.warn("{}: error data content {}", what, byContent);
            if (restored > 0)
                LOGGER.info("{}: restored {} error data entries", what, restored);
        }
    }
}
