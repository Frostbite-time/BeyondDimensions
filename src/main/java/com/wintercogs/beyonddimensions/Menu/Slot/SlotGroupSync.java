package com.wintercogs.beyonddimensions.Menu.Slot;

import com.wintercogs.beyonddimensions.Api.DataBase.Stack.IStackKey;

import java.util.List;

// 同步无序槽位时使用的同步器
public interface SlotGroupSync
{
    int getGroupId(); // 用于标识

    void updateChange();

    void loadChange(List<IStackKey<?>> stacks, List<Long> changedCounts);

    void afterLoadChange();
}
