package com.wintercogs.beyonddimensions.integration.module.emi.slothandler;

import appeng.api.stacks.GenericStack;
import com.wintercogs.beyonddimensions.api.storage.key.IStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.api.storage.key.StackKeyRegistry;
import com.wintercogs.beyonddimensions.api.storage.key.impl.ItemStackKey;
import com.wintercogs.beyonddimensions.client.gui.BDBaseGUI;
import com.wintercogs.beyonddimensions.common.init.BDPackets;
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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.inventory.Slot;


public class SlotDragHandler implements EmiDragDropHandler<Screen>
{

    public SlotDragHandler()
    {
    }

    @Override
    public void render(Screen screen, EmiIngredient dragged, GuiGraphics draw, int mouseX, int mouseY, float delta)
    {
        if (!(screen instanceof BDBaseGUI bdGUI))
            return;

        for (Slot slot : bdGUI.getMenu().slots)
        {
            if (slot instanceof AbstractStackTypedSlot sSlot && sSlot.isFake())
            {
                int slotLeft = bdGUI.getGuiLeft() + slot.x;
                int slotTop = bdGUI.getGuiTop() + slot.y;

                draw.fill(slotLeft, slotTop,
                        slotLeft + 16, slotTop + 16,
                        0x8822BB33);
            }
        }
    }

    @Override
    public boolean dropStack(Screen screen, EmiIngredient ingredient, int x, int y)
    {
        if (!(screen instanceof BDBaseGUI bdGUI))
            return false;

        for (Slot slot : bdGUI.getMenu().slots)
        {
            if (slot instanceof AbstractStackTypedSlot sSlot && sSlot.isFake())
            {
                int slotLeft = bdGUI.getGuiLeft() + slot.x;
                int slotTop = bdGUI.getGuiTop() + slot.y;
                Rect2i slotRect = new Rect2i(slotLeft, slotTop, 16, 16);

                if (slotRect.contains(x, y))
                {
                    // stackKey 是如 Item Fluid的类
                    Object stackKey = ingredient.getEmiStacks().get(0).getKey();
                    CompoundTag dataComponentPatch = ingredient.getEmiStacks().get(0).getNbt();

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
                            GenericStack genericContent = GenericStack.fromItemStack(draggingItemKey.copyStack());

                            if (genericContent != null)
                            {
                                dragging = AEHelper.fromAEKeyToIStack(genericContent.what()).orElse(ItemStackKey.EMPTY);
                            }

                        }
                    }

                    BDPackets.INSTANCE.sendToServer(new SetSlotDirectlyPacket(slot.index, new KeyAmount(dragging, 1)));

                    return true; // 走到发包即表示完成
                }
            }
        }

        return false;
    }
}
