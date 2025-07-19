package com.wintercogs.beyonddimensions.Item.Custom;

import com.wintercogs.beyonddimensions.Api.DataBase.Stack.FluidStackType;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.IStackType;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.ItemStackType;
import com.wintercogs.beyonddimensions.Api.DataBase.Storage.UnifiedStorage;
import com.wintercogs.beyonddimensions.DataComponents.ModDataComponents;
import com.wintercogs.beyonddimensions.Machine.FilterMode;
import com.wintercogs.beyonddimensions.Machine.HopperFluidMode;
import com.wintercogs.beyonddimensions.Machine.HopperNBTMode;
import com.wintercogs.beyonddimensions.Machine.HopperRangeMode;
import com.wintercogs.beyonddimensions.Menu.NetMagnetMenu;
import com.wintercogs.beyonddimensions.Unit.ItemStackHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;

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
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand)
    {
        super.use(level, player, usedHand);
        ItemStack itemstack = player.getItemInHand(usedHand);
        if(usedHand != InteractionHand.MAIN_HAND || player.isShiftKeyDown())
        {
            return InteractionResultHolder.fail(itemstack);
        }

        if(!level.isClientSide())
        {
            player.openMenu(new SimpleMenuProvider((containerId,inv,ServerPlayer) ->
                    new NetMagnetMenu(containerId,inv,itemstack),
                    Component.translatable("menu.title.beyonddimensions.magnet_menu")),
                    buf -> buf.writeEnum(usedHand));
        }
        return InteractionResultHolder.sidedSuccess(itemstack,level.isClientSide());
    }

    @Override
    public void checkComponents(ItemStack stack)
    {
        super.checkComponents(stack);
        if(!stack.has(ModDataComponents.ISTACK_SLOTS))
            stack.set(ModDataComponents.ISTACK_SLOTS,new ArrayList<>(Collections.nCopies(capacity,new ItemStackType())));
        if(!stack.has(ModDataComponents.FILTER_MODE))
            stack.set(ModDataComponents.FILTER_MODE,FilterMode.BLACK);
        if(!stack.has(ModDataComponents.HOPPER_NBT_MODE))
            stack.set(ModDataComponents.HOPPER_NBT_MODE, HopperNBTMode.DENY);
        if(!stack.has(ModDataComponents.HOPPER_FLUID_MODE))
            stack.set(ModDataComponents.HOPPER_FLUID_MODE, HopperFluidMode.DENY);
        if(!stack.has(ModDataComponents.HOPPER_RANGE_MODE))
            stack.set(ModDataComponents.HOPPER_RANGE_MODE, HopperRangeMode.RADIUS_MID);
    }

    @Override
    public boolean shouldWork(ItemStack stack, Level level, Entity holder, int slotId, boolean isSelected)
    {
        return super.shouldWork(stack, level, holder, slotId, isSelected)
                && NetedItem.getNet(stack,level.getServer()) != null;
    }

    @Override
    public void workContent(ItemStack stack, Level level, Entity holder, int slotId, boolean isSelected)
    {
        super.workContent(stack, level, holder, slotId, isSelected);

        FilterMode filterMode = stack.getOrDefault(ModDataComponents.FILTER_MODE,FilterMode.BLACK);
        HopperNBTMode hopperNBTMode = stack.getOrDefault(ModDataComponents.HOPPER_NBT_MODE,HopperNBTMode.DENY);
        HopperFluidMode hopperFluidMode = stack.getOrDefault(ModDataComponents.HOPPER_FLUID_MODE,HopperFluidMode.DENY);
        HopperRangeMode hopperRangeMode = stack.getOrDefault(ModDataComponents.HOPPER_RANGE_MODE,HopperRangeMode.RADIUS_MID);
        List<IStackType> filterSlots = stack.getOrDefault(ModDataComponents.ISTACK_SLOTS,new ArrayList<>());

        AABB searchArea = getSearchArea(hopperRangeMode,level,holder.getOnPos());
        List<ItemEntity> itemEntities = refreshItemEntityCache(hopperNBTMode,level,searchArea);


        UnifiedStorage storage = NetedItem.getNet(stack,level.getServer()).getUnifiedStorage();

        // 开始收集物品
        for(ItemEntity itemEntity : itemEntities)
        {
            if(itemEntity != null && !itemEntity.isRemoved())
            {
                ItemStack itemStack = itemEntity.getItem().copy();
                ItemStackType typedStack = new ItemStackType(itemStack);
                if(matchesFilter(filterMode,filterSlots,typedStack))
                {

                    if(storage.insert(typedStack,true).isEmpty()) // 表示能成功插入
                    {
                        itemEntity.discard();
                        // workContent之前已经由shouldWork检查过net的存在性
                        storage.insert(typedStack,false);
                    }
                }
            }
        }

        // 开始抽取流体
        if(hopperFluidMode == HopperFluidMode.ALLOW)
        {
            fluidCollect(filterMode,filterSlots,storage,level,searchArea);
        }
    }

    @Override
    public int getTicksPerWork(ItemStack stack, Level level, Entity holder, int slotId, boolean isSelected)
    {
        HopperRangeMode hopperRangeMode = stack.getOrDefault(ModDataComponents.HOPPER_RANGE_MODE,HopperRangeMode.RADIUS_MID);

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

    private AABB getSearchArea(HopperRangeMode hopperRangeMode, Level level, Vec3i pos)
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
    private List<ItemEntity> refreshItemEntityCache(HopperNBTMode hopperNBTMode,Level level,AABB searchArea)
    {
        return level.getEntitiesOfClass(
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

    // 收集区域流体
    private void fluidCollect(FilterMode filterMode,List<IStackType> filterSlots,UnifiedStorage storage,Level level,AABB searchArea)
    {

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
                    FluidStackType typedFluid = new FluidStackType(extracted);
                    if(matchesFilter(filterMode,filterSlots,typedFluid))
                    {
                        if(storage.insert(typedFluid,true).isEmpty())
                        {
                            storage.insert(typedFluid,false);
                            // ⑥ 清空方块 & 通知客户端
                            level.setBlock(pos, Blocks.AIR.defaultBlockState(),
                                    Block.UPDATE_ALL_IMMEDIATE);  // 立即更新并刷新渲染
                        }
                    }
                }
            }
        }
    }

    private boolean matchesFilter(FilterMode filterMode,List<IStackType> filterSlots,IStackType otherStack)
    {
        switch (filterMode)
        {

            case BLACK -> {
                for(IStackType stack : filterSlots)
                {
                    if(stack.isSame(otherStack))
                        return false;
                }
                return true;
            }
            case WHITE -> {
                for(IStackType stack : filterSlots)
                {
                    if(stack.isSame(otherStack))
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
}
