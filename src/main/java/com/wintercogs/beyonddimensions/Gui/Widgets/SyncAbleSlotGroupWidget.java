package com.wintercogs.beyonddimensions.Gui.Widgets;

import com.cleanroommc.modularui.value.sync.SyncHandler;
import com.cleanroommc.modularui.widgets.SlotGroupWidget;

// 完全继承自 SlotGroupWidget 但是允许其设置同步器
// 用于对 使用相同存储的槽位组合进行统一同步
// 从而将槽位本身的同步器空出，用于点击事件的同步
public class SyncAbleSlotGroupWidget extends SlotGroupWidget
{
    public SyncAbleSlotGroupWidget()
    {
        super();
    }

    public SyncAbleSlotGroupWidget syncHandler(SyncHandler syncHandler)
    {
        this.setSyncHandler(syncHandler);
        return this;
    }
}
