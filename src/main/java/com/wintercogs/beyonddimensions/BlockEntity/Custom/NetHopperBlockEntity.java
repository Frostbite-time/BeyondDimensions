package com.wintercogs.beyonddimensions.BlockEntity.Custom;

import com.wintercogs.beyonddimensions.Api.DataBase.Handler.StackHandler;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.FluidStackKey;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.IStackKey;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.ItemStackKey;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.KeyAmount;
import com.wintercogs.beyonddimensions.Api.DataBase.Storage.UnifiedStorage;
import com.wintercogs.beyonddimensions.BlockEntity.ModBlockEntities;
import com.wintercogs.beyonddimensions.Fluid.ModFluids;
import com.wintercogs.beyonddimensions.Machine.*;
import com.wintercogs.beyonddimensions.Menu.NetHopperMenu;
import com.wintercogs.beyonddimensions.Unit.ItemStackHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
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
            if(level != null && !level.isClientSide())
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
        super(ModBlockEntities.NET_HOPPER_BLOCK_ENTITY.get(), pos, blockState);
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
        if(hopperItemMode == HopperItemMode.ALLOW)
            refreshItemEntityCache(searchArea);
        if(hopperXpMode == HopperXpMode.ALLOW)
            refreshXpEntityCache(searchArea);
    }

    @Override
    public void workContent()
    {
        UnifiedStorage storage = getNet().getUnifiedStorage(); // getNet已在shouldWork完成null检查

        // 开始收集物品
        if(hopperItemMode == HopperItemMode.ALLOW)
        {
            for(ItemEntity itemEntity : itemEntities)
            {
                if(itemEntity != null && !itemEntity.isRemoved())
                {
                    ItemStack itemStack = itemEntity.getItem();
                    IStackKey<?> itemKey = new ItemStackKey(itemStack);
                    if(matchesFilter(itemKey))
                    {
                        if(storage.insert(itemKey,itemStack.getCount(),true).isEmpty()) // 表示能成功插入
                        {
                            itemEntity.discard();
                            // workContent之前已经由shouldWork检查过net的存在性
                            storage.insert(itemKey,itemStack.getCount(),false);
                        }
                    }
                }
            }
        }

        // 开始收集掉落物
        if(hopperXpMode == HopperXpMode.ALLOW)
        {
            for(ExperienceOrb orb : xpEntities)
            {
                if(orb != null && !orb.isRemoved())
                {
                    int xp = orb.getValue();
                    if(xp > 0 )
                    {
                        long xpFluid = xp * 20L;
                        FluidStackKey xpKey = new FluidStackKey(new FluidStack(ModFluids.XP_FLUID.source(),1));

                        if(storage.insert(xpKey,xpFluid,true).isEmpty())
                        {
                            orb.discard();
                            storage.insert(xpKey,xpFluid,false);
                        }
                    }
                }
            }
        }

        // 开始抽取流体
        if(hopperFluidMode == HopperFluidMode.ALLOW)
        {
            fluidCollect(getSearchArea());
        }
    }

    private AABB getSearchArea()
    {
        if(hopperRangeMode != HopperRangeMode.CHUNK_MODE)
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
            int minY = level.getMinBuildHeight();
            int maxY = level.getMaxBuildHeight();
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
                    if(hopperNBTMode == HopperNBTMode.DENY)
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
        // ① 安全性检查
        if (level == null || level.isClientSide) {
            return;                       // 只在服务器端执行
        }


        int minX = Mth.floor(searchArea.minX);
        int minY = Mth.floor(searchArea.minY);
        int minZ = Mth.floor(searchArea.minZ);
        int maxX = Mth.floor(searchArea.maxX);
        int maxY = Mth.floor(searchArea.maxY);
        int maxZ = Mth.floor(searchArea.maxZ);

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int x = minX; x <= maxX; ++x) {
            for (int y = minY; y <= maxY; ++y) {
                for (int z = minZ; z <= maxZ; ++z) {
                    pos.set(x, y, z);

                    FluidState fluidState = level.getFluidState(pos);
                    if (fluidState.isEmpty()) continue;          // 不是流体

                    // ④ 计算提取量（mB）
                    int amount = fluidState.isSource()
                            ? FluidType.BUCKET_VOLUME
                            : 0;

                    FluidStack extracted = new FluidStack(fluidState.getType(), amount);

                    // ⑤ 交给你的逻辑（存槽、推网络、合并等）
                    UnifiedStorage storage = getNet().getUnifiedStorage();
                    FluidStackKey fluidKey = new FluidStackKey(extracted);
                    if(matchesFilter(fluidKey))
                    {
                        if(storage.insert(fluidKey,extracted.getAmount(),true).isEmpty())
                        {
                            storage.insert(fluidKey,extracted.getAmount(),false);
                            // ⑥ 清空方块 & 通知客户端
                            level.setBlock(pos, Blocks.AIR.defaultBlockState(),
                                    Block.UPDATE_ALL_IMMEDIATE);  // 立即更新并刷新渲染
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
            case BLACK -> {
                for(KeyAmount stack : filterSlots.getStorage())
                {
                    if(stack.key().isSame(otherStack))
                        return false;
                }
                return true;
            }
            case WHITE -> {
                for(KeyAmount stack : filterSlots.getStorage())
                {
                    if(stack.key().isSame(otherStack))
                        return true;
                }
                return false;
            }
            case IGNORE -> {
                return true;
            }

        }
        return false;
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries)
    {
        super.loadAdditional(tag, registries);
        filterSlots.deserializeNBT(registries, tag.getCompound("filter_slots"));
        filterMode = FilterMode.valueOf(tag.getString("filter_type"));
        hopperFluidMode = HopperFluidMode.valueOf(tag.getString("hopper_fluid_mode"));
        hopperNBTMode = HopperNBTMode.valueOf(tag.getString("hopper_nbt_mode"));
        hopperRangeMode = HopperRangeMode.valueOf(tag.getString("hopper_range_mode"));
        hopperItemMode = tag.contains("hopper_item_model") ? HopperItemMode.valueOf(tag.getString("hopper_item_model")) : null;
        hopperXpMode = tag.contains("hopper_xp_mode") ? HopperXpMode.valueOf(tag.getString("hopper_xp_mode")) : null;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries)
    {
        super.saveAdditional(tag, registries);
        tag.put("filter_slots", filterSlots.serializeNBT(registries));
        tag.putString("filter_type", filterMode.name());
        tag.putString("hopper_fluid_mode", hopperFluidMode.name());
        tag.putString("hopper_nbt_mode", hopperNBTMode.name());
        tag.putString("hopper_range_mode", hopperRangeMode.name());
        tag.putString("hopper_item_model", hopperItemMode.name());
        tag.putString("hopper_xp_mode", hopperXpMode.name());
    }

    @Override
    public Component getDisplayName()
    {
        return Component.translatable("menu.title.beyonddimensions.hopper_menu");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player)
    {
        return new NetHopperMenu(containerId,inventory, filterSlots, this);
    }
}
