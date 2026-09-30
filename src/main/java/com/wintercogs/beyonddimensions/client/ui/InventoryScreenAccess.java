package com.wintercogs.beyonddimensions.client.ui;

import com.wintercogs.beyonddimensions.client.ui.base.BdInventoryScreen;
import com.wintercogs.beyonddimensions.common.menu.BDBaseMenu;
import dev.compixel.forge.ComposeInventoryScreen;
import dev.compixel.forge.slots.MenuSlotBounds;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.inventory.Slot;

import java.util.List;

/**
 * 供 JEI、EMI 等可选联动读取的界面几何信息：Compose 槽位裁剪后的实际区域，以及面板外的按钮区域。
 */
public final class InventoryScreenAccess
{
    private InventoryScreenAccess()
    {
    }

    /**
     * 界面对应的 BD 菜单；界面暂时不接受槽位操作时返回 null
     */
    public static BDBaseMenu menu(Screen screen)
    {
        if (screen instanceof ComposeInventoryScreen<?> compose && !compose.getInventory().getInteractionsEnabled())
            return null;
        return screen instanceof MenuAccess<?> access && access.getMenu() instanceof BDBaseMenu menu ? menu : null;
    }

    /**
     * 槽位当前可见的区域；槽位未显示时返回 null
     */
    public static Rect2i slot(Screen screen, Slot slot)
    {
        if (screen instanceof ComposeInventoryScreen<?> compose)
            return rectangle(compose.getInventory().bounds(slot.index));
        return null;
    }

    /**
     * 面板外的按钮（模式按钮、侧边栏）所占的区域，用于配方查看器避让
     */
    public static List<Rect2i> extraAreas(Screen screen)
    {
        return screen instanceof BdInventoryScreen<?> bd ? bd.extraAreas() : List.of();
    }

    private static Rect2i rectangle(MenuSlotBounds bounds)
    {
        if (bounds == null)
            return null;
        int left = (int) Math.floor(bounds.getLeft());
        int top = (int) Math.floor(bounds.getTop());
        int right = (int) Math.ceil(bounds.getRight());
        int bottom = (int) Math.ceil(bounds.getBottom());
        return new Rect2i(left, top, right - left, bottom - top);
    }
}
