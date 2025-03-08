package com.wintercogs.beyonddimensions.DataBase.Handler;

import com.wintercogs.beyonddimensions.DataBase.Stack.IStackType;

import java.util.ArrayList;
import java.util.List;

// 一个通用的，用于存储IStackType实例的类
// 所有相关方法都已经在接口以默认方法，非类型化的实现
public class StackTypedHandler implements IStackTypedHandler
{
    private List<IStackType> storage;

    StackTypedHandler(int size)
    {
        storage = new ArrayList<>(size);
    }

    @Override
    public List<IStackType> getStorage()
    {
        return storage;
    }

    @Override
    public void onChange()
    {

    }

    @Override
    public long getSlotLimit(int slot)
    {
        return getStorage().get(slot).getVanillaMaxStackSize();
    }
}