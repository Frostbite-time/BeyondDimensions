package com.wintercogs.beyonddimensions.api.menu.widget;

import net.minecraft.world.inventory.Slot;

import java.util.List;

/**
 * 能连续制作的工作台：从产物槽取出后会从存储补充原料。存储界面为它提供连续制作的手势：
 * 空格左键点产物一直做到背包放满或原料用完，Shift 点产物做一组，空格左键点原料槽清空原料
 */
public interface BatchCraftWidget extends ClearableWidget
{
    /**
     * 菜单里编号为 {@code index} 的槽是不是产物槽
     */
    boolean isResultSlot(int index);

    /**
     * 放原料的槽
     */
    List<Slot> inputSlots();

    /**
     * 连续制作：{@code crafts} 里每次从产物槽取出都会制作一份
     */
    default void craftBatch(Runnable crafts)
    {
        crafts.run();
    }
}
