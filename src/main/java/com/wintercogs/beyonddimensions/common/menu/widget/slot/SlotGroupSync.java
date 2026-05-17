package com.wintercogs.beyonddimensions.common.menu.widget.slot;

import com.wintercogs.beyonddimensions.api.storage.key.IStackKey;

import java.util.List;

/**
 * 同步无序槽位时使用的同步器
 */
public interface SlotGroupSync
{
    /**
     * 标识符，客户端与服务端需一致，便于传递数据
     */
    int getGroupId();

    /**
     * 服务端发送数据
     */
    void updateChange();

    /**
     * 客户端处理数据发包
     */
    void loadChange(List<IStackKey<?>> keys, List<Long> newCounts, List<Long> newModifiedTime, List<Long> newInsertedTime);

    /**
     * 读取后的后处理
     */
    void afterLoadChange();
}
