package com.wintercogs.beyonddimensions.common.menu;

import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.api.storage.handler.IStackHandler;
import com.wintercogs.beyonddimensions.api.storage.handler.impl.StackHandler;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.client.gui.CommonTextures;
import com.wintercogs.beyonddimensions.common.init.BDDataComponents;
import com.wintercogs.beyonddimensions.common.machine.*;
import com.wintercogs.beyonddimensions.common.menu.widget.slot.FlagStackTypedSlot;
import dev.compixel.forge.sync.MenuAction;
import dev.compixel.forge.sync.MenuSync;
import dev.compixel.sync.state.SyncCodecs;
import dev.compixel.sync.state.SyncSchema;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
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

// 网络磁铁菜单：设置保存在物品组件上，本页七个模式各自独立。
public class NetMagnetMenu extends BDBaseMenu
{
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(Registries.MENU, BDConstants.MODID);
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
                menuStack.set(BDDataComponents.ISTACK_SLOTS, new ArrayList<>(storage.getStorage()));

        }
    };
    private boolean initialized; //initialized必须在初始数据提供完成之后才能设置为true

    public final ItemStack menuStack;

    // 以下字段仅用于客户端镜像，由同步schema的setter写入
    private RedStoneControlMode clientWorking = RedStoneControlMode.IGNORE;
    private boolean workingEditable;
    private FilterMode clientFilter = FilterMode.BLACK;
    private boolean filterEditable;
    private HopperItemMode clientItems = HopperItemMode.ALLOW;
    private boolean itemsEditable;
    private HopperXpMode clientExperience = HopperXpMode.DENY;
    private boolean experienceEditable;
    private HopperFluidMode clientFluids = HopperFluidMode.DENY;
    private boolean fluidsEditable;
    private HopperNBTMode clientComponents = HopperNBTMode.DENY;
    private boolean componentsEditable;
    private HopperRangeMode clientRange = HopperRangeMode.RADIUS_MID;
    private boolean rangeEditable;

    private static final List<RedStoneControlMode> WORKING_OPTIONS = List.of(RedStoneControlMode.IGNORE, RedStoneControlMode.NOT_WORKING);

    private static final SyncSchema<NetMagnetMenu> SCHEMA = SyncSchema.<NetMagnetMenu>builder("beyonddimensions:magnet", 1)
            .field("value.working", SyncCodecs.enumeration(RedStoneControlMode.class),
                    m -> m.player.level().isClientSide() ? m.clientWorking : m.menuStack.getOrDefault(BDDataComponents.CONTROL_MODE, RedStoneControlMode.IGNORE),
                    (m, value) -> m.clientWorking = value)
            .field("editable.working", SyncCodecs.BOOLEAN,
                    m -> m.player.level().isClientSide() ? m.workingEditable : editable(m),
                    (m, value) -> m.workingEditable = value)
            .field("value.filter", SyncCodecs.enumeration(FilterMode.class),
                    m -> m.player.level().isClientSide() ? m.clientFilter : m.menuStack.getOrDefault(BDDataComponents.FILTER_MODE, FilterMode.BLACK),
                    (m, value) -> m.clientFilter = value)
            .field("editable.filter", SyncCodecs.BOOLEAN,
                    m -> m.player.level().isClientSide() ? m.filterEditable : editable(m),
                    (m, value) -> m.filterEditable = value)
            .field("value.items", SyncCodecs.enumeration(HopperItemMode.class),
                    m -> m.player.level().isClientSide() ? m.clientItems : m.menuStack.getOrDefault(BDDataComponents.HOPPER_ITEM_MODE, HopperItemMode.ALLOW),
                    (m, value) -> m.clientItems = value)
            .field("editable.items", SyncCodecs.BOOLEAN,
                    m -> m.player.level().isClientSide() ? m.itemsEditable : editable(m),
                    (m, value) -> m.itemsEditable = value)
            .field("value.experience", SyncCodecs.enumeration(HopperXpMode.class),
                    m -> m.player.level().isClientSide() ? m.clientExperience : m.menuStack.getOrDefault(BDDataComponents.HOPPER_XP_MODE, HopperXpMode.DENY),
                    (m, value) -> m.clientExperience = value)
            .field("editable.experience", SyncCodecs.BOOLEAN,
                    m -> m.player.level().isClientSide() ? m.experienceEditable : editable(m),
                    (m, value) -> m.experienceEditable = value)
            .field("value.fluids", SyncCodecs.enumeration(HopperFluidMode.class),
                    m -> m.player.level().isClientSide() ? m.clientFluids : m.menuStack.getOrDefault(BDDataComponents.HOPPER_FLUID_MODE, HopperFluidMode.DENY),
                    (m, value) -> m.clientFluids = value)
            .field("editable.fluids", SyncCodecs.BOOLEAN,
                    m -> m.player.level().isClientSide() ? m.fluidsEditable : editable(m),
                    (m, value) -> m.fluidsEditable = value)
            .field("value.components", SyncCodecs.enumeration(HopperNBTMode.class),
                    m -> m.player.level().isClientSide() ? m.clientComponents : m.menuStack.getOrDefault(BDDataComponents.HOPPER_NBT_MODE, HopperNBTMode.DENY),
                    (m, value) -> m.clientComponents = value)
            .field("editable.components", SyncCodecs.BOOLEAN,
                    m -> m.player.level().isClientSide() ? m.componentsEditable : editable(m),
                    (m, value) -> m.componentsEditable = value)
            .field("value.range", SyncCodecs.enumeration(HopperRangeMode.class),
                    m -> m.player.level().isClientSide() ? m.clientRange : m.menuStack.getOrDefault(BDDataComponents.HOPPER_RANGE_MODE, HopperRangeMode.RADIUS_MID),
                    (m, value) -> m.clientRange = value)
            .field("editable.range", SyncCodecs.BOOLEAN,
                    m -> m.player.level().isClientSide() ? m.rangeEditable : editable(m),
                    (m, value) -> m.rangeEditable = value)
            .build();

    private static final MenuAction<NetMagnetMenu, RedStoneControlMode> SET_WORKING = MenuAction.of("set.working",
            SyncCodecs.enumeration(RedStoneControlMode.class), (m, player, value) -> {
                if (!editable(m) || !WORKING_OPTIONS.contains(value)) return false;
                var current = m.menuStack.getOrDefault(BDDataComponents.CONTROL_MODE, RedStoneControlMode.IGNORE);
                if (current != value) { m.menuStack.set(BDDataComponents.CONTROL_MODE, value); m.markItemChanged(); }
                return true;
            });

    private static final MenuAction<NetMagnetMenu, FilterMode> SET_FILTER = MenuAction.of("set.filter",
            SyncCodecs.enumeration(FilterMode.class), (m, player, value) -> {
                if (!editable(m)) return false;
                var current = m.menuStack.getOrDefault(BDDataComponents.FILTER_MODE, FilterMode.BLACK);
                if (current != value) { m.menuStack.set(BDDataComponents.FILTER_MODE, value); m.markItemChanged(); }
                return true;
            });

    private static final MenuAction<NetMagnetMenu, HopperItemMode> SET_ITEMS = MenuAction.of("set.items",
            SyncCodecs.enumeration(HopperItemMode.class), (m, player, value) -> {
                if (!editable(m)) return false;
                var current = m.menuStack.getOrDefault(BDDataComponents.HOPPER_ITEM_MODE, HopperItemMode.ALLOW);
                if (current != value) { m.menuStack.set(BDDataComponents.HOPPER_ITEM_MODE, value); m.markItemChanged(); }
                return true;
            });

    private static final MenuAction<NetMagnetMenu, HopperXpMode> SET_EXPERIENCE = MenuAction.of("set.experience",
            SyncCodecs.enumeration(HopperXpMode.class), (m, player, value) -> {
                if (!editable(m)) return false;
                var current = m.menuStack.getOrDefault(BDDataComponents.HOPPER_XP_MODE, HopperXpMode.DENY);
                if (current != value) { m.menuStack.set(BDDataComponents.HOPPER_XP_MODE, value); m.markItemChanged(); }
                return true;
            });

    private static final MenuAction<NetMagnetMenu, HopperFluidMode> SET_FLUIDS = MenuAction.of("set.fluids",
            SyncCodecs.enumeration(HopperFluidMode.class), (m, player, value) -> {
                if (!editable(m)) return false;
                var current = m.menuStack.getOrDefault(BDDataComponents.HOPPER_FLUID_MODE, HopperFluidMode.DENY);
                if (current != value) { m.menuStack.set(BDDataComponents.HOPPER_FLUID_MODE, value); m.markItemChanged(); }
                return true;
            });

    private static final MenuAction<NetMagnetMenu, HopperNBTMode> SET_COMPONENTS = MenuAction.of("set.components",
            SyncCodecs.enumeration(HopperNBTMode.class), (m, player, value) -> {
                if (!editable(m)) return false;
                var current = m.menuStack.getOrDefault(BDDataComponents.HOPPER_NBT_MODE, HopperNBTMode.DENY);
                if (current != value) { m.menuStack.set(BDDataComponents.HOPPER_NBT_MODE, value); m.markItemChanged(); }
                return true;
            });

    private static final MenuAction<NetMagnetMenu, HopperRangeMode> SET_RANGE = MenuAction.of("set.range",
            SyncCodecs.enumeration(HopperRangeMode.class), (m, player, value) -> {
                if (!editable(m)) return false;
                var current = m.menuStack.getOrDefault(BDDataComponents.HOPPER_RANGE_MODE, HopperRangeMode.RADIUS_MID);
                if (current != value) { m.menuStack.set(BDDataComponents.HOPPER_RANGE_MODE, value); m.markItemChanged(); }
                return true;
            });

    private final MenuSync<NetMagnetMenu> synchronization;

    @Override public MenuSync<NetMagnetMenu> menuSync() { return synchronization; }

    public NetMagnetMenu(int id, Inventory playerInventory, FriendlyByteBuf data)
    {
        // Client settings and filters arrive through their server-owned sync channels.
        this(id, playerInventory, ItemStack.EMPTY);
    }

    public NetMagnetMenu(int containerId, Inventory playerInventory, ItemStack menuStack)
    {
        super(Net_Magnet_Menu.get(), containerId, playerInventory);
        this.menuStack = java.util.Objects.requireNonNullElse(menuStack, ItemStack.EMPTY);
        if (!playerInventory.player.level().isClientSide() && this.menuStack.getItem() instanceof com.wintercogs.beyonddimensions.common.item.BaseMachineItem device)
            device.checkComponents(this.menuStack);

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
        synchronization = commands().inventory(BDMenuResources.bind(this, SCHEMA))
                .action(SET_WORKING).action(SET_FILTER).action(SET_ITEMS).action(SET_EXPERIENCE)
                .action(SET_FLUIDS).action(SET_COMPONENTS).action(SET_RANGE);
    }

    private static boolean editable(NetMagnetMenu menu) { return !menu.player.isSpectator(); }

    private void markItemChanged() { player.getInventory().setChanged(); }

    // 客户端读取接口：本页只暴露自己这七个模式

    public RedStoneControlMode working() { return clientWorking; }
    public FilterMode filter() { return clientFilter; }
    public HopperItemMode items() { return clientItems; }
    public HopperXpMode experience() { return clientExperience; }
    public HopperFluidMode fluids() { return clientFluids; }
    public HopperNBTMode components() { return clientComponents; }
    public HopperRangeMode range() { return clientRange; }

    public boolean workingEditable() { return workingEditable; }
    public boolean filterEditable() { return filterEditable; }
    public boolean itemsEditable() { return itemsEditable; }
    public boolean experienceEditable() { return experienceEditable; }
    public boolean fluidsEditable() { return fluidsEditable; }
    public boolean componentsEditable() { return componentsEditable; }
    public boolean rangeEditable() { return rangeEditable; }

    public boolean ready() { return synchronization.hasSnapshot(); }

    public boolean requestWorking(int ordinal)
    {
        return workingEditable && ordinal >= 0 && ordinal < WORKING_OPTIONS.size()
                && synchronization.request(SET_WORKING, WORKING_OPTIONS.get(ordinal)).queued();
    }

    public boolean requestFilter(int ordinal) { return request(SET_FILTER, FilterMode.values(), ordinal, filterEditable); }
    public boolean requestItems(int ordinal) { return request(SET_ITEMS, HopperItemMode.values(), ordinal, itemsEditable); }
    public boolean requestExperience(int ordinal) { return request(SET_EXPERIENCE, HopperXpMode.values(), ordinal, experienceEditable); }
    public boolean requestFluids(int ordinal) { return request(SET_FLUIDS, HopperFluidMode.values(), ordinal, fluidsEditable); }
    public boolean requestComponents(int ordinal) { return request(SET_COMPONENTS, HopperNBTMode.values(), ordinal, componentsEditable); }
    public boolean requestRange(int ordinal) { return request(SET_RANGE, HopperRangeMode.values(), ordinal, rangeEditable); }

    private <E extends Enum<E>> boolean request(MenuAction<NetMagnetMenu, E> action, E[] modes, int ordinal, boolean enabled)
    {
        return enabled && ordinal >= 0 && ordinal < modes.length && synchronization.request(action, modes[ordinal]).queued();
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
        return player.level().isClientSide() || com.wintercogs.beyonddimensions.util.InventoryHelper.containsExactStack(player, menuStack);
    }
}
