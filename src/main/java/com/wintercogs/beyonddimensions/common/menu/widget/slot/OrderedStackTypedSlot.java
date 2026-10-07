package com.wintercogs.beyonddimensions.common.menu.widget.slot;

import com.wintercogs.beyonddimensions.api.storage.handler.IStackHandler;
import com.wintercogs.beyonddimensions.api.storage.key.IStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.api.storage.key.impl.EmptyStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.impl.ItemStackKey;
import com.wintercogs.beyonddimensions.common.menu.BDBaseMenu;


public class OrderedStackTypedSlot extends AbstractStackTypedSlot
{

    public OrderedStackTypedSlot(BDBaseMenu menu, IStackHandler stackTypedHandler, int slotIndex, int xPosition, int yPosition)
    {
        super(menu, stackTypedHandler, slotIndex, xPosition, yPosition);
    }

    public OrderedStackTypedSlot(BDBaseMenu menu, IStackHandler stackTypedHandler, int slotIndex, int quickMoveSlotStartIndex, int quickMoveSlotEndIndex, int xPosition, int yPosition)
    {
        super(menu, stackTypedHandler, slotIndex, quickMoveSlotStartIndex, quickMoveSlotEndIndex, xPosition, yPosition);
    }

    @Override
    public boolean isOrdered()
    {
        return true;
    }

    @Override
    public KeyAmount safeInsert(IStackKey<?> key, long amount)
    {
        if (key != null)
        {
            // storage的insert应当考虑到一切情况
            return storage.insert(theSlot, key, amount, false);
        }
        return new KeyAmount(ItemStackKey.EMPTY, 0);
    }

    @Override
    public KeyAmount safeExtract(IStackKey<?> key, long amount)
    {
        if (key != null && key.getTypeId().equals(getStack().key().getTypeId()) && key.isSameTypeSameComponents(getStack().key()))
        {
            return storage.extract(theSlot, amount, false);
        }
        return new KeyAmount(EmptyStackKey.INSTANCE, amount);
    }

}
