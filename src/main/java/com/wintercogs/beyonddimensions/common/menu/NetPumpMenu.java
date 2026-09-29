package com.wintercogs.beyonddimensions.common.menu;

import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.api.storage.handler.IStackHandler;
import com.wintercogs.beyonddimensions.api.storage.handler.impl.StackHandler;
import com.wintercogs.beyonddimensions.client.gui.CommonTextures;
import com.wintercogs.beyonddimensions.common.block.entity.NetPumpBlockEntity;
import com.wintercogs.beyonddimensions.common.machine.FilterMode;
import com.wintercogs.beyonddimensions.common.machine.RedStoneControlMode;
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

/** 维度网络泵：只同步本页自己的过滤器与红石模式。 */
public class NetPumpMenu extends BDBaseMenu
{
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(Registries.MENU, BDConstants.MODID);
    public static final Supplier<MenuType<NetPumpMenu>> Net_Pump_Menu = MENU_TYPES.register("net_pump_menu", () -> IMenuTypeExtension.create(NetPumpMenu::new));

    private static final int slotStartY = CommonTextures.TOP_BASE_COMMON_HEIGHT + 1;
    private static final int invSlotStartY = CommonTextures.TOP_BASE_COMMON_HEIGHT + CommonTextures.FILTER_SLOTS_HEIGHT * 4 + CommonTextures.COMMON_CONNECTION_HEIGHT + 7;

    private final IStackHandler storage;

    public final NetPumpBlockEntity be;

    // 以下字段仅用于客户端镜像，由同步schema的setter写入
    private FilterMode clientFilter = FilterMode.IGNORE;
    private RedStoneControlMode clientRedstone = RedStoneControlMode.IGNORE;
    private boolean filterEditable;
    private boolean redstoneEditable;

    private static final SyncSchema<NetPumpMenu> SCHEMA = SyncSchema.<NetPumpMenu>builder("beyonddimensions:pump", 1)
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
            .build();

    private static final MenuAction<NetPumpMenu, FilterMode> SET_FILTER = MenuAction.of("set.filter",
            SyncCodecs.enumeration(FilterMode.class), (m, player, value) -> {
                if (!editable(m) || m.be == null) return false;
                if (m.be.filterMode != value) { m.be.filterMode = value; m.markBoardChanged(); }
                return true;
            });

    private static final MenuAction<NetPumpMenu, RedStoneControlMode> SET_REDSTONE = MenuAction.of("set.redstone",
            SyncCodecs.enumeration(RedStoneControlMode.class), (m, player, value) -> {
                if (!editable(m) || m.be == null) return false;
                if (m.be.controlMode != value) { m.be.controlMode = value; m.markBoardChanged(); }
                return true;
            });

    private final MenuSync<NetPumpMenu> synchronization;

    @Override public MenuSync<NetPumpMenu> menuSync() { return synchronization; }

    public NetPumpMenu(int id, Inventory playerInventory, FriendlyByteBuf data)
    {
        this(id, playerInventory, new StackHandler(36), (NetPumpBlockEntity) playerInventory.player.level().getBlockEntity(data.readBlockPos()));
    }

    public NetPumpMenu(int containerId, Inventory playerInventory, @Nullable IStackHandler storage, NetPumpBlockEntity be)
    {
        super(Net_Pump_Menu.get(), containerId, playerInventory);

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
        synchronization = commands().inventory(BDMenuResources.bind(this, SCHEMA)).action(SET_FILTER).action(SET_REDSTONE);
    }

    private static boolean editable(NetPumpMenu menu) { return !menu.player.isSpectator(); }

    private void markBoardChanged()
    {
        be.setChanged();
        player.level().sendBlockUpdated(be.getBlockPos(), be.getBlockState(), be.getBlockState(), 2);
    }

    // 客户端读取接口：本页只暴露自己这两个模式

    public FilterMode filter() { return clientFilter; }
    public RedStoneControlMode redstone() { return clientRedstone; }
    public boolean filterEditable() { return filterEditable; }
    public boolean redstoneEditable() { return redstoneEditable; }
    public boolean ready() { return synchronization.hasSnapshot(); }

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
