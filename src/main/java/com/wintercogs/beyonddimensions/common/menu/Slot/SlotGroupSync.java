package com.wintercogs.beyonddimensions.common.menu.Slot;

import com.wintercogs.beyonddimensions.api.storage.key.IStackKey;

import java.util.List;

// 同步无序槽位时使用的同步器
public interface SlotGroupSync
{
    int getGroupId(); // 用于标识

    void updateChange();

    void loadChange(List<IStackKey<?>> keys, List<Long> newCounts, List<Long> newModifiedTime, List<Long> newInsertedTime);

    void afterLoadChange();
}
