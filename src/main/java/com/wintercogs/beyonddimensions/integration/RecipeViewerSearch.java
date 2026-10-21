package com.wintercogs.beyonddimensions.integration;

import com.wintercogs.beyonddimensions.integration.module.emi.EmiSearchText;
import com.wintercogs.beyonddimensions.integration.module.jei.JeiSearchText;
import org.jetbrains.annotations.Nullable;

/**
 * 与已安装的配方查看器（JEI、EMI）同步搜索文字。未安装时不会加载对应的类
 */
public final class RecipeViewerSearch
{
    /**
     * JEI 的搜索框最多 128 个字符。更长的文字会让 JEI 与原版输入框来回互设，直到栈溢出
     */
    private static final int MAX_LENGTH = 128;

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

    /**
     * 设置配方查看器的搜索文字，超出 {@link #MAX_LENGTH} 的部分截去；开启同步时，存储界面的搜索读回后也停在这个长度
     */
    public static void set(String text)
    {
        String fitted = text.substring(0, Math.min(text.length(), MAX_LENGTH));
        if (ModPresence.isLoaded(OtherModIds.JEI))
            JeiSearchText.set(fitted);
        if (ModPresence.isLoaded(OtherModIds.EMI))
            EmiSearchText.set(fitted);
    }
}
