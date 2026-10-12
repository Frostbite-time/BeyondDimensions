package com.wintercogs.beyonddimensions.api.dimensionnet.rule;

import com.mojang.logging.LogUtils;
import com.wintercogs.beyonddimensions.api.storage.key.IStackKey;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * 维度网络统一存储的插入与提取规则。
 * <p>
 * 资源进入或离开维度网络时，按表中顺序找第一条匹配且给出计划的规则。计划里加入或扣除的资源会再经过规则，
 * 但同一条转换链上每条规则最多生效一次。规则不区分来源：界面、自动化和其他模组的操作一视同仁。
 * <p>
 * 插入与提取各有一张表，id 在各自的表里唯一
 */
public final class UnifiedStorageRules
{
    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * 新规则在表中的位置
     */
    public sealed interface Placement
    {
    }

    private record First() implements Placement
    {
    }

    private record Last() implements Placement
    {
    }

    private record Before(ResourceLocation id) implements Placement
    {
    }

    private record After(ResourceLocation id) implements Placement
    {
    }

    public static Placement first()
    {
        return new First();
    }

    public static Placement last()
    {
        return new Last();
    }

    public static Placement before(ResourceLocation id)
    {
        return new Before(Objects.requireNonNull(id));
    }

    public static Placement after(ResourceLocation id)
    {
        return new After(Objects.requireNonNull(id));
    }

    private static final class Registration<R>
    {
        final ResourceLocation id;
        final @Nullable IStackKey<?> key;
        final @Nullable Predicate<IStackKey<?>> matcher;
        final R rule;
        volatile boolean reported;

        Registration(ResourceLocation id, @Nullable IStackKey<?> key, @Nullable Predicate<IStackKey<?>> matcher, R rule)
        {
            this.id = Objects.requireNonNull(id);
            this.key = key;
            this.matcher = matcher;
            this.rule = Objects.requireNonNull(rule);
        }
    }

    /**
     * 表中的一条规则
     */
    public static final class Entry<R>
    {
        private final Registration<R> registration;
        private final int order;

        private Entry(Registration<R> registration, int order)
        {
            this.registration = registration;
            this.order = order;
        }

        public ResourceLocation id()
        {
            return registration.id;
        }

        public R rule()
        {
            return registration.rule;
        }

        private boolean matches(IStackKey<?> key)
        {
            try
            {
                return registration.matcher.test(key);
            }
            catch (RuntimeException failure)
            {
                report("matching " + key, failure);
                return false;
            }
        }

        /**
         * 规则出错时记录日志，同一条规则只记录第一次
         */
        public void report(String stage, Throwable failure)
        {
            if (registration.reported)
                return;
            registration.reported = true;
            LOGGER.error("Storage rule {} failed while {}; later failures of this rule are not logged", registration.id, stage, failure);
        }
    }

    /**
     * 某一时刻的规则表：按资源精确匹配的规则按资源分组，其余逐条判断
     */
    public static final class Table<R>
    {
        private final List<Entry<R>> conditional = new ArrayList<>();
        private final Map<IStackKey<?>, List<Entry<R>>> exact = new HashMap<>();

        private Table(List<Registration<R>> registrations)
        {
            for (int i = 0; i < registrations.size(); i++)
            {
                Registration<R> registration = registrations.get(i);
                Entry<R> entry = new Entry<>(registration, i);
                if (registration.key != null)
                    exact.computeIfAbsent(registration.key, key -> new ArrayList<>()).add(entry);
                else
                    conditional.add(entry);
            }
        }

        /**
         * 与 key 匹配、排在 previous 之后的第一条规则；previous 为 null 时从头找
         */
        public @Nullable Entry<R> next(IStackKey<?> key, @Nullable Entry<R> previous)
        {
            int after = previous == null ? -1 : previous.order;
            Entry<R> found = null;
            List<Entry<R>> byKey = exact.isEmpty() ? null : exact.get(key);
            if (byKey != null)
            {
                for (Entry<R> entry : byKey)
                {
                    if (entry.order > after)
                    {
                        found = entry;
                        break;
                    }
                }
            }
            for (int i = 0; i < conditional.size(); i++)
            {
                Entry<R> entry = conditional.get(i);
                if (entry.order <= after)
                    continue;
                if (found != null && entry.order > found.order)
                    break;
                if (entry.matches(key))
                    return entry;
            }
            return found;
        }
    }

    private static final class Rules<R>
    {
        private final String kind;
        // 存储在服务端线程读取，注册可能来自模组加载线程或脚本重载：每次改动换一张新表
        private volatile List<Registration<R>> registrations = List.of();
        private volatile Table<R> table = new Table<>(List.of());

        Rules(String kind)
        {
            this.kind = kind;
        }

        synchronized void register(Registration<R> registration, Placement placement)
        {
            Objects.requireNonNull(placement);
            List<Registration<R>> next = new ArrayList<>(registrations);
            if (indexOf(next, registration.id) >= 0)
                throw new IllegalStateException("Storage " + kind + " rule " + registration.id + " is already registered");
            int index = switch (placement)
            {
                case First first -> 0;
                case Last last -> next.size();
                case Before before -> require(next, registration.id, before.id(), "before");
                case After after -> require(next, registration.id, after.id(), "after") + 1;
            };
            next.add(index, registration);
            update(next);
        }

        synchronized boolean remove(ResourceLocation id)
        {
            List<Registration<R>> next = new ArrayList<>(registrations);
            int index = indexOf(next, id);
            if (index < 0)
                return false;
            next.remove(index);
            update(next);
            return true;
        }

        private void update(List<Registration<R>> next)
        {
            registrations = List.copyOf(next);
            table = new Table<>(registrations);
        }

        private int require(List<Registration<R>> list, ResourceLocation id, ResourceLocation anchor, String relation)
        {
            int index = indexOf(list, anchor);
            if (index < 0)
                throw new IllegalStateException("Cannot register storage " + kind + " rule " + id + " " + relation + " " + anchor
                        + ": no such rule is registered. Registered, in order: " + ids());
            return index;
        }

        private static int indexOf(List<? extends Registration<?>> list, ResourceLocation id)
        {
            for (int i = 0; i < list.size(); i++)
                if (list.get(i).id.equals(id)) return i;
            return -1;
        }

        List<ResourceLocation> ids()
        {
            return registrations.stream().map(registration -> registration.id).toList();
        }
    }

    private static final Rules<InsertRule> INSERT = new Rules<>("insert");
    private static final Rules<ExtractRule> EXTRACT = new Rules<>("extract");

    private UnifiedStorageRules()
    {
    }

    /**
     * 注册只处理 key 这一种资源的插入规则，放在表尾
     */
    public static void insert(ResourceLocation id, IStackKey<?> key, InsertRule rule)
    {
        insert(id, key, rule, last());
    }

    /**
     * 注册只处理 key 这一种资源的插入规则。
     *
     * @throws IllegalStateException id 已被注册，或 {@code placement} 引用的规则不存在
     */
    public static void insert(ResourceLocation id, IStackKey<?> key, InsertRule rule, Placement placement)
    {
        INSERT.register(new Registration<>(id, Objects.requireNonNull(key), null, rule), placement);
    }

    /**
     * 注册按条件匹配资源的插入规则，放在表尾
     */
    public static void insertMatching(ResourceLocation id, Predicate<IStackKey<?>> matcher, InsertRule rule)
    {
        insertMatching(id, matcher, rule, last());
    }

    /**
     * 注册按条件匹配资源的插入规则。条件每次插入都会判断，应当足够便宜
     *
     * @throws IllegalStateException id 已被注册，或 {@code placement} 引用的规则不存在
     */
    public static void insertMatching(ResourceLocation id, Predicate<IStackKey<?>> matcher, InsertRule rule, Placement placement)
    {
        INSERT.register(new Registration<>(id, null, Objects.requireNonNull(matcher), rule), placement);
    }

    /**
     * 移除插入规则，返回是否存在
     */
    public static boolean removeInsert(ResourceLocation id)
    {
        return INSERT.remove(id);
    }

    /**
     * 注册只处理 key 这一种资源的提取规则，放在表尾
     */
    public static void extract(ResourceLocation id, IStackKey<?> key, ExtractRule rule)
    {
        extract(id, key, rule, last());
    }

    /**
     * 注册只处理 key 这一种资源的提取规则。
     *
     * @throws IllegalStateException id 已被注册，或 {@code placement} 引用的规则不存在
     */
    public static void extract(ResourceLocation id, IStackKey<?> key, ExtractRule rule, Placement placement)
    {
        EXTRACT.register(new Registration<>(id, Objects.requireNonNull(key), null, rule), placement);
    }

    /**
     * 注册按条件匹配资源的提取规则，放在表尾
     */
    public static void extractMatching(ResourceLocation id, Predicate<IStackKey<?>> matcher, ExtractRule rule)
    {
        extractMatching(id, matcher, rule, last());
    }

    /**
     * 注册按条件匹配资源的提取规则。条件每次提取都会判断，应当足够便宜
     *
     * @throws IllegalStateException id 已被注册，或 {@code placement} 引用的规则不存在
     */
    public static void extractMatching(ResourceLocation id, Predicate<IStackKey<?>> matcher, ExtractRule rule, Placement placement)
    {
        EXTRACT.register(new Registration<>(id, null, Objects.requireNonNull(matcher), rule), placement);
    }

    /**
     * 移除提取规则，返回是否存在
     */
    public static boolean removeExtract(ResourceLocation id)
    {
        return EXTRACT.remove(id);
    }

    /**
     * 当前插入规则的 id，按查找顺序
     */
    public static List<ResourceLocation> insertIds()
    {
        return INSERT.ids();
    }

    /**
     * 当前提取规则的 id，按查找顺序
     */
    public static List<ResourceLocation> extractIds()
    {
        return EXTRACT.ids();
    }

    /**
     * 当前的插入规则表
     */
    public static Table<InsertRule> insertTable()
    {
        return INSERT.table;
    }

    /**
     * 当前的提取规则表
     */
    public static Table<ExtractRule> extractTable()
    {
        return EXTRACT.table;
    }
}
