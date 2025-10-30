package com.wintercogs.beyonddimensions.Menu;

import com.wintercogs.beyonddimensions.Api.DataBase.Handler.IStackTypedHandler;
import com.wintercogs.beyonddimensions.Api.DataBase.Handler.StackTypedHandler;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.IStackType;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.ItemStackType;
import com.wintercogs.beyonddimensions.GUI.CommonTextures;
import com.wintercogs.beyonddimensions.Item.Custom.BaseMachineItem;
import com.wintercogs.beyonddimensions.Machine.FeederMode;
import com.wintercogs.beyonddimensions.Machine.RedStoneControlMode;
import com.wintercogs.beyonddimensions.Menu.Slot.FlagStackTypedSlot;
import com.wintercogs.beyonddimensions.Registry.UIRegister;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class NetFeederMenu extends BDBaseMenu
{


    private static final int slotStartY = CommonTextures.TOP_BASE_COMMON_HEIGHT + 1;
    private static final int invSlotStartY = CommonTextures.TOP_BASE_COMMON_HEIGHT + CommonTextures.FILTER_SLOTS_HEIGHT * 4 + CommonTextures.COMMON_CONNECTION_HEIGHT + 7;

    // storage的初始数据由itemStack提供，随后storage每次变化都重新向其中写入数据
    private final IStackTypedHandler storage = new StackTypedHandler(36)
    {
        @Override
        public void onChange()
        {
            super.onChange();
            if (!player.level().isClientSide() && initialized)
                BaseMachineItem.setFilterSlots(menuStack, new ArrayList<>(storage.getStorage()));

        }

        @Override
        public boolean isStackValid(int slot, IStackType stack)
        {
            return super.isStackValid(slot, stack)
                    && stack instanceof ItemStackType itemStackType
                    && itemStackType.getStack().getFoodProperties(player) != null;
        }
    };
    private boolean initialized; //initialized必须在初始数据提供完成之后才能设置为true

    public final ItemStack menuStack;

    private RedStoneControlMode lastControlMode;
    private FeederMode lastFeederMode;


    public NetFeederMenu(int id, Inventory playerInventory, FriendlyByteBuf data)
    {
        this(id, playerInventory, playerInventory.player.getItemInHand(data.readEnum(InteractionHand.class)));
    }

    public NetFeederMenu(int containerId, Inventory playerInventory, ItemStack menuStack)
    {
        super(UIRegister.Net_Feeder_Menu.get(), containerId, playerInventory);
        this.menuStack = menuStack;

        initialized = false;
        // 为服务端注入真实数据，客户端由槽位同步
        if (!playerInventory.player.level().isClientSide())
        {
            List<IStackType<?>> stacks = BaseMachineItem.getFilterSlotsOrDefault(menuStack, new ArrayList<>());
            for (int i = 0; i < stacks.size(); i++)
            {
                storage.insert(i, stacks.get(i).copy(), false);
            }
        }
        initialized = true;


        addPlayerInv(playerInventory);
        addFlagSlots();

    }

    private void addFlagSlots()
    {
        for (int row = 0; row < 4; row++)
        {
            for (int col = 0; col < 9; col++)
            {
                FlagStackTypedSlot flagSlot = new FlagStackTypedSlot(this, storage, row * 9 + col, 8 + col * 18, slotStartY + row * 18);
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
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 4 + invSlotStartY + 3 * 18));
        }
        inventoryEndIndex = slots.size();
    }

    @Override
    public boolean stillValid(Player player)
    {
        return menuStack != null && !menuStack.isEmpty();
    }

    @Override
    protected boolean shouldSendQuickData()
    {
        boolean result = super.shouldSendQuickData()
                || lastControlMode != BaseMachineItem.getControlModeOrDefault(menuStack, RedStoneControlMode.IGNORE)
                || lastFeederMode != BaseMachineItem.getFeederModeOrDefault(menuStack, FeederMode.NORMAL);

        if (result)
        {
            lastControlMode = BaseMachineItem.getControlModeOrDefault(menuStack, RedStoneControlMode.IGNORE);
            lastFeederMode = BaseMachineItem.getFeederModeOrDefault(menuStack, FeederMode.NORMAL);
        }

        return result;
    }

    @Override
    protected void writeQuickDataTag(CompoundTag tag)
    {
        super.writeQuickDataTag(tag);
        tag.putString("control_mode", BaseMachineItem.getControlModeOrDefault(menuStack, RedStoneControlMode.IGNORE).name());
        tag.putString("feeder_mode", BaseMachineItem.getFeederModeOrDefault(menuStack, FeederMode.NORMAL).name());
    }

    @Override
    public void readQuickDataTag(CompoundTag tag)
    {
        super.readQuickDataTag(tag);
        BaseMachineItem.setControlMode(menuStack, RedStoneControlMode.valueOf(tag.getString("control_mode")));
        BaseMachineItem.setFeederMode(menuStack, FeederMode.valueOf(tag.getString("feeder_mode")));
    }
}
