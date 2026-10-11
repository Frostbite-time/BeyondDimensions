package com.wintercogs.beyonddimensions.common.menu;

import com.mojang.datafixers.util.Pair;
import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.api.machine.Machine;
import com.wintercogs.beyonddimensions.api.machine.MachineItem;
import com.wintercogs.beyonddimensions.api.menu.BdMenuActions;
import com.wintercogs.beyonddimensions.api.storage.handler.IStackHandler;
import com.wintercogs.beyonddimensions.api.storage.handler.impl.StackHandler;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.common.init.BDDataComponents;
import com.wintercogs.beyonddimensions.common.menu.machine.MachineMenu;
import com.wintercogs.beyonddimensions.common.menu.machine.MachineSettingsSync;
import com.wintercogs.beyonddimensions.common.menu.sync.BDMenuResources;
import com.wintercogs.beyonddimensions.common.menu.widget.slot.FlagStackTypedSlot;
import dev.compixel.forge.sync.MenuAction;
import dev.compixel.forge.sync.MenuSync;
import dev.compixel.sync.state.SyncSchema;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
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
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * 网络装配器：五个装备模板槽与玩家背包；设置保存在物品上，由 MachineSettingsSync 统一同步与修改。
 */
public class NetRestockerMenu extends BDBaseMenu implements MachineMenu
{
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(Registries.MENU, BDConstants.MODID);
    public static final Supplier<MenuType<NetRestockerMenu>> Net_Restocker_Menu = MENU_TYPES.register("net_restocker_menu", () -> IMenuTypeExtension.create(NetRestockerMenu::new));

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

    private static final SyncSchema<NetRestockerMenu> SCHEMA =
            MachineSettingsSync.field(SyncSchema.<NetRestockerMenu>builder("beyonddimensions:restocker", 2)).build();

    private static final MenuAction<NetRestockerMenu, MachineSettingsSync.Change> SET_SETTING = MachineSettingsSync.action();

    private final MachineSettingsSync.Mirror machineSettings = new MachineSettingsSync.Mirror();

    private final MenuSync<NetRestockerMenu> synchronization;

    @Override
    public MenuSync<NetRestockerMenu> menuSync()
    {
        return synchronization;
    }

    @Override
    public @Nullable Machine machine()
    {
        return !player.level().isClientSide() && menuStack.getItem() instanceof MachineItem item ? item.machine(menuStack) : null;
    }

    @Override
    public MachineSettingsSync.Mirror machineSettings()
    {
        return machineSettings;
    }

    /**
     * 客户端：把设置改成编辑声明里的第 index 个值
     */
    public boolean requestSetting(ResourceLocation setting, int index)
    {
        return MachineSettingsSync.request(this, synchronization, SET_SETTING, setting, index);
    }

    public NetRestockerMenu(int id, Inventory playerInventory, FriendlyByteBuf data)
    {
        this(id, playerInventory, playerInventory.player.getItemInHand(data.readEnum(InteractionHand.class)));
    }

    public NetRestockerMenu(int containerId, Inventory playerInventory, ItemStack menuStack)
    {
        super(Net_Restocker_Menu.get(), containerId, playerInventory);
        this.menuStack = java.util.Objects.requireNonNullElse(menuStack, ItemStack.EMPTY);
        if (!playerInventory.player.level().isClientSide() && this.menuStack.getItem() instanceof com.wintercogs.beyonddimensions.common.item.BaseMachineItem device)
            device.checkComponents(this.menuStack);

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
        synchronization = BdMenuActions.attach(this, commands().inventory(BDMenuResources.bind(this, SCHEMA)).action(SET_SETTING));
    }

    private void addFlagSlots()
    {
        for (int row = 0; row < 4; row++)
        {
            for (int col = 0; col < 9; col++)
            {
                FlagStackTypedSlot flagSlot = new FlagStackTypedSlot(this, storage, row * 9 + col);
                this.addSlot(flagSlot);
            }
        }

        this.addSlot(createExtraSlot(36, InventoryMenu.EMPTY_ARMOR_SLOT_HELMET));
        this.addSlot(createExtraSlot(37, InventoryMenu.EMPTY_ARMOR_SLOT_CHESTPLATE));
        this.addSlot(createExtraSlot(38, InventoryMenu.EMPTY_ARMOR_SLOT_LEGGINGS));
        this.addSlot(createExtraSlot(39, InventoryMenu.EMPTY_ARMOR_SLOT_BOOTS));
        this.addSlot(createExtraSlot(40, InventoryMenu.EMPTY_ARMOR_SLOT_SHIELD));
    }

    private Slot createExtraSlot(int slotIndex, ResourceLocation noItemIcon)
    {
        return new FlagStackTypedSlot(this, storage, slotIndex)
        {
            @Override
            public Pair<ResourceLocation, ResourceLocation> getNoItemIcon()
            {
                return Pair.of(InventoryMenu.BLOCK_ATLAS, noItemIcon);
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
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 0, 0));
            }
        }
        for (int col = 0; col < 9; ++col)
        {
            this.addSlot(new Slot(playerInventory, col, 0, 0));
        }
        inventoryEndIndex = slots.size();
    }

    @Override
    public boolean stillValid(@NotNull Player player)
    {
        return player.level().isClientSide() || com.wintercogs.beyonddimensions.util.InventoryHelper.containsExactStack(player, menuStack);
    }
}
