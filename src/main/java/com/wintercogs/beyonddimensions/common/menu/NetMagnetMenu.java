package com.wintercogs.beyonddimensions.common.menu;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.api.storage.handler.IStackHandler;
import com.wintercogs.beyonddimensions.api.storage.handler.impl.StackHandler;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.client.gui.CommonTextures;
import com.wintercogs.beyonddimensions.common.init.ModDataComponents;
import com.wintercogs.beyonddimensions.common.machine.*;
import com.wintercogs.beyonddimensions.common.menu.widget.slot.FlagStackTypedSlot;
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

// 网络磁铁菜单
public class NetMagnetMenu extends BDBaseMenu
{
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(Registries.MENU, BeyondDimensions.MODID);
    public static final Supplier<MenuType<NetMagnetMenu>> Net_Magnet_Menu = MENU_TYPES.register("net_magnet_menu", () -> IMenuTypeExtension.create(NetMagnetMenu::new));


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
                menuStack.set(ModDataComponents.ISTACK_SLOTS, new ArrayList<>(storage.getStorage()));

        }
    };
    private boolean initialized; //initialized必须在初始数据提供完成之后才能设置为true

    public final ItemStack menuStack;

    private RedStoneControlMode lastControlMode;
    private FilterMode lastFilterMode;
    private HopperItemMode lastHopperItemMode;
    private HopperXpMode lastHopperXpMode;
    private HopperNBTMode lastHopperNBTMode;
    private HopperFluidMode lastHopperFluidMode;
    private HopperRangeMode lastHopperRangeMode;


    public NetMagnetMenu(int id, Inventory playerInventory, FriendlyByteBuf data)
    {
        this(id, playerInventory, playerInventory.player.getItemInHand(data.readEnum(InteractionHand.class)));
    }

    public NetMagnetMenu(int containerId, Inventory playerInventory, ItemStack menuStack)
    {
        super(Net_Magnet_Menu.get(), containerId, playerInventory);
        this.menuStack = menuStack;

        initialized = false;
        // 为服务端注入真实数据，客户端由槽位同步
        if (!playerInventory.player.level().isClientSide())
        {
            List<KeyAmount> stacks = menuStack.getOrDefault(ModDataComponents.ISTACK_SLOTS, new ArrayList<>());
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
                || lastControlMode != menuStack.get(ModDataComponents.CONTROL_MODE)
                || lastFilterMode != menuStack.get(ModDataComponents.FILTER_MODE)
                || lastHopperItemMode != menuStack.get(ModDataComponents.HOPPER_ITEM_MODE)
                || lastHopperXpMode != menuStack.get(ModDataComponents.HOPPER_XP_MODE)
                || lastHopperNBTMode != menuStack.get(ModDataComponents.HOPPER_NBT_MODE)
                || lastHopperFluidMode != menuStack.get(ModDataComponents.HOPPER_FLUID_MODE)
                || lastHopperRangeMode != menuStack.get(ModDataComponents.HOPPER_RANGE_MODE);

        if (result)
        {
            lastControlMode = menuStack.get(ModDataComponents.CONTROL_MODE);
            lastFilterMode = menuStack.get(ModDataComponents.FILTER_MODE);
            lastHopperItemMode = menuStack.get(ModDataComponents.HOPPER_ITEM_MODE);
            lastHopperXpMode = menuStack.get(ModDataComponents.HOPPER_XP_MODE);
            lastHopperNBTMode = menuStack.get(ModDataComponents.HOPPER_NBT_MODE);
            lastHopperFluidMode = menuStack.get(ModDataComponents.HOPPER_FLUID_MODE);
            lastHopperRangeMode = menuStack.get(ModDataComponents.HOPPER_RANGE_MODE);
        }

        return result;
    }

    @Override
    protected void writeQuickDataTag(CompoundTag tag)
    {
        super.writeQuickDataTag(tag);
        tag.putString("filter_type", menuStack.get(ModDataComponents.FILTER_MODE).name());
        tag.putString("control_mode", menuStack.get(ModDataComponents.CONTROL_MODE).name());
        tag.putString("hopper_item_mode", menuStack.get(ModDataComponents.HOPPER_ITEM_MODE).name());
        tag.putString("hopper_xp_mode", menuStack.get(ModDataComponents.HOPPER_XP_MODE).name());
        tag.putString("hopper_nbt_mode", menuStack.get(ModDataComponents.HOPPER_NBT_MODE).name());
        tag.putString("hopper_fluid_mode", menuStack.get(ModDataComponents.HOPPER_FLUID_MODE).name());
        tag.putString("hopper_range_mode", menuStack.get(ModDataComponents.HOPPER_RANGE_MODE).name());
    }

    @Override
    public void readQuickDataTag(CompoundTag tag)
    {
        super.readQuickDataTag(tag);
        menuStack.set(ModDataComponents.FILTER_MODE, FilterMode.valueOf(tag.getString("filter_type")));
        menuStack.set(ModDataComponents.CONTROL_MODE, RedStoneControlMode.valueOf(tag.getString("control_mode")));
        menuStack.set(ModDataComponents.HOPPER_ITEM_MODE, HopperItemMode.valueOf(tag.getString("hopper_item_mode")));
        menuStack.set(ModDataComponents.HOPPER_XP_MODE, HopperXpMode.valueOf(tag.getString("hopper_xp_mode")));
        menuStack.set(ModDataComponents.HOPPER_NBT_MODE, HopperNBTMode.valueOf(tag.getString("hopper_nbt_mode")));
        menuStack.set(ModDataComponents.HOPPER_FLUID_MODE, HopperFluidMode.valueOf(tag.getString("hopper_fluid_mode")));
        menuStack.set(ModDataComponents.HOPPER_RANGE_MODE, HopperRangeMode.valueOf(tag.getString("hopper_range_mode")));
    }
}
