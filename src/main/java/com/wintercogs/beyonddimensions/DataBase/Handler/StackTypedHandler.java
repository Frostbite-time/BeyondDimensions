package com.wintercogs.beyonddimensions.DataBase.Handler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.wintercogs.beyonddimensions.DataBase.Stack.IStackType;
import com.wintercogs.beyonddimensions.DataBase.Stack.ItemStackType;
import com.wintercogs.beyonddimensions.Registry.StackTypeRegistry;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

// 一个通用的，用于存储IStackType实例的类
// 所有相关方法都已经在接口以默认方法，非类型化的实现
public class StackTypedHandler implements IStackTypedHandler
{
    // 存储
    private List<IStackType> storage;
    // 类型化存储，为其分化包装提供良好的性能
    private final Map<ResourceLocation, List<Integer>> typeIdIndex = new HashMap<>();

    public StackTypedHandler(int size)
    {
        storage = new ArrayList<>(size);
        // 保证非空
        for(int i=0;i<size;i++)
        {
            storage.add(new ItemStackType());
            typeIdIndex.computeIfAbsent(ItemStackType.ID, k -> new ArrayList<>()).add(storage.size() - 1);
        }
    }

    @Override
    public List<IStackType> getStorage()
    {
        return Collections.unmodifiableList(this.storage);
    }

    // 可以在构造时候再重写，根据需求传入实现
    @Override
    public void onChange()
    {

    }

    @Override
    public int getSlots()
    {
        return storage.size();
    }

    @Override
    public void clearStorage()
    {
        storage.clear();
        typeIdIndex.clear();
        onChange();
    }

    @Override
    public IStackType getStackBySlot(int slot)
    {
        return IStackTypedHandler.super.getStackBySlot(slot);
    }

    // 返回找到的第一个对应Stack
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
    public boolean hasStackType(IStackType other)
    {
        if(getStackByStack(other) != null)
            return true;
        else
            return false;
    }

    @Override
    public void setStackDirectly(int slot, IStackType stack)
    {
        ResourceLocation newTypeId = stack.getTypeId();
        ResourceLocation oldTypeId = getStorage().get(slot).getTypeId();
        storage.set(slot,stack.copy());
        // 更新索引
        typeIdIndex.computeIfAbsent(oldTypeId, k -> new ArrayList<>()).remove(Integer.valueOf(slot));
        typeIdIndex.computeIfAbsent(newTypeId, k -> new ArrayList<>()).add(slot);
        onChange();
    }

    @Override
    public void addStackToIndexDirectly(int slot, IStackType stack)
    {
        //使用add方法增加Stack，并且更新索引
        ResourceLocation newTypeId = stack.getTypeId();
        storage.add(slot,stack.copy());
        // storage自增导致的可能的空索引位置无需管，因为那个位置是null。如果读取必然出错，这是编写时候由其他方法保证的
        typeIdIndex.computeIfAbsent(newTypeId, k -> new ArrayList<>()).add(slot);
        onChange();
    }

    @Override
    public void addStackDirectly(IStackType stack)
    {
        //使用add方法增加Stack，并且更新索引
        ResourceLocation newTypeId = stack.getTypeId();
        int slot = storage.size();
        storage.add(stack.copy());
        typeIdIndex.computeIfAbsent(newTypeId, k -> new ArrayList<>()).add(slot);
        onChange();
    }

    @Override
    public IStackType insert(int slot, IStackType stack, boolean simulate)
    {
        // 检查槽位有效性
        if (slot < 0 || slot >= storage.size()) {
            return stack.copy();
        }
        // 检查堆叠有效性
        if (!isStackValid(slot, stack) || stack.isEmpty()) {
            return stack.copy();
        }

        IStackType current = storage.get(slot);
        long maxInsert;
        IStackType remaining;

        if (current == null || current.isEmpty()) {
            // 空槽位：创建新堆叠
            maxInsert = Math.min(stack.getStackAmount(), getSlotCapacity(slot));
            maxInsert = Math.min(maxInsert,stack.getVanillaMaxStackSize()); // 如需突破堆叠上限，则需要重写并移除这条语句
            if (maxInsert <= 0) return stack.copy();

            remaining = stack.copyWithCount(stack.getStackAmount() - maxInsert);
            if (!simulate) {
                IStackType newStack = stack.copyWithCount(maxInsert);
                // 更新索引表 - 移除空槽位索引
                ResourceLocation emptyTypeId = current.getTypeId();
                typeIdIndex.computeIfAbsent(emptyTypeId, k -> new ArrayList<>()).remove(Integer.valueOf(slot));
                
                // 添加到新类型索引
                ResourceLocation newTypeId = stack.getTypeId();
                typeIdIndex.computeIfAbsent(newTypeId, k -> new ArrayList<>()).add(slot);
                
                storage.set(slot, newStack);
                onChange();
            }
        } else {
            // 已有堆叠：检查类型一致性
            if (!current.isSameTypeSameComponents(stack)) {
                return stack.copy();
            }
            // 计算可插入量
            long slotCap = Math.min(getSlotCapacity(slot),stack.getVanillaMaxStackSize());// 如需突破堆叠上限，则需要重写并移除这条语句
            maxInsert = Math.min(
                    stack.getStackAmount(),
                    slotCap - current.getStackAmount()
            );
            if (maxInsert <= 0) return stack.copy();

            remaining = stack.copyWithCount(stack.getStackAmount() - maxInsert);
            if (!simulate) {
                current.grow(maxInsert);
                onChange();
            }
        }
        return remaining;
    }

    @Override
    public IStackType insert(IStackType stack, boolean simulate)
    {
        IStackType remaining = stack.copy();
        ResourceLocation typeId = stack.getTypeId();
        
        // 第一阶段：利用typeIdIndex快速定位相同类型的槽位进行合并
        List<Integer> matchingSlots = typeIdIndex.get(typeId);
        if (matchingSlots != null) {
            for (Integer slot : matchingSlots) {
                IStackType current = storage.get(slot);
                if (!current.isEmpty() && current.isSameTypeSameComponents(stack)) {
                    remaining = insert(slot, remaining, simulate);
                    if (remaining.isEmpty()) return remaining;
                }
            }
        }

        // 第二阶段：填充空槽位 - 必须遍历所有槽位以确保安全
        if (!remaining.isEmpty()) {
            for (int slot = 0; slot < getSlots(); slot++) {
                IStackType current = storage.get(slot);
                if (current.isEmpty()) {
                    remaining = insert(slot, remaining, simulate);
                    if (remaining.isEmpty()) return remaining;
                }
            }
        }

        return remaining;
    }

    @Override
    public IStackType extract(int slot, long count, boolean simulate)
    {
        if (slot < 0 || slot >= storage.size()) {
            return new ItemStackType(); // 以不带参数ItemStackType作为空体
        }

        IStackType current = storage.get(slot);
        if (current.isEmpty()) {
            return current.getEmpty();
        }

        long extractable = Math.min(count, current.getStackAmount());
        IStackType extracted = current.copyWithCount(extractable);

        if (!simulate) {
            ResourceLocation oldTypeId = current.getTypeId();
            current.shrink(extractable);
            if (current.isEmpty()) {
                // 更新索引，移除旧类型索引，添加到空类型索引
                storage.set(slot, current.getEmpty());
                typeIdIndex.computeIfAbsent(oldTypeId, k -> new ArrayList<>()).remove(Integer.valueOf(slot));
                typeIdIndex.computeIfAbsent(ItemStackType.ID, k -> new ArrayList<>()).add(slot);
            }
            onChange();
        }

        return extracted;
    }

    @Override
    public IStackType extract(IStackType stack, boolean simulate)
    {
        IStackType result = stack.getEmpty();
        long remaining = stack.getStackAmount();
        ResourceLocation typeId = stack.getTypeId();
        
        // 利用typeIdIndex直接获取匹配类型的槽位
        List<Integer> matchingSlots = typeIdIndex.get(typeId);
        if (matchingSlots == null || matchingSlots.isEmpty()) {
            return result; // 没有此类型的槽位
        }
        
        // 只遍历匹配类型的槽位
        for (Integer slot : matchingSlots) {
            IStackType current = storage.get(slot);
            if (current.isEmpty() || !current.isSameTypeSameComponents(stack)) {
                continue;
            }

            // 计算可提取量
            long available = current.getStackAmount();
            long toExtract = Math.min(remaining, available);
            if (toExtract <= 0) continue;

            // 执行提取操作
            IStackType extracted = extract(slot, toExtract, simulate);
            if (!extracted.isEmpty()) {
                if (result.isEmpty()) {
                    result = extracted;
                } else {
                    result.grow(extracted.getStackAmount());
                }
                remaining -= extracted.getStackAmount();
                if (remaining <= 0) break;
            }
        }

        return result.copyWithCount(stack.getStackAmount() - remaining);
    }

    @Override
    public long getSlotCapacity(int slot)
    {
        return 64000L; // 最大容量兼容流体，实际能插入多少，由接口默认方法的insert(int slot, IStackType stack, boolean simulate)决定
                       // 默认实现会取slot容量和要插入的堆叠的原版最大容量的最小值。如需突破上限请修改实现
    }

    @Override
    public boolean isStackValid(int slot, IStackType stack)
    {
        return true;
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
                storage.add(new ItemStackType()); // 为空体添加空体占位
                typeIdIndex.computeIfAbsent(ItemStackType.ID, k -> new ArrayList<>()).add(storage.size() - 1);
            }
            else
            {
                ResourceLocation typeId = ResourceLocation.parse(type);
                IStackType stackEmpty = StackTypeRegistry.getType(typeId).copy();
                IStackType stackActual = stackEmpty.deserializeNBT(stackTag.getCompound("TypedStack"),provider);
                storage.add(stackActual); // 无论是不是空体，都添加
                typeIdIndex.computeIfAbsent(stackActual.getTypeId(), k -> new ArrayList<>()).add(storage.size() - 1);
            }

        }
    }
    // endregion

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