package com.wintercogs.beyonddimensions.Integration.JEI;

import com.wintercogs.beyonddimensions.Api.DataBase.Stack.IStackKey;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.ItemStackKey;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.KeyAmount;
import com.wintercogs.beyonddimensions.Api.Registry.StackKeyRegistry;
import com.wintercogs.beyonddimensions.GUI.BDBaseGUI;
import com.wintercogs.beyonddimensions.Menu.Slot.AbstractStackTypedSlot;
import com.wintercogs.beyonddimensions.Packet.SetSlotDirectlyPacket;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

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
//            if (BeyondDimensions.AELoaded)
//            {
//                if (dragging instanceof ItemStackKey draggingItemKey && !dragging.isEmpty())
//                {
//                    appeng.api.stacks.GenericStack genericContent = appeng.api.stacks.GenericStack.fromItemStack(draggingItemKey.copyStack());
//
//                    if (genericContent != null)
//                    {
//                        dragging = AEHelper.fromAEKeyToIStack(genericContent.what()).orElse(ItemStackKey.EMPTY);
//                    }
//
//                }
//            }

            ClientPacketDistributor.sendToServer(new SetSlotDirectlyPacket(slot.index, new KeyAmount(dragging, 1)));

        }
    }
}
