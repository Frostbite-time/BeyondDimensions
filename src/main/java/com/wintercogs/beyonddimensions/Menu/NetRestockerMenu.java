package com.wintercogs.beyonddimensions.Menu;

import com.wintercogs.beyonddimensions.Api.DataBase.Handler.IStackHandler;
import com.wintercogs.beyonddimensions.Api.DataBase.Handler.StackHandler;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.KeyAmount;
import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.GUI.CommonTextures;
import com.wintercogs.beyonddimensions.Machine.FuzzyMode;
import com.wintercogs.beyonddimensions.Machine.ReceiveMode;
import com.wintercogs.beyonddimensions.Machine.RedStoneControlMode;
import com.wintercogs.beyonddimensions.Menu.Slot.FlagStackTypedSlot;
import com.wintercogs.beyonddimensions.common.init.BDDataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class NetRestockerMenu extends BDBaseMenu
{
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(Registries.MENU, BeyondDimensions.MODID);
    public static final Supplier<MenuType<NetRestockerMenu>> Net_Restocker_Menu = MENU_TYPES.register("net_restocker_menu", () -> IMenuTypeExtension.create(NetRestockerMenu::new));


    private static final int slotStartY = CommonTextures.TOP_BASE_COMMON_HEIGHT + CommonTextures.COMMON_CONNECTION_HEIGHT * 2 + 1;
    private static final int invSlotStartY = CommonTextures.TOP_BASE_COMMON_HEIGHT + CommonTextures.COMMON_CONNECTION_HEIGHT * 2 + CommonTextures.FILTER_SLOTS_HEIGHT * 4 + CommonTextures.COMMON_CONNECTION_HEIGHT + 7;
    public static final int EXTRA_SLOT_START_X = 8;
    public static final int EXTRA_SLOT_Y = CommonTextures.TOP_BASE_COMMON_HEIGHT - 1;

    private final IStackHandler storage = new StackHandler(41)
    {
        @Override
        public void onChange()
        {
            super.onChange();
            if (!player.level().isClientSide() && initialized)
                menuStack.set(BDDataComponents.ISTACK_SLOTS, new ArrayList<>(storage.getStorage()));
        }
    };
    private boolean initialized;

    public final ItemStack menuStack;

    private RedStoneControlMode lastControlMode;
    private FuzzyMode lastFuzzyMode;
    private ReceiveMode lastReceiveMode;

    public NetRestockerMenu(int id, Inventory playerInventory, FriendlyByteBuf data)
    {
        this(id, playerInventory, playerInventory.player.getItemInHand(data.readEnum(InteractionHand.class)));
    }

    public NetRestockerMenu(int containerId, Inventory playerInventory, ItemStack menuStack)
    {
        super(Net_Restocker_Menu.get(), containerId, playerInventory);
        this.menuStack = menuStack;

        initialized = false;
        if (!playerInventory.player.level().isClientSide())
        {
            List<KeyAmount> stacks = menuStack.getOrDefault(BDDataComponents.ISTACK_SLOTS, new ArrayList<>());
            for (int i = 0; i < stacks.size(); i++)
            {
                storage.insert(i, stacks.get(i).key(), stacks.get(i).amount(), false);
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

        this.addSlot(createExtraSlot(36, EXTRA_SLOT_START_X, EXTRA_SLOT_Y, InventoryMenu.EMPTY_ARMOR_SLOT_HELMET));
        this.addSlot(createExtraSlot(37, EXTRA_SLOT_START_X + 18, EXTRA_SLOT_Y, InventoryMenu.EMPTY_ARMOR_SLOT_CHESTPLATE));
        this.addSlot(createExtraSlot(38, EXTRA_SLOT_START_X + 36, EXTRA_SLOT_Y, InventoryMenu.EMPTY_ARMOR_SLOT_LEGGINGS));
        this.addSlot(createExtraSlot(39, EXTRA_SLOT_START_X + 54, EXTRA_SLOT_Y, InventoryMenu.EMPTY_ARMOR_SLOT_BOOTS));
        this.addSlot(createExtraSlot(40, EXTRA_SLOT_START_X + 72, EXTRA_SLOT_Y, InventoryMenu.EMPTY_ARMOR_SLOT_SHIELD));
    }

    private Slot createExtraSlot(int slotIndex, int x, int y, Identifier noItemIcon)
    {
        return new FlagStackTypedSlot(this, storage, slotIndex, x, y)
        {
            @Override
            public @Nullable Identifier getNoItemIcon()
            {
                return noItemIcon;
            }
        };
    }

    private void addPlayerInv(Inventory playerInventory)
    {
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
    public boolean stillValid(@NotNull Player player)
    {
        return menuStack != null && !menuStack.isEmpty();
    }

    @Override
    protected boolean shouldSendQuickData()
    {
        boolean result = super.shouldSendQuickData()
                || lastControlMode != menuStack.get(BDDataComponents.CONTROL_MODE)
                || lastFuzzyMode != menuStack.get(BDDataComponents.FUZZY_MODE)
                || lastReceiveMode != menuStack.get(BDDataComponents.RECEIVE_MODE);

        if (result)
        {
            lastControlMode = menuStack.get(BDDataComponents.CONTROL_MODE);
            lastFuzzyMode = menuStack.get(BDDataComponents.FUZZY_MODE);
            lastReceiveMode = menuStack.get(BDDataComponents.RECEIVE_MODE);
        }

        return result;
    }

    @Override
    protected void writeQuickDataTag(CompoundTag tag)
    {
        super.writeQuickDataTag(tag);
        tag.putString("control_mode", menuStack.getOrDefault(BDDataComponents.CONTROL_MODE, RedStoneControlMode.IGNORE).name());
        tag.putString("fuzzy_mode", menuStack.getOrDefault(BDDataComponents.FUZZY_MODE, FuzzyMode.DISABLE).name());
        tag.putString("receive_mode", menuStack.getOrDefault(BDDataComponents.RECEIVE_MODE, ReceiveMode.STOP).name());
    }

    @Override
    public void readQuickDataTag(CompoundTag tag)
    {
        super.readQuickDataTag(tag);
        menuStack.set(BDDataComponents.CONTROL_MODE, RedStoneControlMode.valueOf(tag.getString("control_mode").orElse(RedStoneControlMode.IGNORE.name())));
        menuStack.set(BDDataComponents.FUZZY_MODE, FuzzyMode.valueOf(tag.getString("fuzzy_mode").orElse(FuzzyMode.DISABLE.name())));
        menuStack.set(BDDataComponents.RECEIVE_MODE, ReceiveMode.valueOf(tag.getString("receive_mode").orElse(ReceiveMode.STOP.name())));
    }
}
