package com.wintercogs.beyonddimensions.common.menu.widget.slot;

import com.wintercogs.beyonddimensions.api.storage.handler.IStackHandler;
import com.wintercogs.beyonddimensions.api.storage.key.IStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.api.storage.key.impl.ItemStackKey;
import com.wintercogs.beyonddimensions.common.menu.BDBaseMenu;


public class DisorderedStackTypedSlot extends AbstractStackTypedSlot
{

    public DisorderedStackTypedSlot(BDBaseMenu menu, IStackHandler stackTypedHandler, int slotIndex, int xPosition, int yPosition)
    {
        super(menu, stackTypedHandler, slotIndex, xPosition, yPosition);
    }

    public DisorderedStackTypedSlot(BDBaseMenu menu, IStackHandler stackTypedHandler, int slotIndex, int quickMoveSlotStartIndex, int quickMoveSlotEndIndex, int xPosition, int yPosition)
    {
        super(menu, stackTypedHandler, slotIndex, quickMoveSlotStartIndex, quickMoveSlotEndIndex, xPosition, yPosition);
    }

    @Override
    public boolean isOrdered()
    {
        return false;
    }

    @Override
    public KeyAmount safeInsert(IStackKey<?> stack, long amount)
    {
        if (stack != null)
        {
            return storage.insert(stack, amount, false);
        }
        return new KeyAmount(ItemStackKey.EMPTY, 0);

    }

    @Override
    public KeyAmount safeExtract(IStackKey<?> stack, long amount)
    {
        if (stack != null)
        {
            return storage.extract(stack, amount, false, false);
        }
        return new KeyAmount(ItemStackKey.EMPTY, 0);
    }

}
