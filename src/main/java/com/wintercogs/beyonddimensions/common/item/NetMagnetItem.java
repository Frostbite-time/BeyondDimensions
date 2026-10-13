package com.wintercogs.beyonddimensions.common.item;

import com.wintercogs.beyonddimensions.api.dimensionnet.UnifiedStorage;
import com.wintercogs.beyonddimensions.api.machine.MachineType;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.api.storage.key.impl.FluidStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.impl.ItemStackKey;
import com.wintercogs.beyonddimensions.common.init.BDDataComponents;
import com.wintercogs.beyonddimensions.common.init.BDFluids;
import com.wintercogs.beyonddimensions.common.init.BDMachineSettings;
import com.wintercogs.beyonddimensions.common.init.BDMachineTypes;
import com.wintercogs.beyonddimensions.common.machine.*;
import com.wintercogs.beyonddimensions.common.menu.NetMagnetMenu;
import com.wintercogs.beyonddimensions.util.ItemStackHelper;
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
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.wrapper.PlayerMainInvWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class NetMagnetItem extends BaseMachineItem
{
    public static final int FILTER_SLOTS = 36;
    public static final int INVENTORY_KEEP_SLOTS = 18;
    public static final int capacity = FILTER_SLOTS + INVENTORY_KEEP_SLOTS;

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
                    Component.translatable("menu.title.beyonddimensions.magnet_menu")));
        }
        return InteractionResultHolder.sidedSuccess(itemstack, level.isClientSide());
    }

    @Override
    public void checkComponents(ItemStack stack)
    {
        super.checkComponents(stack);
        ensureSlots(stack, capacity);
        List<KeyAmount> slots = stack.get(BDDataComponents.ISTACK_SLOTS);
        // 旧磁铁保留原来的 36 个过滤标记，只补上背包保留槽。
        boolean invalidKeepSlot = slots.stream().skip(FILTER_SLOTS).limit(INVENTORY_KEEP_SLOTS)
                .anyMatch(entry -> !entry.key().isEmpty() && !(entry.key() instanceof ItemStackKey));
        if (slots.size() < capacity || invalidKeepSlot)
        {
            List<KeyAmount> expanded = new ArrayList<>(slots);
            while (expanded.size() < capacity)
                expanded.add(new KeyAmount(ItemStackKey.EMPTY, 0));
            for (int i = FILTER_SLOTS; i < capacity; i++)
                if (!(expanded.get(i).key() instanceof ItemStackKey))
                    expanded.set(i, new KeyAmount(ItemStackKey.EMPTY, 0));
            stack.set(BDDataComponents.ISTACK_SLOTS, expanded);
        }
    }

    @Override
    public MachineType machineType()
    {
        return BDMachineTypes.NET_MAGNET.get();
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

        FilterMode filterMode = settings(stack).get(BDMachineSettings.FILTER.get());
        // 收集过滤与背包保留过滤共用这一项
        boolean fuzzy = settings(stack).get(BDMachineSettings.FILTER_FUZZY.get()) == FuzzyMode.ENABLE;
        HopperItemMode hopperItemMode = settings(stack).get(BDMachineSettings.COLLECT_ITEMS.get());
        HopperXpMode hopperXpMode = settings(stack).get(BDMachineSettings.COLLECT_EXPERIENCE.get());
        HopperNBTMode hopperNBTMode = settings(stack).get(BDMachineSettings.COLLECT_COMPONENTS.get());
        HopperFluidMode hopperFluidMode = settings(stack).get(BDMachineSettings.COLLECT_FLUIDS.get());
        HopperRangeMode hopperRangeMode = settings(stack).get(BDMachineSettings.RANGE.get());
        FilterMode inventoryKeepMode = settings(stack).get(BDMachineSettings.INVENTORY_KEEP.get());
        List<KeyAmount> slots = stack.getOrDefault(BDDataComponents.ISTACK_SLOTS, List.of());
        List<KeyAmount> filterSlots = slots.subList(0, Math.min(FILTER_SLOTS, slots.size()));
        List<KeyAmount> inventoryKeepSlots = slots.subList(Math.min(FILTER_SLOTS, slots.size()), Math.min(capacity, slots.size()));

        AABB searchArea = getSearchArea(hopperRangeMode, level, holder.getOnPos());

        List<ItemEntity> itemEntities = hopperItemMode == HopperItemMode.ALLOW ? refreshItemEntityCache(hopperNBTMode, level, searchArea) : new ArrayList<>();
        List<ExperienceOrb> xpEntities = hopperXpMode == HopperXpMode.ALLOW ? refreshXpEntityCache(level, searchArea) : new ArrayList<>();

        UnifiedStorage storage = NetedItem.getNet(stack).getUnifiedStorage();

        // 开始收集物品
        if (hopperItemMode == HopperItemMode.ALLOW)
        {
            PickupTarget target = settings(stack).get(BDMachineSettings.PICKUP_TARGET.get());
            Player player = holder instanceof Player p ? p : null;
            for (ItemEntity itemEntity : itemEntities)
            {
                if (itemEntity == null || itemEntity.isRemoved())
                    continue;
                ItemStackKey itemKey = new ItemStackKey(itemEntity.getItem());
                if (filterMode.passes(filterSlots, itemKey, fuzzy))
                {
                    // 背包保留的物品只进背包，不管去向设成什么
                    boolean inventoryOnly = inventoryKeepMode.passes(inventoryKeepSlots, itemKey, fuzzy);
                    pickUp(itemEntity, storage, player, inventoryOnly ? PickupTarget.INVENTORY_ONLY : target);
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
                        FluidStackKey xpStack = new FluidStackKey(new FluidStack(BDFluids.XP_FLUID.source(), 1));

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
            fluidCollect(filterMode, filterSlots, fuzzy, storage, level, searchArea);
        }
    }

    /**
     * 按去向收起掉落物：只送一处，或者先送一处、放不下的再送另一处，哪里都放不下的留在地上。
     * 只有玩家拿着时才能送进背包
     */
    private static void pickUp(ItemEntity itemEntity, UnifiedStorage storage, @Nullable Player player, PickupTarget target)
    {
        ItemStack original = itemEntity.getItem();
        ItemStack left = original.copy();
        // 背包只收过了拾取冷却的掉落物，刚扔出的东西不会马上飞回背包
        boolean inventoryOpen = player != null && !itemEntity.hasPickUpDelay();
        switch (target)
        {
            case NETWORK_ONLY -> left = intoNetwork(storage, left);
            case NETWORK_FIRST ->
            {
                left = intoNetwork(storage, left);
                if (inventoryOpen)
                    left = intoInventory(player, left);
            }
            case INVENTORY_FIRST ->
            {
                // 刚扔出的东西等拾取冷却过了再收，不然会被送进网络，扔不出去
                if (player != null && !inventoryOpen)
                    return;
                if (inventoryOpen)
                    left = intoInventory(player, left);
                left = intoNetwork(storage, left);
            }
            case INVENTORY_ONLY ->
            {
                if (!inventoryOpen)
                    return;
                left = intoInventory(player, left);
            }
        }

        int taken = original.getCount() - left.getCount();
        if (taken <= 0)
            return;
        ItemStack picked = original.copyWithCount(taken);
        if (left.isEmpty())
            itemEntity.discard();
        else
            itemEntity.setItem(left);
        if (player != null)
        {
            // 与原版捡起物品相同的事件与统计
            EventHooks.fireItemPickupPost(itemEntity, player, picked);
            player.awardStat(Stats.ITEM_PICKED_UP.get(picked.getItem()), taken);
            player.onItemPickup(itemEntity);
        }
    }

    // 放进玩家背包，返回放不下的部分
    private static ItemStack intoInventory(Player player, ItemStack stack)
    {
        if (stack.isEmpty())
            return stack;
        return ItemHandlerHelper.insertItemStacked(new PlayerMainInvWrapper(player.getInventory()), stack, false);
    }

    // 送入网络，返回送不进的部分
    private static ItemStack intoNetwork(UnifiedStorage storage, ItemStack stack)
    {
        if (stack.isEmpty())
            return stack;
        long left = storage.insert(new ItemStackKey(stack), stack.getCount(), false).amount();
        return stack.copyWithCount((int) left);
    }

    @Override
    public int getTicksPerWork(ItemStack stack, Level level, Entity holder, int slotId, boolean isSelected)
    {
        HopperRangeMode hopperRangeMode = settings(stack).get(BDMachineSettings.RANGE.get());
        HopperFluidMode hopperFluidMode = settings(stack).get(BDMachineSettings.COLLECT_FLUIDS.get());
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
    private void fluidCollect(FilterMode filterMode, List<KeyAmount> filterSlots, boolean fuzzy, UnifiedStorage storage, Level level, AABB searchArea)
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
                    if (fluidState.isEmpty()) continue;

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
                    if (filterMode.passes(filterSlots, fluidKey, fuzzy))
                    {
                        if (storage.insert(fluidKey, amount, true).isEmpty())
                        {
                            storage.insert(fluidKey, amount, false);
                            // 清空方块 & 通知客户端
                            BlockState state = level.getBlockState(pos);
                            if (state.getBlock() instanceof BucketPickup pickup && !(state.getBlock() instanceof LiquidBlock))
                            {
                                pickup.pickupBlock(null, level, pos, state);
                            }
                            else
                            {
                                level.setBlock(pos, Blocks.AIR.defaultBlockState(),
                                        Block.UPDATE_ALL_IMMEDIATE);
                            }
                        }
                    }
                }
            }
        }
    }
}
