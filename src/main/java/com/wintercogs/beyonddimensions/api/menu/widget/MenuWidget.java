package com.wintercogs.beyonddimensions.api.menu.widget;

import com.wintercogs.beyonddimensions.common.menu.BDBaseMenu;
import dev.compixel.forge.sync.MenuSync;
import dev.compixel.slots.SlotTransferRoutes;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.NotNull;

/**
 * 菜单中一组连续的槽位及其逻辑。构造时用 {@link #addSlot} 把槽位加进菜单，并记下自己的范围；
 * 其他代码用 {@link BDBaseMenu#widget(Class)} 找到部件，不必记槽位编号。
 * 客户端与服务端必须按同样的顺序构造同样的部件
 */
public abstract class MenuWidget
{
    protected final BDBaseMenu menu;
    private final int start;
    private int end;

    protected MenuWidget(BDBaseMenu menu)
    {
        this.menu = menu;
        this.start = menu.slots.size();
        this.end = start;
    }

    /**
     * 把槽位加到菜单里，紧接在本部件已有的槽位之后
     */
    protected final <S extends Slot> S addSlot(S slot)
    {
        if (menu.slots.size() != end)
            throw new IllegalStateException(getClass().getSimpleName() + " must add its slots one after another");
        menu.addWidgetSlot(slot);
        end = menu.slots.size();
        return slot;
    }

    /**
     * 把一个整数同步给客户端，紧接在菜单已有的数据之后；客户端与服务端按同样的顺序添加
     */
    protected final DataSlot addData(DataSlot data)
    {
        menu.addWidgetData(data);
        return data;
    }

    /**
     * 第一个槽位的编号
     */
    public final int start()
    {
        return start;
    }

    /**
     * 最后一个槽位的编号加一
     */
    public final int end()
    {
        return end;
    }

    public final boolean owns(int slotIndex)
    {
        return slotIndex >= start && slotIndex < end;
    }

    /**
     * 声明 Shift 转移的分组与路线。菜单已有的分组为 {@code player}（背包与快捷栏）与 {@code storage}
     */
    public void addQuickMoveRoutes(SlotTransferRoutes.Builder routes)
    {
    }

    /**
     * 加上本部件的界面送往服务器的动作
     */
    public <M extends BDBaseMenu> void addActions(MenuSync<M> sync)
    {
    }

    /**
     * 这个容器的内容变化由本部件处理，菜单不再按原版的方式广播
     */
    public boolean ownsContainer(@NotNull Container container)
    {
        return false;
    }

    /**
     * {@link #ownsContainer} 认领的容器内容变了
     */
    public void slotsChanged(@NotNull Container container)
    {
    }

    /**
     * 原版的双击收集能否从这一格取走物品
     */
    public boolean canTakeForPickAll(@NotNull Slot slot)
    {
        return true;
    }

    /**
     * 菜单关闭
     */
    public void removed(@NotNull Player player)
    {
    }
}
