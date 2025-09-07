package com.wintercogs.beyonddimensions.Menu;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.BlockEntity.Custom.NetHopperBlockEntity;
import com.wintercogs.beyonddimensions.GUI.CommonTextures;
import com.wintercogs.beyonddimensions.Machine.*;
import com.wintercogs.beyonddimensions.Menu.Slot.FlagStackTypedSlot;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public class NetHopperMenu extends BDBaseMenu
{

    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(Registries.MENU, BeyondDimensions.MODID);
    public static final Supplier<MenuType<NetHopperMenu>> Net_Hopper_Menu = MENU_TYPES.register("net_hopper_menu", () -> IMenuTypeExtension.create(NetHopperMenu::new));


    private static final int slotStartY = CommonTextures.TOP_BASE_COMMON_HEIGHT + 1;
    private static final int invSlotStartY = CommonTextures.TOP_BASE_COMMON_HEIGHT + CommonTextures.FILTER_SLOTS_HEIGHT*4 + CommonTextures.COMMON_CONNECTION_HEIGHT +7;

    private final IStackTypedHandler storage;

    public final NetHopperBlockEntity be;

    public NetHopperMenu(int id, Inventory playerInventory, FriendlyByteBuf data)
    {
        this(id, playerInventory, new StackTypedHandler(36), (NetHopperBlockEntity) playerInventory.player.level().getBlockEntity(data.readBlockPos()));
    }

    public NetHopperMenu(int containerId, Inventory playerInventory, @Nullable IStackTypedHandler storage, NetHopperBlockEntity be)
    {
        super(Net_Hopper_Menu.get(), containerId, playerInventory);

        this.be = be;

        if(playerInventory.player.level().isClientSide())
        {
            this.storage = new StackTypedHandler(36);
        }
        else
        {
            this.storage = storage;
        }


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
        tag.putString("hopper_item_mode",be.hopperItemMode.name());
        tag.putString("hopper_xp_mode",be.hopperXpMode.name());
        tag.putString("hopper_fluid_mode", be.hopperFluidMode.name());
        tag.putString("hopper_nbt_mode", be.hopperNBTMode.name());
        tag.putString("hopper_range_mode", be.hopperRangeMode.name());
    }

    @Override
    public void readQuickDataTag(CompoundTag tag)
    {
        super.readQuickDataTag(tag);
        be.filterMode = FilterMode.valueOf(tag.getString("filter_type"));
        be.controlMode = RedStoneControlMode.valueOf(tag.getString("control_mode"));
        be.hopperItemMode = HopperItemMode.valueOf(tag.getString("hopper_item_mode"));
        be.hopperXpMode = HopperXpMode.valueOf(tag.getString("hopper_xp_mode"));
        be.hopperFluidMode = HopperFluidMode.valueOf(tag.getString("hopper_fluid_mode"));
        be.hopperNBTMode = HopperNBTMode.valueOf(tag.getString("hopper_nbt_mode"));
        be.hopperRangeMode = HopperRangeMode.valueOf(tag.getString("hopper_range_mode"));
        if(!player.level().isClientSide())
        {
            // 服务端接收到更新信息后立刻通知保存
            player.level().blockEntityChanged(be.getBlockPos());
            player.level().sendBlockUpdated(be.getBlockPos(),be.getBlockState(),be.getBlockState(),2);
        }
    }
}
