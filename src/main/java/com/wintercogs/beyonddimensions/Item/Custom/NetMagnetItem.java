package com.wintercogs.beyonddimensions.Item.Custom;

import com.wintercogs.beyonddimensions.Api.DataBase.Stack.FluidStackKey;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.IStackKey;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.ItemStackKey;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.KeyAmount;
import com.wintercogs.beyonddimensions.Api.DataBase.Storage.UnifiedStorage;
import com.wintercogs.beyonddimensions.DataComponents.ModDataComponents;
import com.wintercogs.beyonddimensions.Fluid.ModFluids;
import com.wintercogs.beyonddimensions.Machine.*;
import com.wintercogs.beyonddimensions.Menu.NetMagnetMenu;
import com.wintercogs.beyonddimensions.Util.ItemStackHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NetMagnetItem extends BaseMachineItem
{
    public static final int capacity = 36;

    public NetMagnetItem(Properties properties)
    {
        super(properties.stacksTo(1));
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand)
    {
        super.use(level, player, usedHand);
        ItemStack itemstack = player.getItemInHand(usedHand);
        if (usedHand != InteractionHand.MAIN_HAND || player.isShiftKeyDown())
        {
            return InteractionResultHolder.fail(itemstack);
        }

        if (!level.isClientSide())
        {
            player.openMenu(new SimpleMenuProvider((containerId, inv, ServerPlayer) ->
                            new NetMagnetMenu(containerId, inv, itemstack),
                            Component.translatable("menu.title.beyonddimensions.magnet_menu")),
                    buf -> buf.writeEnum(usedHand));
        }
        return InteractionResultHolder.sidedSuccess(itemstack, level.isClientSide());
    }

    @Override
    public void checkComponents(ItemStack stack)
    {
        super.checkComponents(stack);
        if (!stack.has(ModDataComponents.ISTACK_SLOTS))
            stack.set(ModDataComponents.ISTACK_SLOTS, new ArrayList<>(Collections.nCopies(capacity, new KeyAmount(ItemStackKey.EMPTY, 0))));
        if (!stack.has(ModDataComponents.FILTER_MODE))
            stack.set(ModDataComponents.FILTER_MODE, FilterMode.BLACK);
        if (!stack.has(ModDataComponents.HOPPER_ITEM_MODE))
            stack.set(ModDataComponents.HOPPER_ITEM_MODE, HopperItemMode.ALLOW);
        if (!stack.has(ModDataComponents.HOPPER_XP_MODE))
            stack.set(ModDataComponents.HOPPER_XP_MODE, HopperXpMode.DENY);
        if (!stack.has(ModDataComponents.HOPPER_NBT_MODE))
            stack.set(ModDataComponents.HOPPER_NBT_MODE, HopperNBTMode.DENY);
        if (!stack.has(ModDataComponents.HOPPER_FLUID_MODE))
            stack.set(ModDataComponents.HOPPER_FLUID_MODE, HopperFluidMode.DENY);
        if (!stack.has(ModDataComponents.HOPPER_RANGE_MODE))
            stack.set(ModDataComponents.HOPPER_RANGE_MODE, HopperRangeMode.RADIUS_MID);
    }

    @Override
    public boolean shouldWork(ItemStack stack, Level level, Entity holder, int slotId, boolean isSelected)
    {
        return super.shouldWork(stack, level, holder, slotId, isSelected)
                && NetedItem.getNet(stack) != null;
    }

    @Override
    public void workContent(ItemStack stack, Level level, Entity holder, int slotId, boolean isSelected)
    {
        super.workContent(stack, level, holder, slotId, isSelected);

        FilterMode filterMode = stack.getOrDefault(ModDataComponents.FILTER_MODE, FilterMode.BLACK);
        HopperItemMode hopperItemMode = stack.getOrDefault(ModDataComponents.HOPPER_ITEM_MODE, HopperItemMode.ALLOW);
        HopperXpMode hopperXpMode = stack.getOrDefault(ModDataComponents.HOPPER_XP_MODE, HopperXpMode.DENY);
        HopperNBTMode hopperNBTMode = stack.getOrDefault(ModDataComponents.HOPPER_NBT_MODE, HopperNBTMode.DENY);
        HopperFluidMode hopperFluidMode = stack.getOrDefault(ModDataComponents.HOPPER_FLUID_MODE, HopperFluidMode.DENY);
        HopperRangeMode hopperRangeMode = stack.getOrDefault(ModDataComponents.HOPPER_RANGE_MODE, HopperRangeMode.RADIUS_MID);
        List<KeyAmount> filterSlots = stack.getOrDefault(ModDataComponents.ISTACK_SLOTS, new ArrayList<>());

        AABB searchArea = getSearchArea(hopperRangeMode, level, holder.getOnPos());

        List<ItemEntity> itemEntities = hopperItemMode == HopperItemMode.ALLOW ? refreshItemEntityCache(hopperNBTMode, level, searchArea) : new ArrayList<>();
        List<ExperienceOrb> xpEntities = hopperXpMode == HopperXpMode.ALLOW ? refreshXpEntityCache(level, searchArea) : new ArrayList<>();

        UnifiedStorage storage = NetedItem.getNet(stack).getUnifiedStorage();

        // 开始收集物品
        if (hopperItemMode == HopperItemMode.ALLOW)
        {
            for (ItemEntity itemEntity : itemEntities)
            {
                if (itemEntity != null && !itemEntity.isRemoved())
                {
                    ItemStack itemStack = itemEntity.getItem();
                    ItemStackKey itemKey = new ItemStackKey(itemStack);
                    if (matchesFilter(filterMode, filterSlots, itemKey))
                    {
                        int count = itemStack.getCount();

                        if (storage.insert(itemKey, count, true).isEmpty())
                        {
                            if (holder instanceof Player player)
                            {
                                ItemStack originalCopy = itemStack.copy();
                                // 发送事件
                                itemStack.setCount(0);
                                EventHooks.fireItemPickupPost(itemEntity, player, originalCopy);
                                // 成就信息
                                itemStack.setCount(count);
                                player.awardStat(Stats.ITEM_PICKED_UP.get(originalCopy.getItem()), count);
                                player.onItemPickup(itemEntity);
                            }

                            itemEntity.discard();
                            storage.insert(itemKey, count, false);
                        }
                    }
                }
            }
        }
        // 开始收集经验球
        if (hopperXpMode == HopperXpMode.ALLOW)
        {
            for (ExperienceOrb orb : xpEntities)
            {
                if (orb != null && !orb.isRemoved())
                {
                    int xp = orb.getValue();
                    if (xp > 0)
                    {
                        long xpFluid = xp * 20L;
                        FluidStackKey xpStack = new FluidStackKey(new FluidStack(ModFluids.XP_FLUID.source(), 1));

                        if (storage.insert(xpStack, xpFluid, true).isEmpty())
                        {
                            orb.discard();
                            storage.insert(xpStack, xpFluid, false);
                        }
                    }
                }
            }
        }
        // 开始抽取流体
        if (hopperFluidMode == HopperFluidMode.ALLOW)
        {
            fluidCollect(filterMode, filterSlots, storage, level, searchArea);
        }
    }

    @Override
    public int getTicksPerWork(ItemStack stack, Level level, Entity holder, int slotId, boolean isSelected)
    {
        HopperRangeMode hopperRangeMode = stack.getOrDefault(ModDataComponents.HOPPER_RANGE_MODE, HopperRangeMode.RADIUS_MID);
        HopperFluidMode hopperFluidMode = stack.getOrDefault(ModDataComponents.HOPPER_FLUID_MODE, HopperFluidMode.DENY);
        if (hopperFluidMode == HopperFluidMode.ALLOW)
        {
            return switch (hopperRangeMode)
            {
                case RADIUS_LOWEST -> 0;
                case RADIUS_LOW -> 0;
                case RADIUS_MID -> 10;
                case RADIUS_HIGH -> 20;
                case RADIUS_HIGHEST -> 50;
                case CHUNK_MODE -> 1200;
            };
        }
        else
        {
            return switch (hopperRangeMode)
            {
                case RADIUS_LOWEST -> 0;
                case RADIUS_LOW -> 0;
                case RADIUS_MID -> 2;
                case RADIUS_HIGH -> 5;
                case RADIUS_HIGHEST -> 10;
                case CHUNK_MODE -> 1200;
            };
        }
    }

    private AABB getSearchArea(HopperRangeMode hopperRangeMode, Level level, Vec3i pos)
    {
        if (hopperRangeMode != HopperRangeMode.CHUNK_MODE)
        {
            //更正半径
            int radius = switch (hopperRangeMode)
            {
                case RADIUS_LOWEST -> 2;
                case RADIUS_LOW -> 3;
                case RADIUS_MID -> 5;
                case RADIUS_HIGH -> 7;
                case RADIUS_HIGHEST -> 10;
                default -> 1;
            };
            // 计算搜索范围（AABB轴对齐边界框）
            return new AABB(
                    pos.getX() - radius,
                    pos.getY() - radius,
                    pos.getZ() - radius,
                    pos.getX() + radius,
                    pos.getY() + radius,
                    pos.getZ() + radius
            );

        }
        else //全区块收集
        {
            // 获取当前区块坐标
            int chunkX = SectionPos.blockToSectionCoord(pos.getX());
            int chunkZ = SectionPos.blockToSectionCoord(pos.getZ());
            // 获取整个区块区域（16x16）
            int minX = chunkX << 4;       // 区块最小X = 区块坐标 * 16
            int maxX = minX + 15;         // 区块最大X = 最小X + 15
            int minZ = chunkZ << 4;       // 区块最小Z
            int maxZ = minZ + 15;         // 区块最大Z
            // 获取整个世界的Y轴范围
            int minY = level.getMinBuildHeight();
            int maxY = level.getMaxBuildHeight();
            // 创建区块边界框
            return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
        }
    }

    // 接收一个搜索范围，更新物品掉落物表单
    // 刷新后itemEntities即为完成了是否接收NBT过滤的物品实体
    private List<ItemEntity> refreshItemEntityCache(HopperNBTMode hopperNBTMode, Level level, AABB searchArea)
    {
        return level.getEntitiesOfClass(
                ItemEntity.class,
                searchArea,
                itemEntity -> {
                    // NBT过滤
                    if (hopperNBTMode == HopperNBTMode.DENY)
                    {
                        return !ItemStackHelper.hasExtraComponents(itemEntity.getItem());
                    }
                    else
                    {
                        return true;
                    }
                }
        );
    }

    private List<ExperienceOrb> refreshXpEntityCache(Level level, AABB searchArea)
    {
        return level.getEntitiesOfClass(
                ExperienceOrb.class,
                searchArea,
                orb -> true // 不做过滤，拿到区域内所有经验球
        );
    }

    // 收集区域流体
    private void fluidCollect(FilterMode filterMode, List<KeyAmount> filterSlots, UnifiedStorage storage, Level level, AABB searchArea)
    {

        int minX = Mth.floor(searchArea.minX);
        int minY = Mth.floor(searchArea.minY);
        int minZ = Mth.floor(searchArea.minZ);
        int maxX = Mth.floor(searchArea.maxX);
        int maxY = Mth.floor(searchArea.maxY);
        int maxZ = Mth.floor(searchArea.maxZ);

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int x = minX; x <= maxX; ++x)
        {
            for (int y = minY; y <= maxY; ++y)
            {
                for (int z = minZ; z <= maxZ; ++z)
                {
                    pos.set(x, y, z);

                    // 使用原始数据计算提取量
                    FluidState fluidState = level.getFluidState(pos);
                    if (fluidState.isEmpty()) continue;          // 不是流体

                    // 计算提取量（mB）
                    int amount = fluidState.isSource()
                            ? FluidType.BUCKET_VOLUME
                            : 0;

                    // 进行实际交互前，将流体统一归为源+1mb副本
                    Fluid stillFluid = fluidState.getType();
                    if (stillFluid instanceof FlowingFluid ff)
                        stillFluid = ff.getSource();
                    FluidStack extracted = new FluidStack(stillFluid, 1);

                    // 进行存储交互
                    FluidStackKey fluidKey = new FluidStackKey(extracted);
                    if (matchesFilter(filterMode, filterSlots, fluidKey))
                    {
                        if (storage.insert(fluidKey, amount, true).isEmpty())
                        {
                            storage.insert(fluidKey, amount, false);
                            // 清空方块 & 通知客户端
                            level.setBlock(pos, Blocks.AIR.defaultBlockState(),
                                    Block.UPDATE_ALL_IMMEDIATE);  // 立即更新并刷新渲染
                        }
                    }
                }
            }
        }
    }

    private boolean matchesFilter(FilterMode filterMode, List<KeyAmount> filterSlots, IStackKey<?> otherStack)
    {
        switch (filterMode)
        {

            case BLACK ->
            {
                for (KeyAmount stack : filterSlots)
                {
                    if (stack.key().isSame(otherStack))
                        return false;
                }
                return true;
            }
            case WHITE ->
            {
                for (KeyAmount stack : filterSlots)
                {
                    if (stack.key().isSame(otherStack))
                        return true;
                }
                return false;
            }
            case IGNORE ->
            {
                return true;
            }

        }
        return false;
    }
}
