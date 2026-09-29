package com.wintercogs.beyonddimensions.common.menu;

import com.mojang.datafixers.util.Pair;
import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.api.storage.handler.IStackHandler;
import com.wintercogs.beyonddimensions.api.storage.handler.impl.StackHandler;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.client.gui.CommonTextures;
import com.wintercogs.beyonddimensions.common.init.BDDataComponents;
import com.wintercogs.beyonddimensions.common.machine.FuzzyMode;
import com.wintercogs.beyonddimensions.common.machine.ReceiveMode;
import com.wintercogs.beyonddimensions.common.machine.RedStoneControlMode;
import com.wintercogs.beyonddimensions.common.menu.widget.slot.FlagStackTypedSlot;
import dev.compixel.forge.sync.MenuAction;
import dev.compixel.forge.sync.MenuSync;
import dev.compixel.sync.state.SyncCodecs;
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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** 网络装配器：设置保存在物品组件上，另带五个装备模板槽。 */
public class NetRestockerMenu extends BDBaseMenu
{
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(Registries.MENU, BDConstants.MODID);
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

    // 以下字段仅用于客户端镜像，由同步schema的setter写入
    private RedStoneControlMode clientWorking = RedStoneControlMode.IGNORE;
    private boolean workingEditable;
    private FuzzyMode clientMatching = FuzzyMode.DISABLE;
    private boolean matchingEditable;
    private ReceiveMode clientRecycle = ReceiveMode.STOP;
    private boolean recycleEditable;

    private static final List<RedStoneControlMode> WORKING_OPTIONS = List.of(RedStoneControlMode.IGNORE, RedStoneControlMode.NOT_WORKING);

    private static final SyncSchema<NetRestockerMenu> SCHEMA = SyncSchema.<NetRestockerMenu>builder("beyonddimensions:restocker", 1)
            .field("value.working", SyncCodecs.enumeration(RedStoneControlMode.class),
                    m -> m.player.level().isClientSide() ? m.clientWorking : m.menuStack.getOrDefault(BDDataComponents.CONTROL_MODE, RedStoneControlMode.IGNORE),
                    (m, value) -> m.clientWorking = value)
            .field("editable.working", SyncCodecs.BOOLEAN,
                    m -> m.player.level().isClientSide() ? m.workingEditable : editable(m),
                    (m, value) -> m.workingEditable = value)
            .field("value.matching", SyncCodecs.enumeration(FuzzyMode.class),
                    m -> m.player.level().isClientSide() ? m.clientMatching : m.menuStack.getOrDefault(BDDataComponents.FUZZY_MODE, FuzzyMode.DISABLE),
                    (m, value) -> m.clientMatching = value)
            .field("editable.matching", SyncCodecs.BOOLEAN,
                    m -> m.player.level().isClientSide() ? m.matchingEditable : editable(m),
                    (m, value) -> m.matchingEditable = value)
            .field("value.recycle", SyncCodecs.enumeration(ReceiveMode.class),
                    m -> m.player.level().isClientSide() ? m.clientRecycle : m.menuStack.getOrDefault(BDDataComponents.RECEIVE_MODE, ReceiveMode.STOP),
                    (m, value) -> m.clientRecycle = value)
            .field("editable.recycle", SyncCodecs.BOOLEAN,
                    m -> m.player.level().isClientSide() ? m.recycleEditable : editable(m),
                    (m, value) -> m.recycleEditable = value)
            .build();

    private static final MenuAction<NetRestockerMenu, RedStoneControlMode> SET_WORKING = MenuAction.of("set.working",
            SyncCodecs.enumeration(RedStoneControlMode.class), (m, player, value) -> {
                if (!editable(m) || !WORKING_OPTIONS.contains(value)) return false;
                var current = m.menuStack.getOrDefault(BDDataComponents.CONTROL_MODE, RedStoneControlMode.IGNORE);
                if (current != value) { m.menuStack.set(BDDataComponents.CONTROL_MODE, value); m.markItemChanged(); }
                return true;
            });

    private static final MenuAction<NetRestockerMenu, FuzzyMode> SET_MATCHING = MenuAction.of("set.matching",
            SyncCodecs.enumeration(FuzzyMode.class), (m, player, value) -> {
                if (!editable(m)) return false;
                var current = m.menuStack.getOrDefault(BDDataComponents.FUZZY_MODE, FuzzyMode.DISABLE);
                if (current != value) { m.menuStack.set(BDDataComponents.FUZZY_MODE, value); m.markItemChanged(); }
                return true;
            });

    private static final MenuAction<NetRestockerMenu, ReceiveMode> SET_RECYCLE = MenuAction.of("set.recycle",
            SyncCodecs.enumeration(ReceiveMode.class), (m, player, value) -> {
                if (!editable(m)) return false;
                var current = m.menuStack.getOrDefault(BDDataComponents.RECEIVE_MODE, ReceiveMode.STOP);
                if (current != value) { m.menuStack.set(BDDataComponents.RECEIVE_MODE, value); m.markItemChanged(); }
                return true;
            });

    private final MenuSync<NetRestockerMenu> synchronization;

    @Override public MenuSync<NetRestockerMenu> menuSync() { return synchronization; }

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
        synchronization = commands().inventory(BDMenuResources.bind(this, SCHEMA))
                .action(SET_WORKING).action(SET_MATCHING).action(SET_RECYCLE);
    }

    private static boolean editable(NetRestockerMenu menu) { return !menu.player.isSpectator(); }

    private void markItemChanged() { player.getInventory().setChanged(); }

    // 客户端读取接口：本页只暴露自己这三个模式

    public RedStoneControlMode working() { return clientWorking; }
    public FuzzyMode matching() { return clientMatching; }
    public ReceiveMode recycle() { return clientRecycle; }
    public boolean workingEditable() { return workingEditable; }
    public boolean matchingEditable() { return matchingEditable; }
    public boolean recycleEditable() { return recycleEditable; }

    public boolean ready() { return synchronization.hasSnapshot(); }

    public boolean requestWorking(int ordinal)
    {
        return workingEditable && ordinal >= 0 && ordinal < WORKING_OPTIONS.size()
                && synchronization.request(SET_WORKING, WORKING_OPTIONS.get(ordinal)).queued();
    }

    public boolean requestMatching(int ordinal)
    {
        FuzzyMode[] modes = FuzzyMode.values();
        return matchingEditable && ordinal >= 0 && ordinal < modes.length && synchronization.request(SET_MATCHING, modes[ordinal]).queued();
    }

    public boolean requestRecycle(int ordinal)
    {
        ReceiveMode[] modes = ReceiveMode.values();
        return recycleEditable && ordinal >= 0 && ordinal < modes.length && synchronization.request(SET_RECYCLE, modes[ordinal]).queued();
    }

    /** 工作模式只有开/关两个状态，标签也不走通用的模式命名。 */

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

    private Slot createExtraSlot(int slotIndex, int x, int y, ResourceLocation noItemIcon)
    {
        return new FlagStackTypedSlot(this, storage, slotIndex, x, y)
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
        return player.level().isClientSide() || com.wintercogs.beyonddimensions.util.InventoryHelper.containsExactStack(player, menuStack);
    }
}
