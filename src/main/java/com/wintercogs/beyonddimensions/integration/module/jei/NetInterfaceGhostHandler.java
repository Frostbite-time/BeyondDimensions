package com.wintercogs.beyonddimensions.integration.module.jei;

import com.wintercogs.beyonddimensions.api.storage.key.IStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.api.storage.key.StackKeyRegistry;
import com.wintercogs.beyonddimensions.api.storage.key.impl.ItemStackKey;
import com.wintercogs.beyonddimensions.client.gui.BDBaseGUI;
import com.wintercogs.beyonddimensions.common.menu.widget.slot.AbstractStackTypedSlot;
import com.wintercogs.beyonddimensions.integration.ModPresence;
import com.wintercogs.beyonddimensions.integration.OtherModIds;
import com.wintercogs.beyonddimensions.integration.module.ae2.AEHelper;
import com.wintercogs.beyonddimensions.network.packet.both.SetSlotDirectlyPacket;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

// 为网络接口注册JEI拖拽支持
public class NetInterfaceGhostHandler implements IGhostIngredientHandler<BDBaseGUI>
{

    @Override
    public <I> List<Target<I>> getTargetsTyped(BDBaseGUI screen, ITypedIngredient<I> ingredient, boolean doStart)
    {
        List<Target<I>> targets = new ArrayList<>();

        for (Slot slot : screen.getMenu().slots)
        {
            if (slot.isActive() && slot.isFake() && slot instanceof AbstractStackTypedSlot sSlot)
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
        private final Rect2i area;


        public IStackTarget(AbstractStackTypedSlot slot, BDBaseGUI screen)
        {
            this.slot = slot;
            this.area = new Rect2i(screen.getGuiLeft() + slot.x, screen.getGuiTop() + slot.y, 16, 16);
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

            PacketDistributor.sendToServer(new SetSlotDirectlyPacket(slot.index, new KeyAmount(dragging, 1)));

        }
    }
}
