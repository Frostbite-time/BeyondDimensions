package com.wintercogs.beyonddimensions.BlockEntity.Custom;

import com.wintercogs.beyonddimensions.Api.DataBase.DimensionsNet;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.EnergyStackType;
import com.wintercogs.beyonddimensions.Api.DataBase.Storage.EnergyUnifiedStorageHandler;
import com.wintercogs.beyonddimensions.BlockEntity.ModBlockEntities;
import com.wintercogs.beyonddimensions.Machine.PopMode;
import com.wintercogs.beyonddimensions.Menu.NetEnergyMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;

import javax.annotation.Nullable;

public class NetEnergyPathwayBlockEntity extends BaseMachineBlockEntity implements MenuProvider
{
    LazyOptional<IEnergyStorage> opt = LazyOptional.empty();

    private PopMode popMode = PopMode.STOP;

    private final Direction[] directions = Direction.values();

    public NetEnergyPathwayBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.NET_ENERGY_PATHWAY_BLOCK_ENTITY.get(), pos, blockState);
        addNetChangeTask(this::clearCapCache);
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap,Direction side)
    {
        if(cap == ForgeCapabilities.ENERGY)
        {
            DimensionsNet net = getNet();
            if (net == null) {
                return LazyOptional.empty();
            }
            if (!opt.isPresent()) {
                if (popMode == PopMode.OPEN) {
                    opt = LazyOptional.of(() -> new EnergyStorage(0));
                } else {
                    opt = LazyOptional.of(() -> new EnergyUnifiedStorageHandler(net.getUnifiedStorage()));
                }
            }
            return opt.cast();
        }
        return super.getCapability(cap,side);
    }

    @Override
    public void invalidateCaps()
    {
        super.invalidateCaps();
        clearCapCache();
    }

    public void clearCapCache()
    {
        if (opt.isPresent()) opt.invalidate();
        opt = LazyOptional.empty();
    }

    public PopMode getPopMode()
    {
        return popMode;
    }

    public void setPopMode(PopMode newMode)
    {
        if (this.popMode != newMode) {
            this.popMode = newMode;
            clearCapCache();
            setChanged();
        }
    }

    @Override
    public boolean shouldWork()
    {
        return super.shouldWork() && getNet() != null;
    }

    @Override
    public int getTicksPerWork()
    {
        return 1;
    }

    @Override
    public void workContent()
    {
        super.workContent();
        if(popMode == PopMode.OPEN)
        {
            popEnergy();
        }
    }

    private void popEnergy()
    {
        DimensionsNet net = getNet();

        if(net==null)
        {
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
    public void load(CompoundTag tag)
    {
        super.load(tag);

        // 旧数据兼容
        String popModeNew = tag.getString("popMode");
        if(!popModeNew.isEmpty())
        {
            this.popMode = PopMode.valueOf(popModeNew);
        }
        else if(tag.getBoolean("popMode"))
        {
            this.popMode = PopMode.OPEN;
        }
        else
        {
            this.popMode = PopMode.STOP;
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag)
    {
        super.saveAdditional(tag);
        tag.putString("popMode",this.popMode.name());
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
