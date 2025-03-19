package com.wintercogs.beyonddimensions.DataBase.Storage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.wintercogs.beyonddimensions.DataBase.DimensionsNet;
import com.wintercogs.beyonddimensions.DataBase.Handler.IStackTypedHandler;
import com.wintercogs.beyonddimensions.DataBase.Stack.IStackType;
import com.wintercogs.beyonddimensions.DataBase.Stack.StackCreater;
import com.wintercogs.beyonddimensions.Registry.StackTypeRegistry;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

public class UnifiedStorage implements IStackTypedHandler
{
    private DimensionsNet net;
    private final ArrayList<IStackType> storage = new ArrayList<>();
    // 使用Integer索引而不是直接存储对象引用
    private final Map<ResourceLocation, List<Integer>> typeIdIndex = new HashMap<>();

    public UnifiedStorage(DimensionsNet net) {
        this.net = net;
    }

    @Override
    public void onChange()
    {
        net.setDirty();
    }

    @Override
    public int getSlots()
    {
        return storage.size();
    }

    @Override
    public IStackType getStackBySlot(int slot)
    {
        return IStackTypedHandler.super.getStackBySlot(slot);
    }

    @Override
    public ArrayList<IStackType> getStorage()
    {
        return this.storage;
    }

    @Override
    public boolean hasStackType(IStackType other)
    {
        if(getStackByStack(other) != null)
            return true;
        else
            return false;
    }

    @Override
    public IStackType getStackByStack(IStackType stackType)
    {
        ResourceLocation typeId = stackType.getTypeId();
        List<Integer> indices = typeIdIndex.get(typeId);
        
        if (indices != null) {
            for (Integer index : indices) {
                IStackType existing = storage.get(index);
                if (existing.isSameTypeSameComponents(stackType)) {
                    return existing;
                }
            }
        }
        return null;
    }

    @Override
    public long getSlotCapacity(int slot)
    {
        return Long.MAX_VALUE;
    }

    @Override
    public boolean isStackValid(int slot, IStackType stack)
    {
        return true;
    }


    // region 核心操作方法

    // 插入stack 返回剩余量
    @Override
    public IStackType insert(int slot, IStackType stack, boolean simulate)
    {
        return insert(stack,simulate);
    }

    @Override
    public IStackType insert(IStackType stack,boolean simulate) {
        if (stack.isEmpty()) return StackCreater.CreateEmpty(stack.getTypeId());

        long remaining = stack.getStackAmount(); // 剩余可被插入的量
        long canInsert = Math.min(getSlotCapacity(0),stack.getCustomMaxStackSize()); // 能被插入的空间

        // 尝试合并现有堆叠
        ResourceLocation typeId = stack.getTypeId();
        List<Integer> indices = typeIdIndex.get(typeId);
        
        if (indices != null) {
            for (Integer index : indices) {
                IStackType existing = storage.get(index);
                if (existing.isSameTypeSameComponents(stack)) {
                    canInsert = canInsert - existing.getStackAmount();
                    long actualInsert = Math.min(remaining,canInsert);
                    remaining = remaining-actualInsert;

                    if (!simulate) {
                        existing.grow(actualInsert);
                        onChange();
                    }
                    return stack.copyWithCount(remaining);
                }
            }
        }

        // 现有堆叠未找到，尝试新增
        long actualInsert = Math.min(remaining,canInsert);
        remaining = remaining-actualInsert;
        if(!simulate)
        {
            IStackType newStack = stack.copyWithCount(actualInsert);
            storage.add(newStack);
            
            // 更新索引
            int newIndex = storage.size() - 1;
            typeIdIndex.computeIfAbsent(typeId, k -> new ArrayList<>()).add(newIndex);
            
            onChange();
        }
        return stack.copyWithCount(remaining);
    }

    // 尝试按类型导出，返回实际导出量
    @Override
    public IStackType extract(IStackType stack, boolean simulate) {
        if (stack.isEmpty()) return stack.getEmpty();

        ResourceLocation typeId = stack.getTypeId();
        List<Integer> indices = typeIdIndex.get(typeId);
        
        if (indices != null) {
            for (int i = 0; i < indices.size(); i++) {
                int storageIndex = indices.get(i);
                IStackType existing = storage.get(storageIndex);
                if (existing.isSameTypeSameComponents(stack)) {
                    long extracted = Math.min(stack.getStackAmount(), existing.getStackAmount());
                    IStackType sim = existing.copy();
                    IStackType result = sim.split(extracted);

                    if (!simulate) {
                        existing.shrink(extracted);
                        if (existing.getStackAmount() <= 0) {
                            storage.remove(storageIndex);
                            indices.remove(i);
                            
                            // 更新受影响的索引
                            updateIndicesAfterRemoval(storageIndex);
                            
                            if (indices.isEmpty()) {
                                typeIdIndex.remove(typeId);
                            }
                        }
                        onChange();
                    }
                    return result;
                }
            }
        }
        return stack.getEmpty();
    }

    // 当从storage中移除一个元素后，更新所有受影响的索引值
    private void updateIndicesAfterRemoval(int removedIndex) {
        for (List<Integer> indexList : typeIdIndex.values()) {
            for (int i = 0; i < indexList.size(); i++) {
                int currentIndex = indexList.get(i);
                if (currentIndex > removedIndex) {
                    indexList.set(i, currentIndex - 1);
                }
            }
        }
    }

    // 尝试按槽位导出 返回实际导出量
    @Override
    public IStackType extract(int slot,long amount, boolean simulate) {
        if (slot < 0 || slot >= storage.size()) {
            return null;
        }
        
        IStackType existing = storage.get(slot);
        if (existing.isEmpty()) return existing.getEmpty();

        long extracted = Math.min(amount, existing.getStackAmount());
        IStackType sim = existing.copy();
        IStackType result = sim.split(extracted);
        if (!simulate) {
            existing.shrink(extracted);
            if (existing.getStackAmount() <= 0) {
                ResourceLocation typeId = existing.getTypeId();
                storage.remove(slot);
                
                // 更新索引
                List<Integer> indices = typeIdIndex.get(typeId);
                if (indices != null) {
                    indices.remove(Integer.valueOf(slot));
                    if (indices.isEmpty()) {
                        typeIdIndex.remove(typeId);
                    }
                }
                
                // 更新受影响的索引
                updateIndicesAfterRemoval(slot);
                
                onChange();
            }
        }
        return result;
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
        typeIdIndex.clear();
        ListTag stacksTag = tag.getList("Stacks", Tag.TAG_COMPOUND);

        for (Tag t : stacksTag) {
            CompoundTag stackTag = (CompoundTag) t;
            ResourceLocation typeId = ResourceLocation.parse(stackTag.getString("Type"));
            IStackType stackEmpty = StackTypeRegistry.getType(typeId).copy();
            IStackType stackActual = stackEmpty.deserializeNBT(stackTag.getCompound("TypedStack"),provider);
            if(stackActual.isEmpty())
                continue; // 不添加空物品
                
            getStorage().add(stackActual);
            // 更新索引
            typeIdIndex.computeIfAbsent(typeId, k -> new ArrayList<>()).add(storage.size() - 1);
        }
    }
    // endregion

    // 辅助方法：查找已有堆叠的槽位
    private int findExistingSlot(IStackType stack) {
        ResourceLocation typeId = stack.getTypeId();
        List<Integer> indices = typeIdIndex.get(typeId);
        
        if (indices != null) {
            for (Integer index : indices) {
                IStackType existing = storage.get(index);
                if (existing.isSameTypeSameComponents(stack)) {
                    return index;
                }
            }
        }
        return -1;
    }

    // 辅助方法，用于获取索引表
    public Map<ResourceLocation, List<Integer>> getTypeIdIndexMap()
    {
        return this.typeIdIndex;
    }

    public List<Integer> getTypeIdIndexList(ResourceLocation typeId)
    {
        return this.typeIdIndex.get(typeId);
    }
}

