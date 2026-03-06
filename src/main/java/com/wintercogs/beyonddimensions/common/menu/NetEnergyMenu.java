package com.wintercogs.beyonddimensions.common.menu;

import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet;
import com.wintercogs.beyonddimensions.api.storage.key.impl.EnergyStackKey;
import com.wintercogs.beyonddimensions.api.dimensionnet.UnifiedStorage;
import com.wintercogs.beyonddimensions.common.block.entity.NetEnergyPathwayBlockEntity;
import com.wintercogs.beyonddimensions.common.machine.PopMode;
import com.wintercogs.beyonddimensions.common.machine.RedStoneControlMode;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.NotNull;

import static com.wintercogs.beyonddimensions.Registry.UIRegister.Net_Energy_Menu;

public class NetEnergyMenu extends BDBaseMenu
{
    public NetEnergyPathwayBlockEntity be;

    public long lastEnergyCapacity = 0;
    public long lastEnergyStored = 0;
    public long lastEnergySpeedState = 0;

    /**
     * 客户端构造函数
     *
     * @param playerInventory 玩家背包
     */
    public NetEnergyMenu(int id, Inventory playerInventory, FriendlyByteBuf data)
    {
        this(id, playerInventory, (NetEnergyPathwayBlockEntity) playerInventory.player.level().getBlockEntity(data.readBlockPos()));
    }

    /**
     * 服务端构造函数
     *
     * @param playerInventory 玩家背包
     */
    public NetEnergyMenu(int id, Inventory playerInventory, NetEnergyPathwayBlockEntity be)
    {
        super(Net_Energy_Menu.get(), id, playerInventory);

        this.be = be;

        inventoryStartIndex = slots.size();
        for (int row = 0; row < 3; ++row)
        {
            for (int col = 0; col < 9; ++col)
            {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 93 + row * 18));
            }
        }

        for (int col = 0; col < 9; ++col)
        {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 151));
        }
        inventoryEndIndex = slots.size();
    }

    @Override
    protected boolean shouldSendQuickData()
    {
        DimensionsNet netCache = be.getNet();
        if (netCache != null)
        {
            UnifiedStorage storage = netCache.getUnifiedStorage();
            if (lastEnergyStored != getEnergyStored(storage)
                    || lastEnergyCapacity != storage.getSlotCapacity(0)
                    || lastEnergySpeedState != getEnergyStored(storage) - lastEnergyStored)
            {
                lastEnergySpeedState = getEnergyStored(storage) - lastEnergyStored;
                lastEnergyStored = getEnergyStored(storage);
                lastEnergyCapacity = storage.getSlotCapacity(0);
                return true;
            }
        }
        else
        {
            if (lastEnergyStored != 0
                    || lastEnergyCapacity != 0
                    || lastEnergySpeedState != 0)
            {
                lastEnergySpeedState = 0;
                lastEnergyStored = 0;
                lastEnergyCapacity = 0;
                return true;
            }
        }
        return false;
    }

    @Override
    protected void writeQuickDataTag(CompoundTag tag)
    {
        super.writeQuickDataTag(tag);
        tag.putString("popMode", be.getPopMode().name());
        tag.putString("controlMode", be.controlMode.name());
        tag.putLong("lastEnergyCapacity", lastEnergyCapacity);
        tag.putLong("lastEnergySpeedState", lastEnergySpeedState);
        tag.putLong("lastEnergyStored", lastEnergyStored);
    }

    @Override
    public void readQuickDataTag(CompoundTag tag)
    {
        super.readQuickDataTag(tag);
        if (player.level().isClientSide())
        {
            this.lastEnergyStored = tag.getLong("lastEnergyStored");
            this.lastEnergyCapacity = tag.getLong("lastEnergyCapacity");
            this.lastEnergySpeedState = tag.getLong("lastEnergySpeedState");
        }
        else
        {
            be.setPopMode(PopMode.valueOf(tag.getString("popMode")));
            be.controlMode = RedStoneControlMode.valueOf(tag.getString("controlMode"));
            player.level().blockEntityChanged(be.getBlockPos());
            be.invalidateCaps();
            player.level().sendBlockUpdated(be.getBlockPos(), be.getBlockState(), be.getBlockState(), 2);
        }
    }

    @Override
    public boolean stillValid(@NotNull Player player)
    {
        return be != null && !be.isRemoved();
    }

    long getEnergyStored(UnifiedStorage storage)
    {
        return storage.getStackByKey(EnergyStackKey.INSTANCE).amount();
    }
}
