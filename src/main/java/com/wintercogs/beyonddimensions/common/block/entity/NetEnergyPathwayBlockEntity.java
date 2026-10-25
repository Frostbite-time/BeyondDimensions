package com.wintercogs.beyonddimensions.common.block.entity;

import com.wintercogs.beyonddimensions.api.capability.helper.unordered.EnergyUnifiedStorageHandler;
import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet;
import com.wintercogs.beyonddimensions.api.machine.MachineType;
import com.wintercogs.beyonddimensions.api.storage.key.impl.EnergyStackKey;
import com.wintercogs.beyonddimensions.common.init.BDBlockEntities;
import com.wintercogs.beyonddimensions.common.init.BDMachineSettings;
import com.wintercogs.beyonddimensions.common.init.BDMachineTypes;
import com.wintercogs.beyonddimensions.common.machine.MachineSides;
import com.wintercogs.beyonddimensions.common.machine.setting.PopMode;
import com.wintercogs.beyonddimensions.common.machine.setting.SideMode;
import com.wintercogs.beyonddimensions.common.menu.NetEnergyMenu;
import com.wintercogs.beyonddimensions.util.BDMath;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class NetEnergyPathwayBlockEntity extends BaseMachineBlockEntity implements MenuProvider
{
    // 按面缓存的能量能力，面配置、主动弹出或所在网络改变时作废
    private final Map<Direction, LazyOptional<IEnergyStorage>> energyCaps = new HashMap<>();

    private final Direction[] directions = Direction.values();

    public NetEnergyPathwayBlockEntity(BlockPos pos, BlockState blockState)
    {
        super(BDBlockEntities.NET_ENERGY_PATHWAY_BLOCK_ENTITY.get(), pos, blockState);
        addNetChangeTask(this::clearCapCache);
    }

    // 能力注册
    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side)
    {
        if (cap != ForgeCapabilities.ENERGY)
            return super.getCapability(cap, side);
        return energyCaps.computeIfAbsent(side, s -> {
            IEnergyStorage energy = energyFor(s);
            return energy == null ? LazyOptional.empty() : LazyOptional.of(() -> energy);
        }).cast();
    }

    @Override
    public void invalidateCaps()
    {
        super.invalidateCaps();
        clearCapCache();
    }

    // 作废各面的能力，相邻方块随之重新获取
    public void clearCapCache()
    {
        energyCaps.values().forEach(cap -> {
            if (cap.isPresent()) cap.invalidate();
        });
        energyCaps.clear();
    }

    // 按面限制存取，禁用的面没有能力。开启主动弹出后，会被推送的面不再接收：推出去的能量不能从同一面流回网络
    private @Nullable IEnergyStorage energyFor(@Nullable Direction side)
    {
        SideMode mode = MachineSides.mode(this, side);
        if (mode == SideMode.NONE)
            return null;
        DimensionsNet net = getNetId() < 0 ? null : getNet();
        if (net == null)
            return new EnergyStorage(0);
        boolean receives = mode.allowsInput() && !(mode.allowsOutput() && getPopMode() == PopMode.OPEN);
        return new EnergyUnifiedStorageHandler(net.getUnifiedStorage(), receives, mode.allowsOutput());
    }

    @Override
    public MachineType machineType()
    {
        return BDMachineTypes.NET_ENERGY_PATHWAY.get();
    }

    public PopMode getPopMode()
    {
        return setting(BDMachineSettings.POP.get());
    }

    public void setPopMode(PopMode newMode)
    {
        setSetting(BDMachineSettings.POP.get(), newMode);
    }

    // 面配置与主动弹出决定对外暴露的能量能力
    @Override
    protected void onSettingsChanged()
    {
        super.onSettingsChanged();
        clearCapCache();
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
        if (getPopMode() == PopMode.OPEN)
        {
            popEnergy();
        }
    }

    private void popEnergy()
    {
        DimensionsNet net = getNet();

        if (net == null || level == null)
        {
            return;
        }


        for (Direction dir : directions)
        {
            // 只送往允许输出的面
            if (!MachineSides.mode(this, dir).allowsOutput())
                continue;
            BlockPos targetPos = this.getBlockPos().relative(dir);
            BlockEntity neighbor = level.getBlockEntity(targetPos);
            if (neighbor != null && !(neighbor instanceof NetedBlockEntity))
            {
                LazyOptional<IEnergyStorage> otherStorageOptional = neighbor.getCapability(ForgeCapabilities.ENERGY, dir.getOpposite());
                if (otherStorageOptional.isPresent())
                {
                    IEnergyStorage otherStorage = otherStorageOptional.resolve().get();
                    // 先模拟网络能给多少、对方能收多少，再只取出收得下的能量
                    net.getUnifiedStorage().extractInto(EnergyStackKey.INSTANCE, Integer.MAX_VALUE, false,
                            offer -> otherStorage.receiveEnergy(BDMath.clampLongToInt(offer.amount()), true),
                            taken -> taken.amount() - otherStorage.receiveEnergy(BDMath.clampLongToInt(taken.amount()), false));
                }
            }
        }
    }

    @Override
    public void load(CompoundTag tag)
    {
        super.load(tag);
    }

    @Override
    protected void saveAdditional(CompoundTag tag)
    {
        super.saveAdditional(tag);
    }

    @Override
    public @NotNull Component getDisplayName()
    {
        return Component.translatable("menu.title.beyonddimensions.net_energy_menu");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, @NotNull Inventory inventory, Player player)
    {
        return new NetEnergyMenu(containerId, player.getInventory(), this);
    }

}
