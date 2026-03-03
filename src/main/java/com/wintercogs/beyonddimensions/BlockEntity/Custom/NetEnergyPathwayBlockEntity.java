package com.wintercogs.beyonddimensions.BlockEntity.Custom;

import com.wintercogs.beyonddimensions.Api.DataBase.DimensionsNet;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.EnergyStackKey;
import com.wintercogs.beyonddimensions.Api.DataBase.Storage.EnergyUnifiedStorageHandler;
import com.wintercogs.beyonddimensions.Machine.PopMode;
import com.wintercogs.beyonddimensions.Menu.NetEnergyMenu;
import com.wintercogs.beyonddimensions.Util.BDMath;
import com.wintercogs.beyonddimensions.common.init.BDBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.transfer.energy.EmptyEnergyHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NetEnergyPathwayBlockEntity extends BaseMachineBlockEntity implements MenuProvider
{

    private PopMode popMode = PopMode.STOP;
    private final Direction[] directions = Direction.values();

    public NetEnergyPathwayBlockEntity(BlockPos pos, BlockState blockState)
    {
        super(BDBlockEntities.NET_ENERGY_PATHWAY_BLOCK_ENTITY.get(), pos, blockState);
    }

    //--- 能力注册 (通过事件) ---
    public static void registerCapability(RegisterCapabilitiesEvent event)
    {
        event.registerBlockEntity(
                Capabilities.Energy.BLOCK,
                BDBlockEntities.NET_ENERGY_PATHWAY_BLOCK_ENTITY.get(),
                (be, side) -> {
                    if (be.popMode == PopMode.OPEN)
                    {
                        return EmptyEnergyHandler.INSTANCE;
                    }
                    if (be.getNetId() < 0)
                    {
                        return EmptyEnergyHandler.INSTANCE;
                    }
                    DimensionsNet net = be.getNet();
                    if (net != null)
                    {
                        return new EnergyUnifiedStorageHandler(net.getUnifiedStorage());
                    }
                    return EmptyEnergyHandler.INSTANCE;
                }
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

        if (net == null || level == null)
        {
            return; //虽然getNet已经被shouldWork检查过，但是此处仍然进行防御性编程
        }


        for (Direction dir : directions)
        {
            BlockPos targetPos = this.getBlockPos().relative(dir);
            BlockEntity neighbor = level.getBlockEntity(targetPos);
            if (neighbor != null && !(neighbor instanceof NetedBlockEntity))
            {
                EnergyHandler otherStorage = level.getCapability(Capabilities.Energy.BLOCK, targetPos, dir.getOpposite());
                if (otherStorage != null)
                {
                    //getMaxTransfer会返回一个不大于int最大值的long类型数据，因此可以安全转换
                    int maxExtract = BDMath.clampLongToInt(net.getUnifiedStorage().getStackByKey(EnergyStackKey.INSTANCE).amount());
                    if (maxExtract <= 0)
                    {
                        continue;
                    }

                    try (Transaction tx = Transaction.openRoot())
                    {
                        int inserted = otherStorage.insert(maxExtract, tx);
                        if (inserted > 0)
                        {
                            net.getUnifiedStorage().extract(EnergyStackKey.INSTANCE, inserted, false, false);
                            tx.commit();
                        }
                    }
                }
            }
        }
    }

    @Override
    protected void loadAdditional(@NotNull ValueInput input)
    {
        super.loadAdditional(input);

        // 旧数据兼容
        String popModeNew = input.getStringOr("pop_mode", input.getStringOr("popMode", ""));
        if (!popModeNew.isEmpty())
        {
            this.popMode = PopMode.valueOf(popModeNew);
        }
        else if (input.getBooleanOr("popMode", false))
        {
            this.popMode = PopMode.OPEN;
        }
        else
        {
            this.popMode = PopMode.STOP;
        }
    }

    @Override
    protected void saveAdditional(@NotNull ValueOutput output)
    {
        super.saveAdditional(output);
        output.putString("pop_mode", this.popMode.name());
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
