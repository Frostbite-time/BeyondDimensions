package com.wintercogs.beyonddimensions.common.menu.widget;

import com.wintercogs.beyonddimensions.api.storage.key.IStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.api.storage.key.impl.FluidStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.impl.ItemStackKey;
import com.wintercogs.beyonddimensions.integration.ModPresence;
import com.wintercogs.beyonddimensions.integration.OtherModIds;
import com.wintercogs.beyonddimensions.integration.module.jech.PinInMatches;
import com.wintercogs.beyonddimensions.util.TinyPinyinUtils;
import com.wintercogs.beyonddimensions.util.TooltipHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 专用于ClientNetStorage，内部集成搜索用的方法和字段。
 * <p>
 * 搜索框与分类标签共用同一套写法：{@link Query} 是解析好的一条搜索式，名称、提示框、标签等派生数据按资源缓存，所有搜索式共用
 */
public class ClientNetStorageSearchHelper
{
    /**
     * 解析好的一条搜索式。空格分隔的各项按与合并，项内 | 分隔的部分按或合并
     */
    public static final class Query
    {
        public static final Query EMPTY = new Query("", List.of());

        private final String text;
        private final List<String> terms;
        // 这条搜索式对各资源的匹配结果，派生缓存清空后随之作废
        private final Map<IStackKey<?>, Boolean> matchCache = new HashMap<>();
        private long generation = -1;

        private Query(String text, List<String> terms)
        {
            this.text = text;
            this.terms = terms;
        }

        public String text()
        {
            return text;
        }

        /**
         * 没有任何条件：搜索框为空时显示全部
         */
        public boolean isEmpty()
        {
            return terms.isEmpty();
        }

        public static Query parse(@NotNull String text)
        {
            Objects.requireNonNull(text, "searchText cannot be null");
            if (text.isBlank())
                return text.isEmpty() ? EMPTY : new Query(text, List.of());
            List<String> terms = new ArrayList<>();
            StringBuilder current = new StringBuilder();
            boolean inQuotes = false;
            boolean escaping = false;

            for (int i = 0; i < text.length(); i++)
            {
                char c = text.charAt(i);

                // 若前一个字符是反斜杠，则当前字符直接按字面加入
                if (escaping)
                {
                    current.append(c);
                    escaping = false;
                    continue;
                }

                // 反斜杠用于转义下一个字符
                if (c == '\\')
                {
                    escaping = true;
                    continue;
                }

                // 未被转义的双引号：切换引号状态，引号本身不加入内容
                if (c == '"')
                {
                    inQuotes = !inQuotes;
                    continue;
                }

                // 仅在引号外，空白字符才作为分隔符
                if (Character.isWhitespace(c) && !inQuotes)
                {
                    if (!current.isEmpty())
                    {
                        terms.add(current.toString().toLowerCase(Locale.ENGLISH));
                        current.setLength(0);
                    }
                    continue;
                }

                current.append(c);
            }

            // 若最后一个字符是孤立的反斜杠，则将其本身保留
            if (escaping)
                current.append('\\');
            if (!current.isEmpty())
                terms.add(current.toString().toLowerCase(Locale.ENGLISH));
            return new Query(text, List.copyOf(terms));
        }
    }

    private @NotNull Query query = Query.EMPTY;

    // 派生缓存每清空一次加一，各搜索式据此丢弃自己的匹配结果
    private long generation = 0;

    /**
     * 以下缓存不依赖搜索条件，所有搜索式共用
     */
    private final Map<IStackKey<?>, String> nameCache = new HashMap<>();
    private final Map<IStackKey<?>, String> modidCache = new HashMap<>();
    // TODO 当前的tooltip会包含存储数量，但是其对于我们进行提示搜索几乎无影响。因此我们仍然使用不带数量的key做缓存键
    // TODO 等到有空的时候，我应该给IStackKeyRender加个接口，允许其产出无数量tooltip
    private final Map<IStackKey<?>, List<String>> tooltipCache = new HashMap<>();
    private final Map<IStackKey<?>, List<String>> tagCache = new HashMap<>();
    private final Map<IStackKey<?>, String> itemIdCache = new HashMap<>();
    private final Map<IStackKey<?>, Integer> durabilityCache = new HashMap<>();
    private final Map<IStackKey<?>, List<String>> componentCache = new HashMap<>();

    /**
     * 设置搜索框的搜索式
     */
    public void loadTexts(@NotNull String text)
    {
        Objects.requireNonNull(text, "searchText cannot be null");
        if (!query.text().equals(text))
            query = Query.parse(text);
    }

    /**
     * 是否符合搜索框的搜索式
     */
    public boolean matches(@NotNull IStackKey<?> key)
    {
        return matches(query, key);
    }

    /**
     * 是否符合给定的搜索式；没有条件的搜索式符合一切
     */
    public boolean matches(@NotNull Query query, @NotNull IStackKey<?> key)
    {
        Objects.requireNonNull(key, "key cannot be null");
        if (query.isEmpty())
            return true;
        if (query.generation != generation)
        {
            query.matchCache.clear();
            query.generation = generation;
        }
        Boolean cached = query.matchCache.get(key);
        if (cached != null)
            return cached;

        boolean result = true;
        // 多个 searchText 按与合并
        for (String searchText : query.terms)
        {
            if (!matchesSingleSearchText(new KeyAmount(key, 1), searchText))
            {
                result = false;
                break;
            }
        }
        query.matchCache.put(key, result);
        return result;
    }

    /**
     * 单个 searchText：
     * 1. 先按 | 拆分，多个部分按或合并
     * 2. 每个部分可带 - 前缀表示取反
     * 3. 再根据 @ / $ / # / * / % / & / 默认 选择匹配范围
     */
    private boolean matchesSingleSearchText(@NotNull KeyAmount keyAmount, @NotNull String searchText)
    {
        String[] orParts = searchText.split("\\|", -1);

        for (String part : orParts)
        {
            if (part.isEmpty())
            {
                continue;
            }

            if (matchesSingleOrPart(keyAmount, part))
            {
                return true;
            }
        }

        return false;
    }

    /**
     * 单个 or 分支的匹配。
     * 先处理 -，再处理 @ / $ / # / * / % / & / 默认。
     */
    private boolean matchesSingleOrPart(@NotNull KeyAmount keyAmount, @NotNull String part)
    {
        boolean negated = false;
        String actual = part;

        if (!actual.isEmpty() && actual.charAt(0) == '-')
        {
            negated = true;
            actual = actual.substring(1);
        }

        // 例如只有一个 "-"，这里按空搜索处理：不产生实际过滤效果
        if (actual.isEmpty())
        {
            return true;
        }

        boolean matched;

        char prefix = actual.charAt(0);
        String needle = actual.substring(1);
        switch (prefix)
        {
            case '@' -> matched = needle.isEmpty() || matchesModId(keyAmount, needle);
            case '$' -> matched = needle.isEmpty() || matchesTooltip(keyAmount, needle);
            case '#' -> matched = needle.isEmpty() || matchesTag(keyAmount, needle);
            case '*' -> matched = needle.isEmpty() || matchesItemId(keyAmount, needle);
            case '%' -> matched = matchesDurability(keyAmount.key(), needle);
            case '&' -> matched = matchesComponents(keyAmount.key(), needle);
            default -> matched = matchesName(keyAmount, actual);
        }

        return negated ? !matched : matched;
    }

    private boolean matchesModId(@NotNull KeyAmount keyAmount, @NotNull String needle)
    {
        return checkTextMatches(getModId(keyAmount.key()), needle);
    }

    private boolean matchesTooltip(@NotNull KeyAmount keyAmount, @NotNull String needle)
    {
        for (String line : getTooltips(keyAmount))
        {
            if (checkTextMatches(line, needle))
            {
                return true;
            }
        }
        return false;
    }

    private boolean matchesTag(@NotNull KeyAmount keyAmount, @NotNull String needle)
    {
        for (String tag : getTags(keyAmount.key()))
        {
            if (checkTextMatches(tag, needle))
            {
                return true;
            }
        }
        return false;
    }

    private boolean matchesItemId(@NotNull KeyAmount keyAmount, @NotNull String needle)
    {
        return checkTextMatches(getItemId(keyAmount.key()), needle);
    }

    private boolean matchesName(@NotNull KeyAmount keyAmount, @NotNull String needle)
    {
        return checkTextMatches(getName(keyAmount.key()), needle);
    }

    /**
     * 耐久度：单独的 % 表示有耐久度的物品；后面可跟比较与剩余耐久的百分比，如 %<50、%>=90、%100（全新）
     */
    private boolean matchesDurability(@NotNull IStackKey<?> key, @NotNull String spec)
    {
        int percent = getDurability(key);
        if (percent < 0)
            return false;
        if (spec.isEmpty())
            return true;
        int digits = 0;
        while (digits < spec.length() && "<>=".indexOf(spec.charAt(digits)) >= 0)
            digits++;
        String operator = spec.substring(0, digits);
        int value;
        try
        {
            value = Integer.parseInt(spec.substring(digits));
        }
        catch (NumberFormatException e)
        {
            return false;
        }
        return switch (operator)
        {
            case "<" -> percent < value;
            case "<=" -> percent <= value;
            case ">" -> percent > value;
            case ">=" -> percent >= value;
            case "", "=" -> percent == value;
            default -> false;
        };
    }

    /**
     * 额外组件：单独的 & 表示带有原版默认之外的组件；后面的文字匹配这些组件的 ID，如 &enchant
     */
    private boolean matchesComponents(@NotNull IStackKey<?> key, @NotNull String needle)
    {
        List<String> components = getComponents(key);
        if (components.isEmpty())
            return false;
        if (needle.isEmpty())
            return true;
        for (String component : components)
        {
            if (component.contains(needle))
                return true;
        }
        return false;
    }

    /**
     * 检查文本是否匹配名称
     */
    private boolean checkTextMatches(String srcText, String inputText)
    {
        // 在这个类内部我们已经确保所有调用链都传入小写文本了，无需再处理
        // 但以注释形式保留这部分，以免后续忘记
        // srcText = srcText.toLowerCase(Locale.ENGLISH);
        // inputText = inputText.toLowerCase(Locale.ENGLISH);

        boolean matchText = srcText.contains(inputText);

        boolean matchPinyin;

        if (!Minecraft.getInstance().options.languageCode.startsWith("zh"))
        {
            matchPinyin = false; // 非中文地区默认不匹配
        }
        else if (ModPresence.isLoaded(OtherModIds.JE_CHARACTERS))
        {
            matchPinyin = PinInMatches.contains(srcText, inputText);
        }
        else
        {
            String allPinyin = TinyPinyinUtils.getAllPinyin(srcText, false).toLowerCase(Locale.ENGLISH);
            String firstPinyin = TinyPinyinUtils.getFirstPinYin(srcText).toLowerCase(Locale.ENGLISH);
            matchPinyin = allPinyin.contains(inputText) || firstPinyin.contains(inputText);
        }

        return matchText || matchPinyin;
    }

    private @NotNull String getName(@NotNull IStackKey<?> key)
    {
        return this.nameCache.computeIfAbsent(key,
                k -> k.getRender().getDisplayName(k).getString().toLowerCase(Locale.ENGLISH));
    }

    private @NotNull String getModId(@NotNull IStackKey<?> key)
    {
        return this.modidCache.computeIfAbsent(key,
                k -> k.getModId().toLowerCase(Locale.ENGLISH));
    }

    private @NotNull String getItemId(@NotNull IStackKey<?> key)
    {
        return this.itemIdCache.computeIfAbsent(key,
                k -> {
                    if (key instanceof ItemStackKey itemKey)
                    {
                        ResourceLocation registryName = BuiltInRegistries.ITEM.getKey(itemKey.getSource());
                        return registryName.getPath().toLowerCase(Locale.ENGLISH);
                    }
                    return "";
                });
    }

    private @NotNull List<String> getTags(@NotNull IStackKey<?> key)
    {
        return this.tagCache.computeIfAbsent(key,
                k -> k.getTags()
                        .map(tagKey -> tagKey.location().toString().toLowerCase(Locale.ENGLISH))
                        .toList());
    }

    // 剩余耐久的百分比（向下取整），没有耐久度时为 -1
    private int getDurability(@NotNull IStackKey<?> key)
    {
        return this.durabilityCache.computeIfAbsent(key, k -> {
            if (!(k instanceof ItemStackKey item))
                return -1;
            ItemStack stack = item.getReadOnlyStack();
            if (!stack.isDamageableItem())
                return -1;
            return (stack.getMaxDamage() - stack.getDamageValue()) * 100 / stack.getMaxDamage();
        });
    }

    // 原版默认之外的组件的 ID；没有额外组件时为空
    private @NotNull List<String> getComponents(@NotNull IStackKey<?> key)
    {
        return this.componentCache.computeIfAbsent(key, k -> {
            DataComponentPatch patch;
            if (k instanceof ItemStackKey item)
                patch = item.getReadOnlyStack().getComponentsPatch();
            else if (k instanceof FluidStackKey fluid)
                patch = fluid.getReadOnlyStack().getComponentsPatch();
            else
                return List.of();
            return patch.entrySet().stream()
                    .map(entry -> String.valueOf(BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(entry.getKey())).toLowerCase(Locale.ENGLISH))
                    .toList();
        });
    }

    private @NotNull List<String> getTooltips(@NotNull KeyAmount keyAmount)
    {
        return this.tooltipCache.computeIfAbsent(keyAmount.key(), k -> {
            Minecraft mc = Minecraft.getInstance();
            Player player = mc.player;
            Objects.requireNonNull(player, "cannot run text matches when player is null");

            List<Component> tooltips = TooltipHelper.getTooltipLines(
                    keyAmount,
                    Item.TooltipContext.of(player.level()),
                    mc.options.advancedItemTooltips ? TooltipFlag.Default.ADVANCED : TooltipFlag.Default.NORMAL
            );

            return tooltips.stream()
                    .map(component -> component.getString().toLowerCase(Locale.ENGLISH))
                    .collect(Collectors.toList());
        });
    }

    /**
     * 当某些 key 的显示名 / tooltip / tag 可能会在运行时变化，
     * 可以在合适时机调用它清空缓存
     */
    public void clearDerivedCaches()
    {
        this.generation++;
        this.nameCache.clear();
        this.modidCache.clear();
        this.itemIdCache.clear();
        this.tooltipCache.clear();
        this.tagCache.clear();
        this.durabilityCache.clear();
        this.componentCache.clear();
    }

    // 某个资源被移除后，丢弃它的派生缓存
    public void forget(IStackKey<?> key)
    {
        this.query.matchCache.remove(key);
        this.nameCache.remove(key);
        this.modidCache.remove(key);
        this.itemIdCache.remove(key);
        this.tooltipCache.remove(key);
        this.tagCache.remove(key);
        this.durabilityCache.remove(key);
        this.componentCache.remove(key);
    }
}
