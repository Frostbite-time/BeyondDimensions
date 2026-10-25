package com.wintercogs.beyonddimensions.common.item;

import com.wintercogs.beyonddimensions.api.machine.MachineItem;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.api.storage.key.impl.ItemStackKey;
import com.wintercogs.beyonddimensions.common.init.BDMachineSettings;
import com.wintercogs.beyonddimensions.common.machine.BaseMachine;
import com.wintercogs.beyonddimensions.common.machine.setting.RedStoneControlMode;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 物品机器：设置按机器类型保存在物品上（{@link MachineItem}），过滤槽等格子存在物品 NBT 的 filter_slots 下
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
     * 补上缺少的数据；设置不需要补，没有存过的设置取默认值
     */
    public void checkComponents(ItemStack stack)
    {
    }

    /**
     * 格子数为 capacity 的空格子
     */
    protected static void ensureSlots(ItemStack stack, int capacity)
    {
        if (!hasFilterSlots(stack))
            setFilterSlots(stack, new ArrayList<>(Collections.nCopies(capacity, new KeyAmount(ItemStackKey.EMPTY, 0))));
    }

    /**
     * 配置卡连同格子（过滤槽、模板）一起复制
     */
    @Override
    public void exportConfig(ItemStack stack, CompoundTag config)
    {
        MachineItem.super.exportConfig(stack, config);
        List<KeyAmount> slots = getFilterSlotsOrDefault(stack, null);
        if (slots != null)
            config.put(SLOTS, writeSlots(slots));
    }

    @Override
    public void applyConfig(ItemStack stack, CompoundTag config)
    {
        MachineItem.super.applyConfig(stack, config);
        if (config.contains(SLOTS, Tag.TAG_LIST))
            setFilterSlots(stack, readSlots(config.getList(SLOTS, Tag.TAG_COMPOUND)));
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

    // 格子：物品 NBT 中 filter_slots 下的资源列表

    public static List<KeyAmount> getFilterSlotsOrDefault(ItemStack stack, @Nullable List<KeyAmount> defaultValue)
    {
        if (stack.hasTag() && stack.getTag().contains("filter_slots"))
        {
            return readSlots(stack.getTag().getList("filter_slots", Tag.TAG_COMPOUND));
        }
        return defaultValue;
    }

    public static void setFilterSlots(ItemStack stack, List<KeyAmount> filterSlots)
    {
        stack.getOrCreateTag().put("filter_slots", writeSlots(filterSlots));
    }

    public static boolean hasFilterSlots(ItemStack stack)
    {
        return stack.hasTag() && stack.getTag().contains("filter_slots");
    }

    private static List<KeyAmount> readSlots(ListTag tags)
    {
        List<KeyAmount> slots = new ArrayList<>();
        for (int i = 0; i < tags.size(); i++)
        {
            slots.add(KeyAmount.deserializeNBT(tags.getCompound(i)));
        }
        return slots;
    }

    private static ListTag writeSlots(List<KeyAmount> slots)
    {
        ListTag tags = new ListTag();
        for (KeyAmount slot : slots)
        {
            tags.add(KeyAmount.serializeNBT(slot));
        }
        return tags;
    }
}
