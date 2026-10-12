package com.wintercogs.beyonddimensions.integration.module.jei;

import com.wintercogs.beyonddimensions.api.storage.key.IStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.api.storage.key.StackKeyRegistry;
import com.wintercogs.beyonddimensions.api.storage.key.impl.ItemStackKey;
import com.wintercogs.beyonddimensions.client.ui.InventoryScreenAccess;
import com.wintercogs.beyonddimensions.common.menu.widget.slot.AbstractStackTypedSlot;
import com.wintercogs.beyonddimensions.integration.ModPresence;
import com.wintercogs.beyonddimensions.integration.OtherModIds;
import com.wintercogs.beyonddimensions.integration.module.ae2.AEHelper;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.inventory.Slot;

import java.util.ArrayList;
import java.util.List;

// 为网络接口注册JEI拖拽支持
public class NetInterfaceGhostHandler<S extends Screen> implements IGhostIngredientHandler<S>
{

    @Override
    public <I> List<Target<I>> getTargetsTyped(S screen, ITypedIngredient<I> ingredient, boolean doStart)
    {
        List<Target<I>> targets = new ArrayList<>();

        var menu = InventoryScreenAccess.menu(screen);
        if (menu == null) return targets;
        for (Slot slot : menu.slots)
        {
            if (slot.isActive() && slot.isFake() && slot instanceof AbstractStackTypedSlot sSlot && InventoryScreenAccess.slot(screen, slot) != null)
            {
                targets.add(new IStackTarget<>(sSlot, screen));
            }
        }

        return targets;
    }

    @Override
    public void onComplete()
    {
    }

    private static class IStackTarget<I> implements Target<I>
    {
        private final AbstractStackTypedSlot slot;
        private final com.wintercogs.beyonddimensions.common.menu.BDBaseMenu menu;
        private final Rect2i area;


        public IStackTarget(AbstractStackTypedSlot slot, Screen screen)
        {
            this.slot = slot;
            this.menu = InventoryScreenAccess.menu(screen);
            this.area = InventoryScreenAccess.slot(screen, slot);
        }

        @Override
        public Rect2i getArea()
        {
            return area;
        }

        // 当玩家把物品拖过来时发生的事情
        @Override // I是类似 ItemStack的类
        public void accept(I ingredient)
        {
            Object stackKey = ingredient;
            IStackKey<?> dragging = ItemStackKey.EMPTY;
            for (IStackKey<?> type : StackKeyRegistry.getAllTypes())
            {
                if (type.getStackClass().isAssignableFrom(stackKey.getClass()))
                {
                    KeyAmount ka = type.fromStackObject(ingredient);
                    if (ka != null)
                    {
                        dragging = ka.key();
                    }

                    break;
                }
            }

            // AE2通用包裹支持
            if (ModPresence.isLoaded(OtherModIds.AE2))
            {
                if (dragging instanceof ItemStackKey draggingItemKey && !dragging.isEmpty())
                {
                    appeng.api.stacks.GenericStack genericContent = appeng.api.stacks.GenericStack.fromItemStack(draggingItemKey.copyStack());

                    if (genericContent != null)
                    {
                        dragging = AEHelper.fromAEKeyToIStack(genericContent.what()).orElse(ItemStackKey.EMPTY);
                    }

                }
            }

            if (menu != null) menu.commands().ghost(slot.index, new KeyAmount(dragging, 1));

        }
    }
}
