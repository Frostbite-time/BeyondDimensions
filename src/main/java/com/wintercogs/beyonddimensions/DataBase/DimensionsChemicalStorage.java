package com.wintercogs.beyonddimensions.DataBase;

import mekanism.api.Action;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalHandler;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;

public class DimensionsChemicalStorage implements IChemicalHandler {

    private DimensionsNet net;
    private final ArrayList<ChemicalStack> chemicalStorage;

    public DimensionsChemicalStorage() {
        this.chemicalStorage = new ArrayList<>();
    }

    public DimensionsChemicalStorage(DimensionsNet net) {
        this.net = net;
        this.chemicalStorage = new ArrayList<>();
    }

    private void OnChange() {
        net.setDirty();
    }

    // 序列化NBT（根据FluidStorage逻辑添加）
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        ListTag chemicalsTag = new ListTag();

        for (ChemicalStack stack : chemicalStorage) {
            if (!stack.isEmpty()) {
                CompoundTag chemTag = (CompoundTag) stack.save(provider);
                chemicalsTag.add(chemTag);
            }
        }

        tag.put("Chemicals", chemicalsTag);
        return tag;
    }

    // 反序列化NBT（根据FluidStorage逻辑添加）
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        chemicalStorage.clear();
        if (tag.contains("Chemicals", Tag.TAG_LIST)) {
            ListTag chemicalsTag = tag.getList("Chemicals", Tag.TAG_COMPOUND);
            for (int i = 0; i < chemicalsTag.size(); i++) {
                CompoundTag chemTag = chemicalsTag.getCompound(i);
                ChemicalStack stack = ChemicalStack.parseOptional(provider,chemTag); // Mekanism的读取方法
                chemicalStorage.add(stack);
            }
        }
    }

    @Override
    public int getChemicalTanks() {
        return chemicalStorage.size();
    }

    @Override
    public ChemicalStack getChemicalInTank(int tank) {
        if (tank >= 0 && tank < chemicalStorage.size()) {
            return chemicalStorage.get(tank);
        }
        return ChemicalStack.EMPTY;
    }

    @Override
    public void setChemicalInTank(int tank, ChemicalStack stack) {
        if (tank >= 0 && tank < chemicalStorage.size()) {
            chemicalStorage.set(tank, stack.isEmpty() ? ChemicalStack.EMPTY : stack.copy());
            OnChange();
        }
    }

    @Override
    public long getChemicalTankCapacity(int tank) {
        return Long.MAX_VALUE; // 无限容量（模仿FluidStorage的Integer.MAX_VALUE-1）
    }

    @Override
    public boolean isValid(int tank, ChemicalStack stack) {
        return true; // 所有化学品都有效
    }

    // 核心逻辑：合并相同类型的化学品（忽略tank参数，动态处理所有储罐）
    @Override
    public ChemicalStack insertChemical(int tank, ChemicalStack stack, Action action) {
        if (stack.isEmpty()) {
            return stack;
        }

        ChemicalStack resourceToInsert = stack.copy();
        long amountToInsert = resourceToInsert.getAmount();

        // 尝试合并现有储罐
        for (ChemicalStack existing : chemicalStorage) {
            if (existing.getChemical() == resourceToInsert.getChemical()) {
                existing.grow(amountToInsert);
                if (action.execute())
                    OnChange();
                return ChemicalStack.EMPTY; // 容量无限，总是完全插入
            }
        }

        // 新增储罐
        if (action.execute()) {
            chemicalStorage.add(resourceToInsert.copy());
            OnChange();
        }
        return ChemicalStack.EMPTY;
    }

    @Override
    public ChemicalStack extractChemical(int tank, long maxDrain, Action action) {
        if (tank < 0 || tank >= chemicalStorage.size()) {
            return ChemicalStack.EMPTY;
        }

        ChemicalStack existing = chemicalStorage.get(tank);
        if (existing.isEmpty()) {
            return ChemicalStack.EMPTY;
        }

        long drained = Math.min(existing.getAmount(), maxDrain);
        ChemicalStack drainedStack = existing.copy().split(drained);

        if (action.execute()) {
            if (existing.getAmount() == drained) {
                chemicalStorage.set(tank, ChemicalStack.EMPTY);
            } else {
                existing.shrink(drained);
            }
            OnChange();
        }

        return drainedStack;
    }

    @Override
    public ChemicalStack insertChemical(ChemicalStack stack, Action action) {
        if (stack.isEmpty()) return stack;

        // 自动合并同类存储（无视储罐索引）
        for (ChemicalStack existing : chemicalStorage) {
            if (existing.getChemical() ==stack.getChemical()) {
                // 由于容量无限，无条件合并
                long newAmount = existing.getAmount() + stack.getAmount();
                if (action.execute()) {
                    existing.setAmount(newAmount);
                    OnChange();
                }
                return ChemicalStack.EMPTY; // 总能完全吸收
            }
        }

        // 新增化学类型存储
        if (action.execute()) {
            chemicalStorage.add(stack.copy());
            OnChange();
        }
        return ChemicalStack.EMPTY;
    }

    @Override
    public ChemicalStack extractChemical(long amount, Action action) {
        // 优先提取第一个非空储罐
        for (int i = 0; i < chemicalStorage.size(); i++) {
            ChemicalStack existing = chemicalStorage.get(i);
            if (!existing.isEmpty()) {
                long extracted = Math.min(existing.getAmount(), amount);
                ChemicalStack result = existing.copy().split(extracted);

                if (action.execute()) {
                    if (existing.getAmount() == extracted) {
                        chemicalStorage.remove(i);
                    } else {
                        existing.shrink(extracted);
                    }
                    OnChange();
                }
                return result;
            }
        }
        return ChemicalStack.EMPTY;
    }

    @Override
    public ChemicalStack extractChemical(ChemicalStack stack, Action action) {
        if (stack.isEmpty()) return stack;

        // 精确匹配类型和标签
        for (int i = 0; i < chemicalStorage.size(); i++) {
            ChemicalStack existing = chemicalStorage.get(i);
            if (existing.getChemical() ==stack.getChemical()) {
                long extractAmount = Math.min(existing.getAmount(), stack.getAmount());
                ChemicalStack result = existing.copy().split(extractAmount);

                if (action.execute()) {
                    if (existing.getAmount() == extractAmount) {
                        chemicalStorage.remove(i);
                    } else {
                        existing.shrink(extractAmount);
                    }
                    OnChange();
                }
                return result;
            }
        }
        return ChemicalStack.EMPTY;
    }

}
