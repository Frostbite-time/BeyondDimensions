package com.wintercogs.beyonddimensions.common.machine.setting;

import com.wintercogs.beyonddimensions.api.storage.key.IStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;

public enum FilterMode
{
    IGNORE, // 忽略
    WHITE, // 白名单模式
    BLACK; // 黑名单模式

    /**
     * 这种资源能否通过过滤。模糊匹配时只看种类，否则 NBT 也要与标记相同
     *
     * @param marks 过滤槽里的标记
     */
    public boolean passes(Iterable<KeyAmount> marks, IStackKey<?> key, boolean fuzzy)
    {
        return switch (this)
        {
            case IGNORE -> true;
            case WHITE -> marked(marks, key, fuzzy);
            case BLACK -> !marked(marks, key, fuzzy);
        };
    }

    private static boolean marked(Iterable<KeyAmount> marks, IStackKey<?> key, boolean fuzzy)
    {
        for (KeyAmount mark : marks)
        {
            if (fuzzy ? mark.key().isSame(key) : mark.key().isSameTypeSameComponents(key))
                return true;
        }
        return false;
    }
}
