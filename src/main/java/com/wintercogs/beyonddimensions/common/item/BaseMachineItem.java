package com.wintercogs.beyonddimensions.common.item;

import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.common.machine.*;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseMachineItem extends NetedItem implements BaseMachine
{
    public BaseMachineItem(Item.Properties properties)
    {
        super(properties);
    }

    @Override
    public void inventoryTick(@NotNull ItemStack stack, @NotNull Level level, @NotNull Entity entity, int slotId, boolean isSelected)
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

    public void checkComponents(ItemStack stack)
    {
        if (!hasControlMode(stack))
            setControlMode(stack, RedStoneControlMode.IGNORE);
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
        return getControlModeOrDefault(stack, RedStoneControlMode.IGNORE);
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

    // 以下是一些通用辅助方法-作为ItemStack机器控制的快速属性获取，可以随时按模板添加

    // 基本：红石控制模式
    public static RedStoneControlMode getControlModeOrDefault(ItemStack stack, @Nullable RedStoneControlMode defaultValue)
    {
        if (stack.hasTag() && stack.getTag().contains("control_mode"))
        {
            return RedStoneControlMode.valueOf(stack.getTag().getString("control_mode"));
        }
        return defaultValue; //未命中
    }

    public static void setControlMode(ItemStack stack, RedStoneControlMode newMode)
    {
        stack.getOrCreateTag().putString("control_mode", newMode.name());
    }

    // 检查是否有control_mode的tag
    public static boolean hasControlMode(ItemStack stack)
    {
        return stack.hasTag() && stack.getTag().contains("control_mode");
    }

    // 通用：过滤模式
    public static FilterMode getFilterModeOrDefault(ItemStack stack, @Nullable FilterMode defaultValue)
    {
        if (stack.hasTag() && stack.getTag().contains("filter_mode"))
        {
            return FilterMode.valueOf(stack.getTag().getString("filter_mode"));
        }
        return defaultValue; //未命中
    }

    public static void setFilterMode(ItemStack stack, FilterMode newMode)
    {
        stack.getOrCreateTag().putString("filter_mode", newMode.name());
    }

    public static boolean hasFilterMode(ItemStack stack)
    {
        return stack.hasTag() && stack.getTag().contains("filter_mode");
    }

    // 漏斗：是否收集物品
    public static HopperItemMode getHopperItemModeOrDefault(ItemStack stack, @Nullable HopperItemMode defaultValue)
    {
        if (stack.hasTag() && stack.getTag().contains("hopper_item_mode"))
        {
            return HopperItemMode.valueOf(stack.getTag().getString("hopper_item_mode"));
        }
        return defaultValue; //未命中
    }

    public static void setHopperItemMode(ItemStack stack, HopperItemMode newMode)
    {
        stack.getOrCreateTag().putString("hopper_item_mode", newMode.name());
    }

    public static boolean hasHopperItemMode(ItemStack stack)
    {
        return stack.hasTag() && stack.getTag().contains("hopper_item_mode");
    }

    // 漏斗：是否收集经验球
    public static HopperXpMode getHopperXpModeOrDefault(ItemStack stack, @Nullable HopperXpMode defaultValue)
    {
        if (stack.hasTag() && stack.getTag().contains("hopper_xp_mode"))
        {
            return HopperXpMode.valueOf(stack.getTag().getString("hopper_xp_mode"));
        }
        return defaultValue; //未命中
    }

    public static void setHopperXpMode(ItemStack stack, HopperXpMode newMode)
    {
        stack.getOrCreateTag().putString("hopper_xp_mode", newMode.name());
    }

    public static boolean hasHopperXpMode(ItemStack stack)
    {
        return stack.hasTag() && stack.getTag().contains("hopper_xp_mode");
    }

    // 漏斗：是否收集NBT物品
    public static HopperNBTMode getHopperNBTModeOrDefault(ItemStack stack, @Nullable HopperNBTMode defaultValue)
    {
        if (stack.hasTag() && stack.getTag().contains("hopper_nbt_mode"))
        {
            return HopperNBTMode.valueOf(stack.getTag().getString("hopper_nbt_mode"));
        }
        return defaultValue; //未命中
    }

    public static void setHopperNBTMode(ItemStack stack, HopperNBTMode newMode)
    {
        stack.getOrCreateTag().putString("hopper_nbt_mode", newMode.name());
    }

    public static boolean hasHopperNBTMode(ItemStack stack)
    {
        return stack.hasTag() && stack.getTag().contains("hopper_nbt_mode");
    }

    // 漏斗：是否收集流体
    public static HopperFluidMode getHopperFluidModeOrDefault(ItemStack stack, @Nullable HopperFluidMode defaultValue)
    {
        if (stack.hasTag() && stack.getTag().contains("hopper_fluid_mode"))
        {
            return HopperFluidMode.valueOf(stack.getTag().getString("hopper_fluid_mode"));
        }
        return defaultValue; //未命中
    }

    public static void setHopperFluidMode(ItemStack stack, HopperFluidMode newMode)
    {
        stack.getOrCreateTag().putString("hopper_fluid_mode", newMode.name());
    }

    public static boolean hasHopperFluidMode(ItemStack stack)
    {
        return stack.hasTag() && stack.getTag().contains("hopper_fluid_mode");
    }

    // 漏斗：收集范围
    public static HopperRangeMode getHopperRangeModeOrDefault(ItemStack stack, @Nullable HopperRangeMode defaultValue)
    {
        if (stack.hasTag() && stack.getTag().contains("hopper_range_mode"))
        {
            return HopperRangeMode.valueOf(stack.getTag().getString("hopper_range_mode"));
        }
        return defaultValue; //未命中
    }

    public static void setHopperRangeMode(ItemStack stack, HopperRangeMode newMode)
    {
        stack.getOrCreateTag().putString("hopper_range_mode", newMode.name());
    }

    public static boolean hasHopperRangeMode(ItemStack stack)
    {
        return stack.hasTag() && stack.getTag().contains("hopper_range_mode");
    }

    // 喂食：喂食模式
    public static FeederMode getFeederModeOrDefault(ItemStack stack, @Nullable FeederMode defaultValue)
    {
        if (stack.hasTag() && stack.getTag().contains("feeder_mode"))
        {
            return FeederMode.valueOf(stack.getTag().getString("feeder_mode"));
        }
        return defaultValue; //未命中
    }

    public static void setFeederMode(ItemStack stack, FeederMode newMode)
    {
        stack.getOrCreateTag().putString("feeder_mode", newMode.name());
    }

    public static boolean hasFeederMode(ItemStack stack)
    {
        return stack.hasTag() && stack.getTag().contains("feeder_mode");
    }

    // 补货器：模糊模式
    public static FuzzyMode getFuzzyModeOrDefault(ItemStack stack, @Nullable FuzzyMode defaultValue)
    {
        if (stack.hasTag() && stack.getTag().contains("fuzzy_mode"))
        {
            return FuzzyMode.valueOf(stack.getTag().getString("fuzzy_mode"));
        }
        return defaultValue;
    }

    public static void setFuzzyMode(ItemStack stack, FuzzyMode newMode)
    {
        stack.getOrCreateTag().putString("fuzzy_mode", newMode.name());
    }

    public static boolean hasFuzzyMode(ItemStack stack)
    {
        return stack.hasTag() && stack.getTag().contains("fuzzy_mode");
    }

    // 补货器：回收模式
    public static ReceiveMode getReceiveModeOrDefault(ItemStack stack, @Nullable ReceiveMode defaultValue)
    {
        if (stack.hasTag() && stack.getTag().contains("receive_mode"))
        {
            return ReceiveMode.valueOf(stack.getTag().getString("receive_mode"));
        }
        return defaultValue;
    }

    public static void setReceiveMode(ItemStack stack, ReceiveMode newMode)
    {
        stack.getOrCreateTag().putString("receive_mode", newMode.name());
    }

    public static boolean hasReceiveMode(ItemStack stack)
    {
        return stack.hasTag() && stack.getTag().contains("receive_mode");
    }

    // 标记槽位：IStackType的列表
    public static List<KeyAmount> getFilterSlotsOrDefault(ItemStack stack, @Nullable List<KeyAmount> defaultValue)
    {
        if (stack.hasTag() && stack.getTag().contains("filter_slots"))
        {
            ListTag tags = stack.getTag().getList("filter_slots", Tag.TAG_COMPOUND);
            List<KeyAmount> filterSlots = new ArrayList<>();
            for (int i = 0; i < tags.size(); i++)
            {
                filterSlots.add(KeyAmount.deserializeNBT(tags.getCompound(i)));
            }
            return filterSlots;
        }
        return defaultValue;
    }

    public static void setFilterSlots(ItemStack stack, List<KeyAmount> filterSlots)
    {
        ListTag tags = new ListTag();
        for (int i = 0; i < filterSlots.size(); i++)
        {
            tags.add(KeyAmount.serializeNBT(filterSlots.get(i)));
        }
        stack.getOrCreateTag().put("filter_slots", tags);
    }

    public static boolean hasFilterSlots(ItemStack stack)
    {
        return stack.hasTag() && stack.getTag().contains("filter_slots");
    }
}

