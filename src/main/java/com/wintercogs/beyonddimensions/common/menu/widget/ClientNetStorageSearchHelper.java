package com.wintercogs.beyonddimensions.common.menu.widget;

import com.wintercogs.beyonddimensions.api.storage.key.IStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.integration.ModPresence;
import com.wintercogs.beyonddimensions.integration.OtherModIds;
import com.wintercogs.beyonddimensions.integration.module.jech.PinInMatches;
import com.wintercogs.beyonddimensions.util.TinyPinyinUtils;
import com.wintercogs.beyonddimensions.util.TooltipHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 专用于ClientNetStorage，内部集成搜索用的方法和字段
 */
public class ClientNetStorageSearchHelper
{
    private @NotNull String originalSearchText = "";
    private final @NotNull List<String> searchTexts = new ArrayList<>();

    /**
     * 当前搜索条件下的最终匹配缓存。
     * 搜索文本变化时必须清空。
     */
    private final Map<IStackKey<?>, Boolean> matchCache = new HashMap<>();

    /**
     * 以下缓存不依赖当前搜索条件，因此无需在搜索文本变化时清空。
     */
    private final Map<IStackKey<?>, String> nameCache = new HashMap<>();
    private final Map<IStackKey<?>, String> modidCache = new HashMap<>();
    // TODO 当前的tooltip会包含存储数量，但是其对于我们进行提示搜索几乎无影响。因此我们仍然使用不带数量的key做缓存键
    // TODO 等到有空的时候，我应该给IStackKeyRender加个接口，允许其产出无数量tooltip
    private final Map<IStackKey<?>, List<String>> tooltipCache = new HashMap<>();
    private final Map<IStackKey<?>, List<String>> tagCache = new HashMap<>();

    public void loadTexts(@NotNull String text)
    {
        Objects.requireNonNull(text, "searchText cannot be null");
        if (this.originalSearchText.equals(text)) return;

        this.originalSearchText = text;
        this.searchTexts.clear();
        this.matchCache.clear();

        if (text.isEmpty())
        {
            return;
        }

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
                    this.searchTexts.add(current.toString().toLowerCase(Locale.ENGLISH));
                    current.setLength(0);
                }
                continue;
            }

            // 普通字符
            current.append(c);
        }

        // 若最后一个字符是孤立的反斜杠，则将其本身保留
        if (escaping)
        {
            current.append('\\');
        }

        // 收尾
        if (!current.isEmpty())
        {
            this.searchTexts.add(current.toString().toLowerCase(Locale.ENGLISH));
        }
    }

    /**
     * 可用于对外搜索匹配的接口
     */
    public boolean matches(@NotNull IStackKey<?> key)
    {
        Objects.requireNonNull(key, "key cannot be null");
        if (originalSearchText.isEmpty()) return true;

        Boolean cached = this.matchCache.get(key);
        if (cached != null)
        {
            return cached;
        }

        boolean result = true;

        // 多个 searchText 按与合并
        for (String searchText : this.searchTexts)
        {
            if (!matchesSingleSearchText(new KeyAmount(key, 1), searchText))
            {
                result = false;
                break;
            }
        }

        this.matchCache.put(key, result);
        return result;
    }

    /**
     * 单个 searchText：
     * 1. 先按 | 拆分，多个部分按或合并
     * 2. 每个部分可带 - 前缀表示取反
     * 3. 再根据 @ / $ / # / 默认 选择匹配范围
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
     * 先处理 -，再处理 @/$/#/默认。
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
        String needle;
        switch (prefix)
        {
            case '@' ->
            {
                needle = actual.substring(1);
                matched = needle.isEmpty() || matchesModId(keyAmount, needle);
            }
            case '$' ->
            {
                needle = actual.substring(1);
                matched = needle.isEmpty() || matchesTooltip(keyAmount, needle);
            }
            case '#' ->
            {
                needle = actual.substring(1);
                matched = needle.isEmpty() || matchesTag(keyAmount, needle);
            }
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

    private boolean matchesName(@NotNull KeyAmount keyAmount, @NotNull String needle)
    {
        return checkTextMatches(getName(keyAmount.key()), needle);
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

    private @NotNull List<String> getTags(@NotNull IStackKey<?> key)
    {
        return this.tagCache.computeIfAbsent(key,
                k -> k.getTags()
                        .map(tagKey -> tagKey.location().toString().toLowerCase(Locale.ENGLISH))
                        .toList());
    }

    private @NotNull List<String> getTooltips(@NotNull KeyAmount keyAmount)
    {
        return this.tooltipCache.computeIfAbsent(keyAmount.key(), k -> {
            Minecraft mc = Minecraft.getInstance();
            Player player = mc.player;
            Objects.requireNonNull(player, "cannot run text matches when player is null");

            List<Component> tooltips = TooltipHelper.getTooltipLines(
                    keyAmount,
                    player,
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
        this.matchCache.clear();
        this.nameCache.clear();
        this.modidCache.clear();
        this.tooltipCache.clear();
        this.tagCache.clear();
    }
}