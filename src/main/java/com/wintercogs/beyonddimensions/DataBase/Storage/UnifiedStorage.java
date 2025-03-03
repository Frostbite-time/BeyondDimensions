package com.wintercogs.beyonddimensions.DataBase.Storage;

import com.wintercogs.beyonddimensions.DataBase.DimensionsNet;
import com.wintercogs.beyonddimensions.DataBase.Stack.GenericStack;
import com.wintercogs.beyonddimensions.DataBase.Stack.IStackType;
import com.wintercogs.beyonddimensions.Registry.StackTypeRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public class UnifiedStorage {
    private DimensionsNet net;
    private final List<GenericStack> stacks = new ArrayList<>();

    public UnifiedStorage(DimensionsNet net) {
        this.net = net;
    }

    public void onChange()
    {
        net.setDirty();
    }


    // region 核心操作方法
    public <T> void insert(ResourceLocation typeId, T stack, long amount, boolean simulate) {
        IStackType<T> type = StackTypeRegistry.getType(typeId);
        if (type.isEmpty(stack)) return;

        // 尝试合并现有堆叠
        for (GenericStack gs : stacks) {
            if (gs.getTypeId().equals(typeId)) {
                T existing = gs.getStack(type);
                if (type.isSameStack(existing, stack)) {
                    long remaining = type.mergeStacks(existing, stack, amount);
                    if (!simulate) {
                        gs.setAmount(gs.getAmount() + (amount - remaining));
                        onChange();
                    }
                    return;
                }
            }
        }

        // 添加新堆叠
        if (!simulate) {
            T newStack = type.copyStackWithCount(stack, amount);
            stacks.add(new GenericStack(typeId, newStack, amount));
            onChange();
        }
    }

    public <T> T extract(ResourceLocation typeId, T stack, long amount, boolean simulate) {
        IStackType<T> type = StackTypeRegistry.getType(typeId);
        if (type.isEmpty(stack)) return type.getEmptyStack();

        for (int i = 0; i < stacks.size(); i++) {
            GenericStack gs = stacks.get(i);
            if (gs.getTypeId().equals(typeId)) {
                T existing = gs.getStack(type);
                if (type.isSameStackSameComponents(existing, stack)) {
                    long extracted = Math.min(amount, gs.getAmount());
                    T result = type.splitStack(existing, extracted);

                    if (!simulate) {
                        gs.setAmount(gs.getAmount() - extracted);
                        if (gs.getAmount() <= 0) {
                            stacks.remove(i);
                        }
                        onChange();
                    }
                    return result;
                }
            }
        }
        return type.getEmptyStack();
    }
    // endregion

    // region 序列化方法
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        ListTag stacksTag = new ListTag();

        for (GenericStack gs : stacks) {
            // 修改后的序列化代码
            CompoundTag stackTag = new CompoundTag();
            ResourceLocation typeId = gs.getTypeId();
            IStackType<?> rawType = StackTypeRegistry.getType(typeId);

            stackTag.putString("Type", typeId.toString());
            stackTag.putLong("Amount", gs.getAmount());

            // 使用类型安全的序列化方式 将堆叠数据放入"Data"标签
            rawType.serializeNBT(stackTag, provider, gs);

            stacksTag.add(stackTag);
        }

        tag.put("Stacks", stacksTag);
        return tag;
    }

    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        stacks.clear();
        ListTag stacksTag = tag.getList("Stacks", Tag.TAG_COMPOUND);

        for (Tag t : stacksTag) {
            CompoundTag stackTag = (CompoundTag) t;
            ResourceLocation typeId = ResourceLocation.parse(stackTag.getString("Type"));
            long amount = stackTag.getLong("Amount");
            CompoundTag data = stackTag.getCompound("Data");

            IStackType<?> type = StackTypeRegistry.getType(typeId);
            Object stack = type.deserializeNBT(data, provider);
            stacks.add(new GenericStack(typeId, stack, amount));
        }
    }
    // endregion
}

