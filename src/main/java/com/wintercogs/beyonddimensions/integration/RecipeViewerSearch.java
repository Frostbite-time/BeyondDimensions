package com.wintercogs.beyonddimensions.integration;

import com.wintercogs.beyonddimensions.integration.module.emi.EmiSearchText;
import com.wintercogs.beyonddimensions.integration.module.jei.JeiSearchText;
import org.jetbrains.annotations.Nullable;

/**
 * 与已安装的配方查看器（JEI、EMI）同步搜索文字。未安装时不会加载对应的类
 */
public final class RecipeViewerSearch
{
    private RecipeViewerSearch()
    {
    }

    /**
     * 配方查看器当前的搜索文字；没有安装配方查看器时返回 null
     */
    public static @Nullable String get()
    {
        if (ModPresence.isLoaded(OtherModIds.JEI))
        {
            String text = JeiSearchText.get();
            if (text != null)
                return text;
        }
        if (ModPresence.isLoaded(OtherModIds.EMI))
            return EmiSearchText.get();
        return null;
    }

    public static void set(String text)
    {
        if (ModPresence.isLoaded(OtherModIds.JEI))
            JeiSearchText.set(text);
        if (ModPresence.isLoaded(OtherModIds.EMI))
            EmiSearchText.set(text);
    }
}
