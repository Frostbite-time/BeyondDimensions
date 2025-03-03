package com.wintercogs.beyonddimensions.DataBase.Storage;

import com.wintercogs.beyonddimensions.DataBase.Stack.IStackType;

public interface IStorageSystem {
    // 核心方法
    <T> T insertStack(T stack, IStackType<T> type);
    <T> T extractStack(T stack, IStackType<T> type);

    // 辅助方法
    int getSlots();          // 当前已用槽位数量
    int getMaxSlots();            // 最大槽位数量（可返回Integer.MAX_VALUE表示无限）
    boolean hasEmptySlots();      // 是否存在空槽位
}

