package com.wintercogs.beyonddimensions.DataBase.Handler;

import com.wintercogs.beyonddimensions.DataBase.Stack.IStackType;
import com.wintercogs.beyonddimensions.DataBase.Stack.ItemStackType;
import com.wintercogs.beyonddimensions.Registry.StackTypeRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

// 一个通用的，用于存储IStackType实例的类
// 所有相关方法都已经在接口以默认方法，非类型化的实现
public class StackTypedHandler implements IStackTypedHandler
{
    private List<IStackType> storage;

    public StackTypedHandler(int size)
    {
        storage = new ArrayList<>(size);
        // 保证非空
        for(int i=0;i<size;i++)
        {
            storage.add(new ItemStackType());
        }
    }

    @Override
    public List<IStackType> getStorage()
    {
        return storage;
    }

    // 可以在构造时候再重写，根据需求传入实现
    @Override
    public void onChange()
    {

    }

    @Override
    public long getSlotCapacity(int slot)
    {
        if(slot<0||slot>=getStorage().size())
            return 64L;
        IStackType stack = getStorage().get(slot);
        if(stack !=null)
            return stack.getVanillaMaxStackSize();
        else
            return 64L;
    }

    // region 序列化方法
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        ListTag stacksTag = new ListTag();

        for (int i = 0; i< getStorage().size() ;i++) {
            IStackType stack = getStorage().get(i);

            CompoundTag stackTag = new CompoundTag();
            if(stack.isEmpty()) // 为空物品执行占位机制
            {
                stackTag.put("TypedStack",IntTag.valueOf(1));
                stackTag.putString("Type","Empty");
            }
            else
            {
                // 使用类型安全的序列化方式 将堆叠数据放入"Data"标签
                stackTag.put("TypedStack",stack.serializeNBT(provider));
                stackTag.putString("Type",stack.getTypeId().toString());

            }
            stacksTag.add(stackTag);
        }

        tag.put("Stacks", stacksTag);
        return tag;
    }

    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        storage.clear();
        ListTag stacksTag = tag.getList("Stacks", Tag.TAG_COMPOUND);

        for (Tag t : stacksTag) {
            CompoundTag stackTag = (CompoundTag) t;
            String type = stackTag.getString("Type");
            if(type.equals("Empty"))
            {
                getStorage().add(new ItemStackType()); // 为空体添加空体占位
            }
            else
            {
                ResourceLocation typeId = ResourceLocation.parse(type);
                IStackType stackEmpty = StackTypeRegistry.getType(typeId).copy();
                IStackType stackActual = stackEmpty.deserializeNBT(stackTag.getCompound("TypedStack"),provider);
                getStorage().add(stackActual); // 无论是不是空体，都添加
            }

        }
    }
    // endregion
}