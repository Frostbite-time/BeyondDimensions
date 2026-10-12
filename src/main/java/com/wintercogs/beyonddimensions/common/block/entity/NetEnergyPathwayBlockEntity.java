package com.wintercogs.beyonddimensions.common.block.entity;

import com.wintercogs.beyonddimensions.api.capability.helper.unordered.EnergyUnifiedStorageHandler;
import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet;
import com.wintercogs.beyonddimensions.api.machine.BDMachineSettings;
import com.wintercogs.beyonddimensions.api.machine.MachineType;
import com.wintercogs.beyonddimensions.api.storage.key.impl.EnergyStackKey;
import com.wintercogs.beyonddimensions.common.init.BDBlockEntities;
import com.wintercogs.beyonddimensions.common.init.BDMachineTypes;
import com.wintercogs.beyonddimensions.common.machine.PopMode;
import com.wintercogs.beyonddimensions.common.menu.NetEnergyMenu;
import com.wintercogs.beyonddimensions.util.BDMath;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NetEnergyPathwayBlockEntity extends BaseMachineBlockEntity implements MenuProvider
{

    private final Direction[] directions = Direction.values();

    public NetEnergyPathwayBlockEntity(BlockPos pos, BlockState blockState)
    {
        super(BDBlockEntities.NET_ENERGY_PATHWAY_BLOCK_ENTITY.get(), pos, blockState);
    }

    // 能力注册
    public static void registerCapability(RegisterCapabilitiesEvent event)
    {
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                BDBlockEntities.NET_ENERGY_PATHWAY_BLOCK_ENTITY.get(),
                (be, side) -> {
                    if (be.getPopMode() == PopMode.OPEN)
                    {
                        return new EnergyStorage(0);
                    }
                    if (be.getNetId() < 0)
                    {
                        return new EnergyStorage(0);
                    }
                    DimensionsNet net = be.getNet();
                    if (net != null)
                    {
                        return new EnergyUnifiedStorageHandler(net.getUnifiedStorage());
                    }
                    return new EnergyStorage(0);
                }
        );
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

    // 推送模式决定对外暴露的能量能力
    @Override
    protected void onSettingsChanged()
    {
        super.onSettingsChanged();
        if (level != null)
            level.invalidateCapabilities(worldPosition);
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
            BlockPos targetPos = this.getBlockPos().relative(dir);
            BlockEntity neighbor = level.getBlockEntity(targetPos);
            if (neighbor != null && !(neighbor instanceof NetedBlockEntity))
            {
                IEnergyStorage otherStorage = level.getCapability(Capabilities.EnergyStorage.BLOCK, targetPos, dir.getOpposite());
                if (otherStorage != null)
                {
                    // 先模拟网络能给多少、对方能收多少，再只取出收得下的能量
                    net.getUnifiedStorage().extractInto(EnergyStackKey.INSTANCE, Integer.MAX_VALUE, false,
                            offer -> otherStorage.receiveEnergy(BDMath.clampLongToInt(offer.amount()), true),
                            taken -> taken.amount() - otherStorage.receiveEnergy(BDMath.clampLongToInt(taken.amount()), false));
                }
            }
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries)
    {
        super.loadAdditional(tag, registries);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries)
    {
        super.saveAdditional(tag, registries);
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
