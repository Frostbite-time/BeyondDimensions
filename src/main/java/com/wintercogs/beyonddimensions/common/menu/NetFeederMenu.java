package com.wintercogs.beyonddimensions.common.menu;

import com.wintercogs.beyonddimensions.api.storage.handler.IStackHandler;
import com.wintercogs.beyonddimensions.api.storage.handler.impl.StackHandler;
import com.wintercogs.beyonddimensions.api.storage.key.IStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.impl.ItemStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.client.gui.CommonTextures;
import com.wintercogs.beyonddimensions.machine.FeederMode;
import com.wintercogs.beyonddimensions.machine.RedStoneControlMode;
import com.wintercogs.beyonddimensions.common.menu.Slot.FlagStackTypedSlot;
import com.wintercogs.beyonddimensions.common.init.BDDataComponents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class NetFeederMenu extends BDBaseMenu
{
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(Registries.MENU, BeyondDimensions.MODID);
    public static final Supplier<MenuType<NetFeederMenu>> Net_Feeder_Menu = MENU_TYPES.register("net_feeder_menu", () -> IMenuTypeExtension.create(NetFeederMenu::new));


    private static final int slotStartY = CommonTextures.TOP_BASE_COMMON_HEIGHT + 1;
    private static final int invSlotStartY = CommonTextures.TOP_BASE_COMMON_HEIGHT + CommonTextures.FILTER_SLOTS_HEIGHT * 4 + CommonTextures.COMMON_CONNECTION_HEIGHT + 7;

    // storage的初始数据由itemStack提供，随后storage每次变化都重新向其中写入数据
    private final IStackHandler storage = new StackHandler(36)
    {
        @Override
        public void onChange()
        {
            super.onChange();
            if (!player.level().isClientSide() && initialized)
                menuStack.set(BDDataComponents.ISTACK_SLOTS, new ArrayList<>(storage.getStorage()));

        }

        @Override
        public boolean isStackValid(int slot, IStackKey<?> stack)
        {
            return super.isStackValid(slot, stack)
                    && stack instanceof ItemStackKey itemStackKey
                    && itemStackKey.getReadOnlyStack().get(DataComponents.FOOD) != null;
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
        super(Net_Feeder_Menu.get(), containerId, playerInventory);
        this.menuStack = menuStack;

        initialized = false;
        // 为服务端注入真实数据，客户端由槽位同步
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
    public boolean stillValid(@NotNull Player player)
    {
        return menuStack != null && !menuStack.isEmpty();
    }

    @Override
    protected boolean shouldSendQuickData()
    {
        boolean result = super.shouldSendQuickData()
                || lastControlMode != menuStack.get(BDDataComponents.CONTROL_MODE)
                || lastFeederMode != menuStack.get(BDDataComponents.FEEDER_MODE);

        if (result)
        {
            lastControlMode = menuStack.get(BDDataComponents.CONTROL_MODE);
            lastFeederMode = menuStack.get(BDDataComponents.FEEDER_MODE);
        }

        return result;
    }

    @Override
    protected void writeQuickDataTag(CompoundTag tag)
    {
        super.writeQuickDataTag(tag);
        tag.putString("control_mode", menuStack.get(BDDataComponents.CONTROL_MODE).name());
        tag.putString("feeder_mode", menuStack.get(BDDataComponents.FEEDER_MODE).name());
    }

    @Override
    public void readQuickDataTag(CompoundTag tag)
    {
        super.readQuickDataTag(tag);
        menuStack.set(BDDataComponents.CONTROL_MODE, RedStoneControlMode.valueOf(tag.getString("control_mode").orElse(RedStoneControlMode.IGNORE.name())));
        menuStack.set(BDDataComponents.FEEDER_MODE, FeederMode.valueOf(tag.getString("feeder_mode").orElse(FeederMode.NORMAL.name())));
    }
}
