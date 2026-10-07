package com.wintercogs.beyonddimensions.common.menu.widget.slot;

import com.wintercogs.beyonddimensions.api.storage.handler.IStackHandler;
import com.wintercogs.beyonddimensions.api.storage.key.IStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.api.storage.key.impl.EmptyStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.impl.ItemStackKey;
import com.wintercogs.beyonddimensions.common.menu.BDBaseMenu;


// 用于标记性槽位的AbstractStackTypedSlot实现
// 注意，标记性槽位必须用于有序容器
public class FlagStackTypedSlot extends AbstractStackTypedSlot
{

    public FlagStackTypedSlot(BDBaseMenu menu, IStackHandler storage, int slotIndex, int xPosition, int yPosition)
    {
        super(menu, storage, slotIndex, xPosition, yPosition);
        setFake(true); // 标记性槽位为假槽位
    }

    @Override
    public boolean isOrdered()
    {
        return true;
    }

    // 内部会copy这个stack，因此无需再次操作
    // 直接写入的标记同样要遵守存储对资源类型的限制
    @Override
    public void setStackDirectly(IStackKey<?> key, long amount)
    {
        if (key == null || (!key.isEmpty() && amount > 0 && !storage.isStackValid(theSlot, key)))
            return;
        storage.setStackDirectly(theSlot, key, amount);
    }

    @Override
    public KeyAmount safeInsert(IStackKey<?> key, long amount)
    {
        if (key != null)
        {
            setStackDirectly(key, amount);
        }
        return new KeyAmount(EmptyStackKey.INSTANCE, amount);
    }

    @Override
    public KeyAmount safeExtract(IStackKey<?> key, long amount)
    {
        setStackDirectly(ItemStackKey.EMPTY, amount);
        return new KeyAmount(ItemStackKey.EMPTY, amount); // 标记槽永远取出空
    }

}
