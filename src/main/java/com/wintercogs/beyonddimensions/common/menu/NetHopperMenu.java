package com.wintercogs.beyonddimensions.common.menu;

import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.api.storage.handler.IStackHandler;
import com.wintercogs.beyonddimensions.api.storage.handler.impl.StackHandler;
import com.wintercogs.beyonddimensions.client.gui.CommonTextures;
import com.wintercogs.beyonddimensions.common.block.entity.NetHopperBlockEntity;
import com.wintercogs.beyonddimensions.common.machine.*;
import com.wintercogs.beyonddimensions.common.menu.sync.BDMenuResources;
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
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * 维度网络漏斗：本页七个模式各自独立声明与校验。
 */
public class NetHopperMenu extends BDBaseMenu
{
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(Registries.MENU, BDConstants.MODID);
    public static final Supplier<MenuType<NetHopperMenu>> Net_Hopper_Menu = MENU_TYPES.register("net_hopper_menu", () -> IMenuTypeExtension.create(NetHopperMenu::new));

    private static final int slotStartY = CommonTextures.TOP_BASE_COMMON_HEIGHT + 1;
    private static final int invSlotStartY = CommonTextures.TOP_BASE_COMMON_HEIGHT + CommonTextures.FILTER_SLOTS_HEIGHT * 4 + CommonTextures.COMMON_CONNECTION_HEIGHT + 7;

    private final IStackHandler storage;

    public final NetHopperBlockEntity be;

    // 以下字段仅用于客户端镜像，由同步schema的setter写入
    private FilterMode clientFilter = FilterMode.IGNORE;
    private boolean filterEditable;
    private RedStoneControlMode clientRedstone = RedStoneControlMode.IGNORE;
    private boolean redstoneEditable;
    private HopperItemMode clientItems = HopperItemMode.ALLOW;
    private boolean itemsEditable;
    private HopperXpMode clientExperience = HopperXpMode.ALLOW;
    private boolean experienceEditable;
    private HopperFluidMode clientFluids = HopperFluidMode.ALLOW;
    private boolean fluidsEditable;
    private HopperNBTMode clientComponents = HopperNBTMode.ALLOW;
    private boolean componentsEditable;
    private HopperRangeMode clientRange = HopperRangeMode.RADIUS_MID;
    private boolean rangeEditable;

    private static final SyncSchema<NetHopperMenu> SCHEMA = SyncSchema.<NetHopperMenu>builder("beyonddimensions:hopper", 1)
            .field("value.filter", SyncCodecs.enumeration(FilterMode.class),
                    m -> m.player.level().isClientSide() ? m.clientFilter : m.be.filterMode,
                    (m, value) -> m.clientFilter = value)
            .field("editable.filter", SyncCodecs.BOOLEAN,
                    m -> m.player.level().isClientSide() ? m.filterEditable : editable(m),
                    (m, value) -> m.filterEditable = value)
            .field("value.redstone", SyncCodecs.enumeration(RedStoneControlMode.class),
                    m -> m.player.level().isClientSide() ? m.clientRedstone : m.be.controlMode,
                    (m, value) -> m.clientRedstone = value)
            .field("editable.redstone", SyncCodecs.BOOLEAN,
                    m -> m.player.level().isClientSide() ? m.redstoneEditable : editable(m),
                    (m, value) -> m.redstoneEditable = value)
            .field("value.items", SyncCodecs.enumeration(HopperItemMode.class),
                    m -> m.player.level().isClientSide() ? m.clientItems : m.be.hopperItemMode,
                    (m, value) -> m.clientItems = value)
            .field("editable.items", SyncCodecs.BOOLEAN,
                    m -> m.player.level().isClientSide() ? m.itemsEditable : editable(m),
                    (m, value) -> m.itemsEditable = value)
            .field("value.experience", SyncCodecs.enumeration(HopperXpMode.class),
                    m -> m.player.level().isClientSide() ? m.clientExperience : m.be.hopperXpMode,
                    (m, value) -> m.clientExperience = value)
            .field("editable.experience", SyncCodecs.BOOLEAN,
                    m -> m.player.level().isClientSide() ? m.experienceEditable : editable(m),
                    (m, value) -> m.experienceEditable = value)
            .field("value.fluids", SyncCodecs.enumeration(HopperFluidMode.class),
                    m -> m.player.level().isClientSide() ? m.clientFluids : m.be.hopperFluidMode,
                    (m, value) -> m.clientFluids = value)
            .field("editable.fluids", SyncCodecs.BOOLEAN,
                    m -> m.player.level().isClientSide() ? m.fluidsEditable : editable(m),
                    (m, value) -> m.fluidsEditable = value)
            .field("value.components", SyncCodecs.enumeration(HopperNBTMode.class),
                    m -> m.player.level().isClientSide() ? m.clientComponents : m.be.hopperNBTMode,
                    (m, value) -> m.clientComponents = value)
            .field("editable.components", SyncCodecs.BOOLEAN,
                    m -> m.player.level().isClientSide() ? m.componentsEditable : editable(m),
                    (m, value) -> m.componentsEditable = value)
            .field("value.range", SyncCodecs.enumeration(HopperRangeMode.class),
                    m -> m.player.level().isClientSide() ? m.clientRange : m.be.hopperRangeMode,
                    (m, value) -> m.clientRange = value)
            .field("editable.range", SyncCodecs.BOOLEAN,
                    m -> m.player.level().isClientSide() ? m.rangeEditable : editable(m),
                    (m, value) -> m.rangeEditable = value)
            .build();

    private static final MenuAction<NetHopperMenu, FilterMode> SET_FILTER = MenuAction.of("set.filter",
            SyncCodecs.enumeration(FilterMode.class), (m, player, value) -> {
                if (!editable(m) || m.be == null) return false;
                if (m.be.filterMode != value)
                {
                    m.be.filterMode = value;
                    m.markBoardChanged();
                }
                return true;
            });

    private static final MenuAction<NetHopperMenu, RedStoneControlMode> SET_REDSTONE = MenuAction.of("set.redstone",
            SyncCodecs.enumeration(RedStoneControlMode.class), (m, player, value) -> {
                if (!editable(m) || m.be == null) return false;
                if (m.be.controlMode != value)
                {
                    m.be.controlMode = value;
                    m.markBoardChanged();
                }
                return true;
            });

    private static final MenuAction<NetHopperMenu, HopperItemMode> SET_ITEMS = MenuAction.of("set.items",
            SyncCodecs.enumeration(HopperItemMode.class), (m, player, value) -> {
                if (!editable(m) || m.be == null) return false;
                if (m.be.hopperItemMode != value)
                {
                    m.be.hopperItemMode = value;
                    m.markBoardChanged();
                }
                return true;
            });

    private static final MenuAction<NetHopperMenu, HopperXpMode> SET_EXPERIENCE = MenuAction.of("set.experience",
            SyncCodecs.enumeration(HopperXpMode.class), (m, player, value) -> {
                if (!editable(m) || m.be == null) return false;
                if (m.be.hopperXpMode != value)
                {
                    m.be.hopperXpMode = value;
                    m.markBoardChanged();
                }
                return true;
            });

    private static final MenuAction<NetHopperMenu, HopperFluidMode> SET_FLUIDS = MenuAction.of("set.fluids",
            SyncCodecs.enumeration(HopperFluidMode.class), (m, player, value) -> {
                if (!editable(m) || m.be == null) return false;
                if (m.be.hopperFluidMode != value)
                {
                    m.be.hopperFluidMode = value;
                    m.markBoardChanged();
                }
                return true;
            });

    private static final MenuAction<NetHopperMenu, HopperNBTMode> SET_COMPONENTS = MenuAction.of("set.components",
            SyncCodecs.enumeration(HopperNBTMode.class), (m, player, value) -> {
                if (!editable(m) || m.be == null) return false;
                if (m.be.hopperNBTMode != value)
                {
                    m.be.hopperNBTMode = value;
                    m.markBoardChanged();
                }
                return true;
            });

    private static final MenuAction<NetHopperMenu, HopperRangeMode> SET_RANGE = MenuAction.of("set.range",
            SyncCodecs.enumeration(HopperRangeMode.class), (m, player, value) -> {
                if (!editable(m) || m.be == null) return false;
                if (m.be.hopperRangeMode != value)
                {
                    m.be.hopperRangeMode = value;
                    m.markBoardChanged();
                }
                return true;
            });

    private final MenuSync<NetHopperMenu> synchronization;

    @Override
    public MenuSync<NetHopperMenu> menuSync()
    {
        return synchronization;
    }

    public NetHopperMenu(int id, Inventory playerInventory, FriendlyByteBuf data)
    {
        this(id, playerInventory, new StackHandler(36), (NetHopperBlockEntity) playerInventory.player.level().getBlockEntity(data.readBlockPos()));
    }

    public NetHopperMenu(int containerId, Inventory playerInventory, @Nullable IStackHandler storage, NetHopperBlockEntity be)
    {
        super(Net_Hopper_Menu.get(), containerId, playerInventory);

        this.be = be;

        if (playerInventory.player.level().isClientSide())
        {
            this.storage = new StackHandler(36);
        }
        else
        {
            this.storage = storage;
        }

        addPlayerInv(playerInventory);
        addFlagSlots();
        synchronization = commands().inventory(BDMenuResources.bind(this, SCHEMA))
                .action(SET_FILTER).action(SET_REDSTONE).action(SET_ITEMS).action(SET_EXPERIENCE)
                .action(SET_FLUIDS).action(SET_COMPONENTS).action(SET_RANGE);
    }

    private static boolean editable(NetHopperMenu menu)
    {
        return !menu.player.isSpectator();
    }

    private void markBoardChanged()
    {
        be.setChanged();
        player.level().sendBlockUpdated(be.getBlockPos(), be.getBlockState(), be.getBlockState(), 2);
    }

    // 客户端读取接口：本页只暴露自己这七个模式

    public FilterMode filter()
    {
        return clientFilter;
    }

    public RedStoneControlMode redstone()
    {
        return clientRedstone;
    }

    public HopperItemMode items()
    {
        return clientItems;
    }

    public HopperXpMode experience()
    {
        return clientExperience;
    }

    public HopperFluidMode fluids()
    {
        return clientFluids;
    }

    public HopperNBTMode components()
    {
        return clientComponents;
    }

    public HopperRangeMode range()
    {
        return clientRange;
    }

    public boolean filterEditable()
    {
        return filterEditable;
    }

    public boolean redstoneEditable()
    {
        return redstoneEditable;
    }

    public boolean itemsEditable()
    {
        return itemsEditable;
    }

    public boolean experienceEditable()
    {
        return experienceEditable;
    }

    public boolean fluidsEditable()
    {
        return fluidsEditable;
    }

    public boolean componentsEditable()
    {
        return componentsEditable;
    }

    public boolean rangeEditable()
    {
        return rangeEditable;
    }

    public boolean requestFilter(int ordinal)
    {
        FilterMode[] modes = FilterMode.values();
        return filterEditable && ordinal >= 0 && ordinal < modes.length && synchronization.request(SET_FILTER, modes[ordinal]).queued();
    }

    public boolean requestRedstone(int ordinal)
    {
        RedStoneControlMode[] modes = RedStoneControlMode.values();
        return redstoneEditable && ordinal >= 0 && ordinal < modes.length && synchronization.request(SET_REDSTONE, modes[ordinal]).queued();
    }

    public boolean requestItems(int ordinal)
    {
        HopperItemMode[] modes = HopperItemMode.values();
        return itemsEditable && ordinal >= 0 && ordinal < modes.length && synchronization.request(SET_ITEMS, modes[ordinal]).queued();
    }

    public boolean requestExperience(int ordinal)
    {
        HopperXpMode[] modes = HopperXpMode.values();
        return experienceEditable && ordinal >= 0 && ordinal < modes.length && synchronization.request(SET_EXPERIENCE, modes[ordinal]).queued();
    }

    public boolean requestFluids(int ordinal)
    {
        HopperFluidMode[] modes = HopperFluidMode.values();
        return fluidsEditable && ordinal >= 0 && ordinal < modes.length && synchronization.request(SET_FLUIDS, modes[ordinal]).queued();
    }

    public boolean requestComponents(int ordinal)
    {
        HopperNBTMode[] modes = HopperNBTMode.values();
        return componentsEditable && ordinal >= 0 && ordinal < modes.length && synchronization.request(SET_COMPONENTS, modes[ordinal]).queued();
    }

    public boolean requestRange(int ordinal)
    {
        HopperRangeMode[] modes = HopperRangeMode.values();
        return rangeEditable && ordinal >= 0 && ordinal < modes.length && synchronization.request(SET_RANGE, modes[ordinal]).queued();
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
        return be != null && !be.isRemoved();
    }
}
