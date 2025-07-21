package com.wintercogs.beyonddimensions.Menu;

import com.wintercogs.beyonddimensions.Api.DataBase.DimensionsNet;
import com.wintercogs.beyonddimensions.Api.DataBase.Storage.UnifiedStorage;
import com.wintercogs.beyonddimensions.BlockEntity.Custom.NetEnergyPathwayBlockEntity;
import com.wintercogs.beyonddimensions.Machine.PopMode;
import com.wintercogs.beyonddimensions.Machine.RedStoneControlMode;
import com.wintercogs.beyonddimensions.Registry.UIRegister;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;

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
     * @param playerInventory  玩家背包
     */
    public NetEnergyMenu(int id, Inventory playerInventory, NetEnergyPathwayBlockEntity be)
    {
        super(UIRegister.Net_Energy_Menu.get(), id,playerInventory);

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
        if(netCache != null)
        {
            UnifiedStorage storage = netCache.getUnifiedStorage();
            if(lastEnergyStored != storage.getEnergyStored()
                    || lastEnergyCapacity != storage.getSlotCapacity(0)
                    || lastEnergySpeedState != storage.getEnergyStored() - lastEnergyStored)
            {
                lastEnergySpeedState = storage.getEnergyStored() - lastEnergyStored;
                lastEnergyStored = storage.getEnergyStored();
                lastEnergyCapacity = storage.getSlotCapacity(0);
                return true;
            }
        }
        else
        {
            if(lastEnergyStored != 0
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
        tag.putString("popMode", be.popMode.name());
        tag.putString("controlMode", be.controlMode.name());
        tag.putLong("lastEnergyCapacity", lastEnergyCapacity);
        tag.putLong("lastEnergySpeedState", lastEnergySpeedState);
        tag.putLong("lastEnergyStored", lastEnergyStored);
    }

    @Override
    public void readQuickDataTag(CompoundTag tag)
    {
        super.readQuickDataTag(tag);
        if(player.level().isClientSide())
        {
            this.lastEnergyStored = tag.getLong("lastEnergyStored");
            this.lastEnergyCapacity = tag.getLong("lastEnergyCapacity");
            this.lastEnergySpeedState = tag.getLong("lastEnergySpeedState");
        }
        else
        {
            be.popMode = PopMode.valueOf(tag.getString("popMode"));
            be.controlMode = RedStoneControlMode.valueOf(tag.getString("controlMode"));
            player.level().blockEntityChanged(be.getBlockPos());
            be.invalidateCaps();
            player.level().sendBlockUpdated(be.getBlockPos(),be.getBlockState(),be.getBlockState(),2);
        }
    }

    @Override
    public boolean stillValid(Player player)
    {
        return be != null && !be.isRemoved();
    }
}
