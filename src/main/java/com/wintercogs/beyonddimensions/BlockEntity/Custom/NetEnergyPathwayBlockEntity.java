package com.wintercogs.beyonddimensions.BlockEntity.Custom;

import com.wintercogs.beyonddimensions.Api.DataBase.DimensionsNet;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.EnergyStackKey;
import com.wintercogs.beyonddimensions.Api.DataBase.Storage.EnergyUnifiedStorageHandler;
import com.wintercogs.beyonddimensions.BlockEntity.ModBlockEntities;
import com.wintercogs.beyonddimensions.Machine.PopMode;
import com.wintercogs.beyonddimensions.Menu.NetEnergyMenu;
import com.wintercogs.beyonddimensions.Util.BDMath;
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
import org.jetbrains.annotations.Nullable;

public class NetEnergyPathwayBlockEntity extends BaseMachineBlockEntity implements MenuProvider
{

    private PopMode popMode = PopMode.STOP;
    private final Direction[] directions = Direction.values();

    public NetEnergyPathwayBlockEntity(BlockPos pos, BlockState blockState)
    {
        super(ModBlockEntities.NET_ENERGY_PATHWAY_BLOCK_ENTITY.get(), pos, blockState);
    }

    //--- 能力注册 (通过事件) ---
    public static void registerCapability(RegisterCapabilitiesEvent event)
    {
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK, // 标准物品能力
                ModBlockEntities.NET_ENERGY_PATHWAY_BLOCK_ENTITY.get(),
                (be, side) -> {
                    if (be.popMode == PopMode.OPEN)
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
                } // 根据方向返回处理器
        );
    }

    public PopMode getPopMode()
    {
        return popMode;
    }

    public void setPopMode(PopMode newMode)
    {
        if (this.popMode != newMode)
        {
            this.popMode = newMode;
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
        if (popMode == PopMode.OPEN)
        {
            popEnergy();
        }
    }

    private void popEnergy()
    {
        DimensionsNet net = getNet();

        if (net == null)
        {
            return; //虽然getNet已经被shouldWork检查过，但是此处仍然进行防御性编程
        }


        for (Direction dir : directions)
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
                    int maxExtract = BDMath.clampLongToInt(net.getUnifiedStorage().getStackByKey(EnergyStackKey.INSTANCE).amount());
                    int receive = otherStorage.receiveEnergy(maxExtract, false);
                    net.getUnifiedStorage().extract(EnergyStackKey.INSTANCE, receive, false, false);
                }
            }
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries)
    {
        super.loadAdditional(tag, registries);

        // 旧数据兼容
        String popModeNew = tag.getString("popMode");
        if (!popModeNew.isEmpty())
        {
            this.popMode = PopMode.valueOf(popModeNew);
        }
        else if (tag.getBoolean("popMode"))
        {
            this.popMode = PopMode.OPEN;
        }
        else
        {
            this.popMode = PopMode.STOP;
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries)
    {
        super.saveAdditional(tag, registries);
        tag.putString("popMode", this.popMode.name());
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
