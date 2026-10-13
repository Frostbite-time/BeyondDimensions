package com.wintercogs.beyonddimensions.common.item;

import com.wintercogs.beyonddimensions.api.machine.MachineItem;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.api.storage.key.impl.ItemStackKey;
import com.wintercogs.beyonddimensions.common.init.BDDataComponents;
import com.wintercogs.beyonddimensions.common.init.BDMachineSettings;
import com.wintercogs.beyonddimensions.common.machine.BaseMachine;
import com.wintercogs.beyonddimensions.common.machine.setting.RedStoneControlMode;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 物品机器：设置按机器类型保存在物品上（{@link MachineItem}），过滤槽等格子存在 ISTACK_SLOTS 组件里
 */
public abstract class BaseMachineItem extends NetedItem implements BaseMachine, MachineItem
{
    private static final String SLOTS = "slots";

    public BaseMachineItem(Properties properties)
    {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected)
    {
        super.inventoryTick(stack, level, entity, slotId, isSelected);

        checkComponents(stack);

        if (level.isClientSide()) return;

        // 同时确保getTicksPerWork为0时可以每tick触发
        if (getTicksPerWork(stack, level, entity, slotId, isSelected) <= 0)
            working(stack, level, entity, slotId, isSelected);
        else if (level.getGameTime() % getTicksPerWork(stack, level, entity, slotId, isSelected) == 0)
            working(stack, level, entity, slotId, isSelected);
    }

    /**
     * 补上缺少的组件；设置不需要补，没有存过的设置取默认值
     */
    public void checkComponents(ItemStack stack)
    {
    }

    /**
     * 格子数为 capacity 的空 ISTACK_SLOTS
     */
    protected static void ensureSlots(ItemStack stack, int capacity)
    {
        if (!stack.has(BDDataComponents.ISTACK_SLOTS))
            stack.set(BDDataComponents.ISTACK_SLOTS, new ArrayList<>(Collections.nCopies(capacity, new KeyAmount(ItemStackKey.EMPTY, 0))));
    }

    /**
     * 配置卡连同格子（过滤槽、模板）一起复制
     */
    @Override
    public void exportConfig(ItemStack stack, CompoundTag config, HolderLookup.Provider registries)
    {
        MachineItem.super.exportConfig(stack, config, registries);
        List<KeyAmount> slots = stack.get(BDDataComponents.ISTACK_SLOTS);
        if (slots != null)
            KeyAmount.CODEC.listOf().encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), slots)
                    .result().ifPresent(tag -> config.put(SLOTS, tag));
    }

    @Override
    public void applyConfig(ItemStack stack, CompoundTag config, HolderLookup.Provider registries)
    {
        MachineItem.super.applyConfig(stack, config, registries);
        if (config.contains(SLOTS))
            KeyAmount.CODEC.listOf().parse(registries.createSerializationContext(NbtOps.INSTANCE), config.get(SLOTS))
                    .result().ifPresent(slots -> stack.set(BDDataComponents.ISTACK_SLOTS, new ArrayList<>(slots)));
    }

    @Override
    public void workStart(ItemStack stack, Level level, Entity holder, int slotId, boolean isSelected)
    {
        BaseMachine.super.workStart(stack, level, holder, slotId, isSelected);

    }

    @Override
    public RedStoneControlMode getControlMode()
    {
        return RedStoneControlMode.IGNORE;
    }

    @Override
    public RedStoneControlMode getControlMode(ItemStack stack)
    {
        return settings(stack).get(BDMachineSettings.WORKING.get());
    }

    @Override
    public boolean hasRedStoneSignal()
    {
        return false;
    }

    @Override
    public int getStepTick()
    {
        return 0;
    }

    @Override
    public void setStepTick(int newTick)
    {

    }
}
