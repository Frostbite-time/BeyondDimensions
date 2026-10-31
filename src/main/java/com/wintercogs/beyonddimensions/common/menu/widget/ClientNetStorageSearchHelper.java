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
        // 每项是 | 分隔的各个部分
        private final List<List<Part>> terms;
        // 这条搜索式对各资源的匹配结果，派生缓存清空后随之作废
        private final Map<IStackKey<?>, Boolean> matchCache = new HashMap<>();
        private long generation = -1;

        private Query(String text, List<List<Part>> terms)
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
            List<List<Part>> terms = new ArrayList<>();
            StringBuilder current = new StringBuilder();
            // current 中被反斜杠转义的位置：这些字符不再当作 |、- 或前缀
            BitSet escaped = new BitSet();
            boolean inQuotes = false;
            boolean escaping = false;

            for (int i = 0; i < text.length(); i++)
            {
                char c = text.charAt(i);

                // 若前一个字符是反斜杠，则当前字符直接按字面加入
                if (escaping)
                {
                    escaped.set(current.length());
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
                    addTerm(terms, current, escaped);
                    continue;
                }

                current.append(c);
            }

            // 若最后一个字符是孤立的反斜杠，则将其本身保留
            if (escaping)
            {
                escaped.set(current.length());
                current.append('\\');
            }
            addTerm(terms, current, escaped);
            return new Query(text, List.copyOf(terms));
        }

        // 把 current 按未转义的 | 拆成各个部分作为一项，然后清空
        private static void addTerm(List<List<Part>> terms, StringBuilder current, BitSet escaped)
        {
            if (current.isEmpty())
                return;
            List<Part> parts = new ArrayList<>();
            int start = 0;
            for (int i = 0; i <= current.length(); i++)
            {
                if (i < current.length() && (current.charAt(i) != '|' || escaped.get(i)))
                    continue;
                if (i > start)
                    parts.add(Part.of(current, escaped, start, i));
                start = i + 1;
            }
            terms.add(List.copyOf(parts));
            current.setLength(0);
            escaped.clear();
        }
    }

    /**
     * | 分隔的一个部分：可带 - 前缀表示取反，再由 @ / $ / # / * / ~ / % / & 选择匹配范围，没有前缀时匹配名称
     */
    private record Part(boolean negated, char prefix, String needle)
    {
        private static final char NAME = 0;
        private static final String PREFIXES = "@$#*~%&";

        static Part of(CharSequence text, BitSet escaped, int start, int end)
        {
            boolean negated = text.charAt(start) == '-' && !escaped.get(start);
            if (negated)
                start++;
            // 例如只有一个 "-"，这里按空搜索处理：不产生实际过滤效果
            if (start == end)
                return new Part(false, NAME, "");
            char prefix = NAME;
            if (PREFIXES.indexOf(text.charAt(start)) >= 0 && !escaped.get(start))
                prefix = text.charAt(start++);
            return new Part(negated, prefix, text.subSequence(start, end).toString().toLowerCase(Locale.ENGLISH));
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
    private final Map<IStackKey<?>, String> sourceIdCache = new HashMap<>();
    private final Map<IStackKey<?>, String> typeIdCache = new HashMap<>();
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
        // 多个项按与合并
        for (List<Part> term : query.terms)
        {
            if (!matchesTerm(new KeyAmount(key, 1), term))
            {
                result = false;
                break;
            }
        }
        query.matchCache.put(key, result);
        return result;
    }

    /**
     * 单个项：| 分隔的各个部分按或合并
     */
    private boolean matchesTerm(@NotNull KeyAmount keyAmount, @NotNull List<Part> term)
    {
        for (Part part : term)
        {
            if (matchesPart(keyAmount, part))
            {
                return true;
            }
        }

        return false;
    }

    /**
     * 单个 or 分支的匹配：按前缀选择匹配范围，再处理取反
     */
    private boolean matchesPart(@NotNull KeyAmount keyAmount, @NotNull Part part)
    {
        String needle = part.needle();
        boolean matched = switch (part.prefix())
        {
            case '@' -> needle.isEmpty() || matchesModId(keyAmount, needle);
            case '$' -> needle.isEmpty() || matchesTooltip(keyAmount, needle);
            case '#' -> needle.isEmpty() || matchesTag(keyAmount, needle);
            case '*' -> needle.isEmpty() || matchesSourceId(keyAmount, needle);
            case '~' -> needle.isEmpty() || matchesTypeId(keyAmount, needle);
            case '%' -> matchesDurability(keyAmount.key(), needle);
            case '&' -> matchesComponents(keyAmount.key(), needle);
            default -> needle.isEmpty() || matchesName(keyAmount, needle);
        };

        return part.negated() ? !matched : matched;
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

    private boolean matchesSourceId(@NotNull KeyAmount keyAmount, @NotNull String needle)
    {
        return checkTextMatches(getSourceId(keyAmount.key()), needle);
    }

    private boolean matchesTypeId(@NotNull KeyAmount keyAmount, @NotNull String needle)
    {
        return checkTextMatches(getTypeId(keyAmount.key()), needle);
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

    private @NotNull String getSourceId(@NotNull IStackKey<?> key)
    {
        return this.sourceIdCache.computeIfAbsent(key,
                k -> k.getSourceId().toString().toLowerCase(Locale.ENGLISH));
    }

    private @NotNull String getTypeId(@NotNull IStackKey<?> key)
    {
        return this.typeIdCache.computeIfAbsent(key,
                k -> k.getTypeId().toString().toLowerCase(Locale.ENGLISH));
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
            int maxDamage = stack.getMaxDamage();
            if (!stack.isDamageableItem() || maxDamage <= 0)
                return -1;
            return (stack.getMaxDamage() - stack.getDamageValue()) * 100 / maxDamage;
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
        this.sourceIdCache.clear();
        this.typeIdCache.clear();
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
        this.sourceIdCache.remove(key);
        this.typeIdCache.remove(key);
        this.tooltipCache.remove(key);
        this.tagCache.remove(key);
        this.durabilityCache.remove(key);
        this.componentCache.remove(key);
    }
}
