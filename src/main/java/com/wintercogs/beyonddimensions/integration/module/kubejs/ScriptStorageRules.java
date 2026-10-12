package com.wintercogs.beyonddimensions.integration.module.kubejs;

import com.wintercogs.beyonddimensions.api.dimensionnet.rule.ExtractRule;
import com.wintercogs.beyonddimensions.api.dimensionnet.rule.InsertRule;
import com.wintercogs.beyonddimensions.api.dimensionnet.rule.UnifiedStorageRules;
import com.wintercogs.beyonddimensions.api.storage.key.IStackKey;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * 服务端脚本里的 UnifiedStorageRules：用法与 {@link UnifiedStorageRules} 相同，脚本注册的规则在服务端脚本重载前全部移除，
 * 重新执行脚本时不会重复注册。要让其他模组的规则不生效，把自己的规则放在它前面
 */
public final class ScriptStorageRules
{
    private static final Set<ResourceLocation> insertIds = new LinkedHashSet<>();
    private static final Set<ResourceLocation> extractIds = new LinkedHashSet<>();

    private ScriptStorageRules()
    {
    }

    public static UnifiedStorageRules.Placement first()
    {
        return UnifiedStorageRules.first();
    }

    public static UnifiedStorageRules.Placement last()
    {
        return UnifiedStorageRules.last();
    }

    public static UnifiedStorageRules.Placement before(ResourceLocation id)
    {
        return UnifiedStorageRules.before(id);
    }

    public static UnifiedStorageRules.Placement after(ResourceLocation id)
    {
        return UnifiedStorageRules.after(id);
    }

    public static void insert(ResourceLocation id, IStackKey<?> key, InsertRule rule)
    {
        insert(id, key, rule, last());
    }

    public static synchronized void insert(ResourceLocation id, IStackKey<?> key, InsertRule rule, UnifiedStorageRules.Placement placement)
    {
        UnifiedStorageRules.insert(id, key, rule, placement);
        insertIds.add(id);
    }

    public static void insertMatching(ResourceLocation id, Predicate<IStackKey<?>> matcher, InsertRule rule)
    {
        insertMatching(id, matcher, rule, last());
    }

    public static synchronized void insertMatching(ResourceLocation id, Predicate<IStackKey<?>> matcher, InsertRule rule, UnifiedStorageRules.Placement placement)
    {
        UnifiedStorageRules.insertMatching(id, matcher, rule, placement);
        insertIds.add(id);
    }

    public static void extract(ResourceLocation id, IStackKey<?> key, ExtractRule rule)
    {
        extract(id, key, rule, last());
    }

    public static synchronized void extract(ResourceLocation id, IStackKey<?> key, ExtractRule rule, UnifiedStorageRules.Placement placement)
    {
        UnifiedStorageRules.extract(id, key, rule, placement);
        extractIds.add(id);
    }

    public static void extractMatching(ResourceLocation id, Predicate<IStackKey<?>> matcher, ExtractRule rule)
    {
        extractMatching(id, matcher, rule, last());
    }

    public static synchronized void extractMatching(ResourceLocation id, Predicate<IStackKey<?>> matcher, ExtractRule rule, UnifiedStorageRules.Placement placement)
    {
        UnifiedStorageRules.extractMatching(id, matcher, rule, placement);
        extractIds.add(id);
    }

    /**
     * 当前全部插入规则的 id，按查找顺序
     */
    public static List<ResourceLocation> insertIds()
    {
        return UnifiedStorageRules.insertIds();
    }

    /**
     * 当前全部提取规则的 id，按查找顺序
     */
    public static List<ResourceLocation> extractIds()
    {
        return UnifiedStorageRules.extractIds();
    }

    /**
     * 移除脚本注册的全部规则
     */
    static synchronized void clear()
    {
        insertIds.forEach(UnifiedStorageRules::removeInsert);
        extractIds.forEach(UnifiedStorageRules::removeExtract);
        insertIds.clear();
        extractIds.clear();
    }
}
