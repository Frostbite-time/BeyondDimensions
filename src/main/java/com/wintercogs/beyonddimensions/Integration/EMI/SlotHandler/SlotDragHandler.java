package com.wintercogs.beyonddimensions.Integration.EMI.SlotHandler;

import com.wintercogs.beyonddimensions.Api.DataBase.Stack.IStackType;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.ItemStackType;
import com.wintercogs.beyonddimensions.Api.Registry.StackTypeRegistry;
import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.GUI.BDBaseGUI;
import com.wintercogs.beyonddimensions.Integration.AE.AEHelper;
import com.wintercogs.beyonddimensions.Menu.Slot.AbstractStackTypedSlot;
import com.wintercogs.beyonddimensions.Network.Packet.ClientOrServer.SetSlotDirectlyPacket;
import com.wintercogs.beyonddimensions.Registry.PacketRegister;
import dev.emi.emi.api.EmiDragDropHandler;
import dev.emi.emi.api.stack.EmiIngredient;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.inventory.Slot;

import java.util.function.BiConsumer;
import java.util.function.Predicate;

public class SlotDragHandler implements EmiDragDropHandler<BDBaseGUI>
{
    private final Predicate<Slot> slotFilter = slot -> {
        if(slot instanceof AbstractStackTypedSlot sSlot)
        {
            return sSlot.isFake();
        }
        return false;
    };
    private final BiConsumer<Slot, EmiIngredient> dropHandler = (slot, ingredient) -> {
        // stackKey 是如 Item Fluid的类
        Object stackKey = ingredient.getEmiStacks().get(0).getKey();
        CompoundTag dataComponentPatch = ingredient.getEmiStacks().get(0).getNbt();

        IStackType dragging = new ItemStackType();
        for(IStackType type : StackTypeRegistry.getAllTypes())
        {
            if(type.getSourceClass().isAssignableFrom(stackKey.getClass()))
            {

                dragging = type.fromObject(stackKey,1,dataComponentPatch);
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

        PacketRegister.INSTANCE.sendToServer(new SetSlotDirectlyPacket(slot.index,dragging));
    };

    public SlotDragHandler() {}
    @Override
    public boolean dropStack(BDBaseGUI screen, EmiIngredient ingredient, int x, int y) {
        // 转换屏幕坐标到容器相对坐标
        double mouseX = x - screen.getGuiLeft();
        double mouseY = y - screen.getGuiTop();
        // 遍历所有槽位检查是否在有效区域内
        for (Slot slot : screen.getMenu().slots) {
            if (slotFilter.test(slot) && isMouseOverSlot(screen, slot, mouseX, mouseY)) {
                dropHandler.accept(slot, ingredient);
                return true;
            }
        }
        return false;
    }

    @Override
    public void render(BDBaseGUI screen, EmiIngredient dragged, GuiGraphics draw, int mouseX, int mouseY, float delta)
    {
        EmiDragDropHandler.super.render(screen, dragged, draw, mouseX, mouseY, delta);
    }

    private boolean isMouseOverSlot(BDBaseGUI screen, Slot slot,
                                    double mouseX, double mouseY) {
        return isPointInRegion(screen, slot.x, slot.y, 16, 16, mouseX, mouseY);
    }
    private boolean isPointInRegion(BDBaseGUI<?> screen, int x, int y,
                                    int width, int height, double pointX, double pointY) {
        return pointX >= x - 1 && pointX < x + width + 1 &&
                pointY >= y - 1 && pointY < y + height + 1;
    }

}
