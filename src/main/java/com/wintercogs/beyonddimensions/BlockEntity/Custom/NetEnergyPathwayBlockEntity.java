package com.wintercogs.beyonddimensions.BlockEntity.Custom;

import com.wintercogs.beyonddimensions.BlockEntity.ModBlockEntities;
import com.wintercogs.beyonddimensions.DataBase.DimensionsNet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;

public class NetEnergyPathwayBlockEntity extends NetedBlockEntity
{

    public final int transHold = 20;
    public int transTime = 0;

    public boolean popMode = false;

    private final Direction[] directions = Direction.values();
    private com.wintercogs.beyonddimensions.DataBase.Storage.EnergyStorage energyStorage = null; // 仅用于作为缓存，不长期存储


    public NetEnergyPathwayBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.NET_ENERGY_PATHWAY_BLOCK_ENTITY.get(), pos, blockState);
    }

    //--- 能力注册 (通过事件) ---
    public static void registerCapability(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK, // 标准物品能力
                ModBlockEntities.NET_ENERGY_PATHWAY_BLOCK_ENTITY.get(),
                (be, side) -> {
                    if(be.popMode)
                    {
                        return new EnergyStorage(0);
                    }
                    if(be.getNetId()<0)
                    {
                        return new EnergyStorage(0);
                    }
                    DimensionsNet net = be.getNet();
                    if(net != null)
                    {
                        return net.getEnergyStorage();
                    }
                    return new EnergyStorage(0);
                } // 根据方向返回处理器
        );
    }

    // 此方法的签名与 BlockEntityTicker 函数接口的签名匹配.
    public static void tick(Level level, BlockPos pos, BlockState state, NetEnergyPathwayBlockEntity blockEntity) {
        // 你希望在计时期间执行的任何操作.
        // 例如，你可以在这里更改一个制作进度值或消耗能量.
        if(level.isClientSide())
            return; // 客户端不执行任何操作

        if(blockEntity.getNetId() != -1)
        {
            blockEntity.transTime++;
            if(blockEntity.transTime>=blockEntity.transHold)
            {
                blockEntity.transTime = 0;
                // 定时计划写在这里
            }
        }

        // 尝试输出物品到周围
        if(blockEntity.popMode)
        {
            if(!(blockEntity.getNetId()<0))
            {
                blockEntity.popEnergy();
            }
        }
    }

    private void popEnergy()
    {
        if(energyStorage==null)
        {
            energyStorage = getNet().getEnergyStorage();
        }

        for(Direction dir: directions)
        {
            BlockPos targetPos = this.getBlockPos().relative(dir);
            BlockEntity neighbor = level.getBlockEntity(targetPos);
            if (neighbor != null && !(neighbor instanceof NetedBlockEntity))
            {
                // 开始查询能力 记住，你获取你上方的方块，一定是获取其下方的能力
                IEnergyStorage otherStorage = level.getCapability(Capabilities.EnergyStorage.BLOCK, targetPos, dir.getOpposite());
                if (otherStorage != null)
                {
                    //getMaxTransfer会返回一个不大于int最大值的long类型数据，因此可以安全转换
                    int maxExtract = (int)Math.min(energyStorage.getRealEnergyCapacity(), energyStorage.getMaxTransfer());
                    int receive = otherStorage.receiveEnergy(maxExtract, false);
                    energyStorage.extractEnergy(receive, false);
                }
            }
        }
    }

    @Override
    public void invalidateCapabilities()
    {
        super.invalidateCapabilities();
        energyStorage = null;
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries)
    {
        super.loadAdditional(tag,registries);
        this.popMode = tag.getBoolean("popMode");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries)
    {
        super.saveAdditional(tag, registries);
        tag.putBoolean("popMode",this.popMode);
    }

}
