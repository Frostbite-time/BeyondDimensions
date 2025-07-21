package com.wintercogs.beyonddimensions.Menu;

import com.wintercogs.beyonddimensions.Api.DataBase.Handler.IStackTypedHandler;
import com.wintercogs.beyonddimensions.Api.DataBase.Handler.StackTypedHandler;
import com.wintercogs.beyonddimensions.BlockEntity.Custom.NetPumpBlockEntity;
import com.wintercogs.beyonddimensions.GUI.CommonTextures;
import com.wintercogs.beyonddimensions.Machine.FilterMode;
import com.wintercogs.beyonddimensions.Machine.RedStoneControlMode;
import com.wintercogs.beyonddimensions.Menu.Slot.FlagStackTypedSlot;
import com.wintercogs.beyonddimensions.Registry.UIRegister;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;

import javax.annotation.Nullable;

public class NetPumpMenu extends BDBaseMenu
{

    private static final int slotStartY = CommonTextures.TOP_BASE_COMMON_HEIGHT + 1;
    private static final int invSlotStartY = CommonTextures.TOP_BASE_COMMON_HEIGHT + CommonTextures.FILTER_SLOTS_HEIGHT*4 + CommonTextures.COMMON_CONNECTION_HEIGHT +7;

    private final IStackTypedHandler storage;

    public final NetPumpBlockEntity be;

    public NetPumpMenu(int id, Inventory playerInventory, FriendlyByteBuf data)
    {
        this(id, playerInventory, new StackTypedHandler(36), (NetPumpBlockEntity) playerInventory.player.level().getBlockEntity(data.readBlockPos()));
    }

    public NetPumpMenu(int containerId, Inventory playerInventory, @Nullable IStackTypedHandler storage,  NetPumpBlockEntity be)
    {
        super(UIRegister.Net_Pump_Menu.get(), containerId, playerInventory);

        this.be = be;

        this.storage = storage;

        addPlayerInv(playerInventory);
        addFlagSlots();

    }

    private void addFlagSlots()
    {
        for(int row = 0; row < 4; row++)
        {
            for (int col = 0; col < 9; col++)
            {
                FlagStackTypedSlot flagSlot = new FlagStackTypedSlot(this, storage, row*9+col, 8 + col * 18, slotStartY + row * 18);
                this.addSlot(flagSlot);
            }
        }
    }

    private void addPlayerInv(Inventory playerInventory)
    {
        // 添加背包以及快捷栏
        inventoryStartIndex = slots.size();
        for (int row = 0; row < 3; ++row)
        {
            for (int col = 0; col < 9; ++col)
            {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, invSlotStartY + row * 18));
            }
        }
        for (int col = 0; col < 9; ++col)
        {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18,  4+invSlotStartY + 3 * 18));
        }
        inventoryEndIndex = slots.size();
    }


    @Override
    public boolean stillValid(Player player)
    {
        return be != null && !be.isRemoved();
    }

    @Override
    protected boolean shouldSendQuickData()
    {
        return false;
    }

    @Override
    protected void writeQuickDataTag(CompoundTag tag)
    {
        super.writeQuickDataTag(tag);
        tag.putString("filter_type",be.filterMode.name());
        tag.putString("control_mode",be.controlMode.name());
    }

    @Override
    public void readQuickDataTag(CompoundTag tag)
    {
        super.readQuickDataTag(tag);
        be.filterMode = FilterMode.valueOf(tag.getString("filter_type"));
        be.controlMode = RedStoneControlMode.valueOf(tag.getString("control_mode"));
        if(!player.level().isClientSide())
        {
            player.level().sendBlockUpdated(be.getBlockPos(),be.getBlockState(),be.getBlockState(),2);
        }
    }
}
