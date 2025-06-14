package com.wintercogs.beyonddimensions.Integration.JEI;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.DataBase.Stack.IStackType;
import com.wintercogs.beyonddimensions.DataBase.Stack.ItemStackType;
import com.wintercogs.beyonddimensions.GUI.NetInterfaceBaseGUI;
import com.wintercogs.beyonddimensions.Integration.AE.AEHelper;
import com.wintercogs.beyonddimensions.Menu.Slot.StoredStackSlot;
import com.wintercogs.beyonddimensions.Packet.FlagSlotSetPacket;
import com.wintercogs.beyonddimensions.Registry.StackTypeRegistry;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

// 为网络接口注册JEI拖拽支持
public class NetInterfaceGhostHandler implements IGhostIngredientHandler<NetInterfaceBaseGUI>
{

    @Override
    public <I> List<Target<I>> getTargetsTyped(NetInterfaceBaseGUI screen, ITypedIngredient<I> ingredient, boolean doStart)
    {
        List<Target<I>> targets = new ArrayList<>();

        for(Slot slot: screen.getMenu().slots)
        {
            if(slot.isActive() && slot.isFake() && slot instanceof StoredStackSlot sSlot)
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
        private final StoredStackSlot slot;
        private final Rect2i area;


        public IStackTarget(StoredStackSlot slot, NetInterfaceBaseGUI screen)
        {
            this.slot = slot;
            this.area = new Rect2i(screen.getGuiLeft() + slot.x, screen.getGuiTop() + slot.y,16 ,16);
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
            IStackType dragging = new ItemStackType();
            for(IStackType type : StackTypeRegistry.getAllTypes())
            {
                if(type.getStackClass().isAssignableFrom(stackKey.getClass()))
                {

                    dragging = type.getEmpty();
                    dragging.setStack(ingredient);
                    break;
                }
            }

            // AE2通用包裹支持
            if(BeyondDimensions.AELoaded)
            {
                if(dragging instanceof ItemStackType draggingItem && !dragging.isEmpty())
                {
                    appeng.api.stacks.GenericStack genericContent = appeng.api.stacks.GenericStack.fromItemStack(draggingItem.getStack());

                    if(genericContent != null)
                    {
                        dragging = AEHelper.fromAEKeyToIStack(genericContent.what(), 1).orElse(new ItemStackType());
                    }

                }
            }


            IStackType clickItem = slot.getVanillaActualStack();
            // button的数字0代表左键
            PacketDistributor.sendToServer(new FlagSlotSetPacket(slot.index,clickItem,dragging));

        }
    }
}
