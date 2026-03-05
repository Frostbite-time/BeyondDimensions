package com.wintercogs.beyonddimensions.common.block.entity;

import com.wintercogs.beyonddimensions.api.storage.handler.impl.StackHandler;
import com.wintercogs.beyonddimensions.api.storage.key.impl.FluidStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.IStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.impl.ItemStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.api.dimensionnet.UnifiedStorage;
import com.wintercogs.beyonddimensions.machine.*;
import com.wintercogs.beyonddimensions.common.menu.NetHopperMenu;
import com.wintercogs.beyonddimensions.util.ItemStackHelper;
import com.wintercogs.beyonddimensions.common.init.BDBlockEntities;
import com.wintercogs.beyonddimensions.common.init.BDFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class NetHopperBlockEntity extends BaseMachineBlockEntity implements MenuProvider
{
    private static final int capacity = 36;
    private final StackHandler filterSlots = new StackHandler(capacity)
    {
        @Override
        public void onChange()
        {
            if (level != null && !level.isClientSide())
                level.blockEntityChanged(worldPosition);
        }
    };

    public FilterMode filterMode = FilterMode.BLACK;
    public HopperItemMode hopperItemMode = HopperItemMode.ALLOW;
    public HopperFluidMode hopperFluidMode = HopperFluidMode.DENY;
    public HopperNBTMode hopperNBTMode = HopperNBTMode.DENY;
    public HopperXpMode hopperXpMode = HopperXpMode.DENY;
    public HopperRangeMode hopperRangeMode = HopperRangeMode.RADIUS_MID;

    private List<ItemEntity> itemEntities = new ArrayList<>();
    private List<ExperienceOrb> xpEntities = new ArrayList<>();


    public NetHopperBlockEntity(BlockPos pos, BlockState blockState)
    {
        super(BDBlockEntities.NET_HOPPER_BLOCK_ENTITY.get(), pos, blockState);
    }

    @Override
    public boolean shouldWork()
    {
        return super.shouldWork() && getNet() != null;
    }

    @Override
    public int getTicksPerWork()
    {
        return switch (hopperRangeMode)
        {
            case RADIUS_LOWEST -> 5;
            case RADIUS_LOW -> 10;
            case RADIUS_MID -> 20;
            case RADIUS_HIGH -> 60;
            case RADIUS_HIGHEST -> 100;
            case CHUNK_MODE -> 1200;
        };
    }

    @Override
    public void workStart()
    {
        AABB searchArea = getSearchArea();
        if (hopperItemMode == HopperItemMode.ALLOW)
            refreshItemEntityCache(searchArea);
        if (hopperXpMode == HopperXpMode.ALLOW)
            refreshXpEntityCache(searchArea);
    }

    @Override
    public void workContent()
    {
        UnifiedStorage storage = getNet().getUnifiedStorage(); // getNet已在shouldWork完成null检查

        // 开始收集物品
        if (hopperItemMode == HopperItemMode.ALLOW)
        {
            for (ItemEntity itemEntity : itemEntities)
            {
                if (itemEntity != null && !itemEntity.isRemoved())
                {
                    ItemStack itemStack = itemEntity.getItem();
                    IStackKey<?> itemKey = new ItemStackKey(itemStack);
                    if (matchesFilter(itemKey))
                    {
                        if (storage.insert(itemKey, itemStack.getCount(), true).isEmpty()) // 表示能成功插入
                        {
                            itemEntity.discard();
                            // workContent之前已经由shouldWork检查过net的存在性
                            storage.insert(itemKey, itemStack.getCount(), false);
                        }
                    }
                }
            }
        }

        // 开始收集掉落物
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
                        FluidStackKey xpKey = new FluidStackKey(new FluidStack(BDFluids.XP_FLUID.source(), 1));

                        if (storage.insert(xpKey, xpFluid, true).isEmpty())
                        {
                            orb.discard();
                            storage.insert(xpKey, xpFluid, false);
                        }
                    }
                }
            }
        }

        // 开始抽取流体
        if (hopperFluidMode == HopperFluidMode.ALLOW)
        {
            fluidCollect(getSearchArea());
        }
    }

    private AABB getSearchArea()
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
                    worldPosition.getX() - radius,
                    worldPosition.getY() - radius,
                    worldPosition.getZ() - radius,
                    worldPosition.getX() + radius,
                    worldPosition.getY() + radius,
                    worldPosition.getZ() + radius
            );

        }
        else //全区块收集
        {
            // 获取当前区块坐标
            int chunkX = SectionPos.blockToSectionCoord(worldPosition.getX());
            int chunkZ = SectionPos.blockToSectionCoord(worldPosition.getZ());
            // 获取整个区块区域（16x16）
            int minX = chunkX << 4;       // 区块最小X = 区块坐标 * 16
            int maxX = minX + 15;         // 区块最大X = 最小X + 15
            int minZ = chunkZ << 4;       // 区块最小Z
            int maxZ = minZ + 15;         // 区块最大Z
            // 获取整个世界的Y轴范围
            int minY = level.getMinY();
            int maxY = level.getMaxY();
            // 创建区块边界框
            return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
        }
    }

    // 接收一个搜索范围，更新物品掉落物表单
    // 刷新后itemEntities即为完成了是否接收NBT过滤的物品实体
    private void refreshItemEntityCache(AABB searchArea)
    {
        this.itemEntities = level.getEntitiesOfClass(
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

    private void refreshXpEntityCache(AABB searchArea)
    {
        this.xpEntities = this.level.getEntitiesOfClass(
                ExperienceOrb.class,
                searchArea,
                orb -> true // 不做过滤，拿到区域内所有经验球
        );
    }

    // 收集区域流体
    private void fluidCollect(AABB searchArea)
    {
        if (level == null || level.isClientSide())
        {
            return;
        }


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

                    FluidState fluidState = level.getFluidState(pos);
                    if (fluidState.isEmpty()) continue;

                    // 计算提取量（mB）
                    int amount = fluidState.isSource()
                            ? FluidType.BUCKET_VOLUME
                            : 0;

                    FluidStack extracted = new FluidStack(fluidState.getType(), amount);

                    // 流体收集
                    UnifiedStorage storage = getNet().getUnifiedStorage();
                    FluidStackKey fluidKey = new FluidStackKey(extracted);
                    if (matchesFilter(fluidKey))
                    {
                        if (storage.insert(fluidKey, extracted.getAmount(), true).isEmpty())
                        {
                            storage.insert(fluidKey, extracted.getAmount(), false);
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

    private boolean matchesFilter(IStackKey<?> otherStack)
    {
        switch (filterMode)
        {
            case BLACK ->
            {
                for (KeyAmount stack : filterSlots.getStorage())
                {
                    if (stack.key().isSame(otherStack))
                        return false;
                }
                return true;
            }
            case WHITE ->
            {
                for (KeyAmount stack : filterSlots.getStorage())
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

    @Override
    protected void loadAdditional(@NotNull ValueInput input)
    {
        super.loadAdditional(input);
        filterSlots.deserializeNBT(input.lookup(), input.read("filter_slots", net.minecraft.nbt.CompoundTag.CODEC).orElseGet(net.minecraft.nbt.CompoundTag::new));
        filterMode = FilterMode.valueOf(input.getStringOr("filter_type", FilterMode.BLACK.name()));
        hopperFluidMode = HopperFluidMode.valueOf(input.getStringOr("hopper_fluid_mode", HopperFluidMode.DENY.name()));
        hopperNBTMode = HopperNBTMode.valueOf(input.getStringOr("hopper_nbt_mode", HopperNBTMode.DENY.name()));
        hopperRangeMode = HopperRangeMode.valueOf(input.getStringOr("hopper_range_mode", HopperRangeMode.RADIUS_MID.name()));
        hopperItemMode = HopperItemMode.valueOf(input.getStringOr("hopper_item_model", HopperItemMode.ALLOW.name()));
        hopperXpMode = HopperXpMode.valueOf(input.getStringOr("hopper_xp_mode", HopperXpMode.DENY.name()));
    }

    @Override
    protected void saveAdditional(@NotNull ValueOutput output)
    {
        super.saveAdditional(output);
        output.store("filter_slots", net.minecraft.nbt.CompoundTag.CODEC, filterSlots.serializeNBT(lookupProvider()));
        output.putString("filter_type", filterMode.name());
        output.putString("hopper_fluid_mode", hopperFluidMode.name());
        output.putString("hopper_nbt_mode", hopperNBTMode.name());
        output.putString("hopper_range_mode", hopperRangeMode.name());
        output.putString("hopper_item_model", hopperItemMode.name());
        output.putString("hopper_xp_mode", hopperXpMode.name());
    }

    @Override
    public @NotNull Component getDisplayName()
    {
        return Component.translatable("menu.title.beyonddimensions.hopper_menu");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, @NotNull Inventory inventory, @NotNull Player player)
    {
        return new NetHopperMenu(containerId, inventory, filterSlots, this);
    }
}
