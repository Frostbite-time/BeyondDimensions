package com.wintercogs.beyonddimensions.common.menu.ability;

/**
 * 带合成区的菜单：一个产物槽与一组合成格，槽位交互据此处理它们上面的点击
 */
public interface CraftableMenu
{
    /**
     * 是否是产物槽
     */
    boolean isCraftResultSlot(int index);

    /**
     * 是否是合成格的槽位，不含产物槽
     */
    boolean isCraftGridSlot(int index);

    /**
     * 连续合成：其间每次从产物槽取出都会合成一份，实现可借此沿用同一份配方
     */
    void craftBatch(Runnable crafts);

    /**
     * 清空合成格，物品按玩家的偏好退回
     */
    void clearCraftGrid();
}
