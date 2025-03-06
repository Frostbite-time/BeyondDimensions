package com.wintercogs.beyonddimensions.DataBase.Storage;

import com.wintercogs.beyonddimensions.DataBase.DimensionsNet;
import com.wintercogs.beyonddimensions.DataBase.Stack.IStackType;
import com.wintercogs.beyonddimensions.DataBase.Stack.StackCreater;
import com.wintercogs.beyonddimensions.Registry.StackTypeRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;

public class UnifiedStorage {
    private DimensionsNet net;
    private final ArrayList<IStackType> storage = new ArrayList<>();

    public UnifiedStorage(DimensionsNet net) {
        this.net = net;
    }

    public void onChange()
    {
        net.setDirty();
    }

    public ArrayList<IStackType> getStorage()
    {
        return this.storage;
    }

    public boolean hasStackType(IStackType other)
    {
        if(getStackByStack(other) != null)
            return true;
        else
            return false;
    }

    public IStackType getStackByIndex(int index)
    {
        if (index >= 0 && index < storage.size())
        {
            return storage.get(index);
        }
        else
        {
            return null;
        }
    }

    public IStackType getStackByStack(IStackType stackType)
    {
        for (IStackType existing : storage)
        {
            if (existing.getTypeId().equals(stackType.getTypeId()))
            {
                if(existing.isSameTypeSameComponents(stackType))
                    return existing;
            }
        }
        return null;
    }


    // region 核心操作方法
    // 插入stack 返回剩余量
    public IStackType insert(IStackType stack,boolean simulate) {
        if (stack.isEmpty()) return StackCreater.CreateEmpty(stack.getTypeId());

        // 尝试合并现有堆叠
        for (IStackType existing : storage) {
            if (existing.getTypeId().equals(stack.getTypeId())) {
                if (existing.isSameTypeSameComponents(stack)) {
                    if (!simulate) {
                        existing.grow(stack.getStackAmount());
                        onChange();
                        return StackCreater.CreateEmpty(stack.getTypeId());
                    }
                }
            }
        }
        // 现有堆叠未找到，尝试新增
        if(!simulate)
        {
            storage.add(stack.copy());
            onChange();
        }
        return StackCreater.CreateEmpty(stack.getTypeId());
    }

    // 尝试按类型导出，返回实际导出量
    public IStackType extract(IStackType stack, boolean simulate) {
        if (stack.isEmpty()) return stack.getEmpty();

        for (int i = 0; i < storage.size(); i++) {
            IStackType existing = storage.get(i);
            if (existing.getTypeId().equals(stack.getTypeId())) {
                if (existing.isSameTypeSameComponents(stack)) {
                    long extracted = Math.min(stack.getStackAmount(), existing.getStackAmount());
                    IStackType sim = existing.copy();
                    IStackType result = sim.split(extracted);

                    if (!simulate) {
                        existing.shrink(extracted);
                        if (existing.getStackAmount() <= 0) {
                            storage.remove(i);
                        }
                        onChange();
                    }
                    return result;
                }
            }
        }
        return stack.getEmpty();
    }

    // 尝试按槽位导出 返回实际导出量
    public IStackType extract(ResourceLocation typeId ,int slot,long amount, boolean simulate) {
        IStackType existing = storage.get(slot);
        if (existing.isEmpty()) return existing.getEmpty();

        if (existing.getTypeId().equals(typeId)) {
                long extracted = Math.min(amount, existing.getStackAmount());
                IStackType sim = existing.copy();
                IStackType result = sim.split(extracted);
                if (!simulate) {
                    existing.shrink(extracted);
                    if (existing.getStackAmount() <= 0) {
                        storage.remove(slot);
                    }
                    onChange();
                }
                return result;
        }
        return existing.getEmpty();
    }
    // endregion

    // region 序列化方法
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        ListTag stacksTag = new ListTag();

        for (IStackType stack : storage) {
            // 修改后的序列化代码
            if(stack.isEmpty())
                continue; // 不序列化空物品
            CompoundTag stackTag = new CompoundTag();
            // 使用类型安全的序列化方式 将堆叠数据放入"Data"标签
            stackTag.put("TypedStack",stack.serializeNBT(provider));
            stackTag.putString("Type",stack.getTypeId().toString());
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
            ResourceLocation typeId = ResourceLocation.parse(stackTag.getString("Type"));
            IStackType stackEmpty = StackTypeRegistry.getType(typeId).copy();
            IStackType stackActual = stackEmpty.deserializeNBT(stackTag.getCompound("TypedStack"),provider);
            if(stackActual.isEmpty())
                continue; // 不添加空物品
            getStorage().add(stackActual);
        }
    }
    // endregion
}

