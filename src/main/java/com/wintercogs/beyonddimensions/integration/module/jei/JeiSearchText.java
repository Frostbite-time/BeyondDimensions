package com.wintercogs.beyonddimensions.integration.module.jei;

import org.jetbrains.annotations.Nullable;

/**
 * JEI 搜索框文字；仅在 JEI 已加载时调用
 */
public final class JeiSearchText
{
    private JeiSearchText()
    {
    }

    public static @Nullable String get()
    {
        return BDjeiPlugin.runtime().map(runtime -> runtime.getIngredientFilter().getFilterText()).orElse(null);
    }

    public static void set(String text)
    {
        BDjeiPlugin.runtime().ifPresent(runtime -> {
            var filter = runtime.getIngredientFilter();
            if (!text.equals(filter.getFilterText()))
                filter.setFilterText(text);
        });
    }
}
