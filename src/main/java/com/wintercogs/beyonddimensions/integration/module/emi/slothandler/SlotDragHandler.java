package com.wintercogs.beyonddimensions.integration.module.emi.slothandler;

import com.wintercogs.beyonddimensions.api.storage.key.IStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.api.storage.key.StackKeyRegistry;
import com.wintercogs.beyonddimensions.api.storage.key.impl.ItemStackKey;
import com.wintercogs.beyonddimensions.client.ui.InventoryScreenAccess;
import com.wintercogs.beyonddimensions.common.menu.widget.slot.AbstractStackTypedSlot;
import com.wintercogs.beyonddimensions.integration.ModPresence;
import com.wintercogs.beyonddimensions.integration.OtherModIds;
import com.wintercogs.beyonddimensions.integration.module.ae2.AEHelper;
import com.wintercogs.beyonddimensions.network.packet.both.SetSlotDirectlyPacket;
import dev.emi.emi.api.EmiDragDropHandler;
import dev.emi.emi.api.stack.EmiIngredient;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.network.PacketDistributor;


public class SlotDragHandler implements EmiDragDropHandler<Screen>
{

    public SlotDragHandler()
    {
    }

    @Override
    public void render(Screen screen, EmiIngredient dragged, GuiGraphics draw, int mouseX, int mouseY, float delta)
    {
        var menu = InventoryScreenAccess.menu(screen);
        if (menu == null)
            return;

        for (Slot slot : menu.slots)
        {
            Rect2i area = slot instanceof AbstractStackTypedSlot && slot.isFake() ? InventoryScreenAccess.slot(screen, slot) : null;
            if (area != null)
                draw.fill(area.getX(), area.getY(), area.getX() + area.getWidth(), area.getY() + area.getHeight(), 0x8822BB33);
        }
    }

    @Override
    public boolean dropStack(Screen screen, EmiIngredient ingredient, int x, int y)
    {
        var menu = InventoryScreenAccess.menu(screen);
        if (menu == null || ingredient.getEmiStacks().isEmpty())
            return false;

        for (Slot slot : menu.slots)
        {
            if (slot instanceof AbstractStackTypedSlot && slot.isFake())
            {
                Rect2i slotRect = InventoryScreenAccess.slot(screen, slot);

                if (slotRect != null && slotRect.contains(x, y))
                {
                    // stackKey 是如 Item Fluid的类
                    Object stackKey = ingredient.getEmiStacks().get(0).getKey();
                    DataComponentPatch dataComponentPatch = ingredient.getEmiStacks().get(0).getComponentChanges();

                    IStackKey<?> dragging = ItemStackKey.EMPTY;
                    for (IStackKey<?> type : StackKeyRegistry.getAllTypes())
                    {
                        if (type.getSourceClass().isAssignableFrom(stackKey.getClass()))
                        {

                            dragging = type.fromSourceObject(stackKey, dataComponentPatch);
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

                    return true; // 走到发包即表示完成
                }
            }
        }

        return false;
    }
}
