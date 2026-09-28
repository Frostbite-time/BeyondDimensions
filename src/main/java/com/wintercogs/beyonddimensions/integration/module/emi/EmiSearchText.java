package com.wintercogs.beyonddimensions.integration.module.emi;

import dev.emi.emi.api.EmiApi;

/**
 * EMI 搜索框文字；仅在 EMI 已加载时调用
 */
public final class EmiSearchText
{
    private EmiSearchText()
    {
    }

    public static String get()
    {
        return EmiApi.getSearchText();
    }

    public static void set(String text)
    {
        if (!text.equals(EmiApi.getSearchText()))
            EmiApi.setSearchText(text);
    }
}
