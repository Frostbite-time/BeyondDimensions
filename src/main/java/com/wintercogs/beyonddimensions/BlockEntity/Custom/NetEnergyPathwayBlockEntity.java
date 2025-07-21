package com.wintercogs.beyonddimensions.BlockEntity.Custom;

import com.wintercogs.beyonddimensions.Api.DataBase.DimensionsNet;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.EnergyStackType;
import com.wintercogs.beyonddimensions.Api.DataBase.Storage.EnergyUnifiedStorageHandler;
import com.wintercogs.beyonddimensions.BlockEntity.ModBlockEntities;
import com.wintercogs.beyonddimensions.Menu.NetEnergyMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;

import javax.annotation.Nullable;

public class NetEnergyPathwayBlockEntity extends NetedBlockEntity implements MenuProvider
{

    public final int transHold = 20;
    public int transTime = 0;

    public boolean popMode = false;

    private final Direction[] directions = Direction.values();

    private DimensionsNet net = null; //用于缓存


    public NetEnergyPathwayBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.NET_ENERGY_PATHWAY_BLOCK_ENTITY.get(), pos, blockState);
    }


    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap,Direction side)
    {
        if(cap == ForgeCapabilities.ENERGY)
        {
            if(this.getNetId()>=0)
            {
                DimensionsNet net = getNet();
                if(net != null)
                {
                    return LazyOptional.of(() -> new EnergyUnifiedStorageHandler(net.getUnifiedStorage())).cast();
                }
            }
        }
        return super.getCapability(cap,side);
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
        if(net==null)
        {
            DimensionsNet net = getNet();
            if(net != null)
                this.net = net;
            else
                return;
        }

        for(Direction dir: directions)
        {
            BlockPos targetPos = this.getBlockPos().relative(dir);
            BlockEntity neighbor = level.getBlockEntity(targetPos);
            if (neighbor != null && !(neighbor instanceof NetedBlockEntity))
            {
                // 开始查询能力 记住，你获取你上方的方块，一定是获取其下方的能力
                LazyOptional<IEnergyStorage> otherStorageOptional = neighbor.getCapability(ForgeCapabilities.ENERGY, dir.getOpposite());
                if (otherStorageOptional.isPresent())
                {
                    IEnergyStorage otherStorage = otherStorageOptional.resolve().get();
                    //getMaxTransfer会返回一个不大于int最大值的long类型数据，因此可以安全转换
                    int maxExtract = (int)Math.min(net.getUnifiedStorage().getEnergyStored(), Integer.MAX_VALUE);
                    int receive = otherStorage.receiveEnergy(maxExtract, false);
                    net.getUnifiedStorage().extract(new EnergyStackType(receive),false);
                }
            }
        }
    }

    @Override
    public void invalidateCaps()
    {
        super.invalidateCaps();
        net = null;
    }

    @Override
    public void load(CompoundTag tag)
    {
        super.load(tag);
        this.popMode = tag.getBoolean("popMode");
    }

    @Override
    protected void saveAdditional(CompoundTag tag)
    {
        super.saveAdditional(tag);
        tag.putBoolean("popMode",this.popMode);
    }

    @Override
    public Component getDisplayName()
    {
        return Component.translatable("menu.title.beyonddimensions.net_energy_menu");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player)
    {
        return new NetEnergyMenu(containerId, player.getInventory(), this);
    }

}
