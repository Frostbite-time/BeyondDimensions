package com.wintercogs.beyonddimensions.api.menu.widget;

/**
 * 放着玩家物品的部件，例如合成格与工作台的输入槽，界面可以请服务器把里面的物品清回存储或背包。
 * 工作台的界面用 {@code StorageWorkbenchContext.clearItems} 发出请求
 */
public interface ClearableWidget
{
    /**
     * 在服务端把物品还给玩家：{@code toStorageFirst} 时先退回存储，否则先退回背包
     */
    void clearItems(boolean toStorageFirst);
}
