package com.wintercogs.beyonddimensions.common.menu;

import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.api.storage.handler.IStackHandler;
import com.wintercogs.beyonddimensions.api.storage.handler.impl.StackHandler;
import com.wintercogs.beyonddimensions.api.storage.key.IStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.api.storage.key.impl.ItemStackKey;
import com.wintercogs.beyonddimensions.client.gui.CommonTextures;
import com.wintercogs.beyonddimensions.common.init.BDDataComponents;
import com.wintercogs.beyonddimensions.common.machine.FeederMode;
import com.wintercogs.beyonddimensions.common.machine.RedStoneControlMode;
import com.wintercogs.beyonddimensions.common.menu.widget.slot.FlagStackTypedSlot;
import dev.composemc.forge.sync.MenuAction;
import dev.composemc.forge.sync.MenuSync;
import dev.composemc.sync.state.SyncCodecs;
import dev.composemc.sync.state.SyncSchema;
import net.minecraft.core.registries.Registries;
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

/** 网络喂食器：设置保存在物品组件上，只同步工作状态与喂食模式。 */
public class NetFeederMenu extends BDBaseMenu
{
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(Registries.MENU, BDConstants.MODID);
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
                    && itemStackKey.getReadOnlyStack().getFoodProperties(player) != null;
        }
    };
    private boolean initialized; //initialized必须在初始数据提供完成之后才能设置为true

    public final ItemStack menuStack;

    // 以下字段仅用于客户端镜像，由同步schema的setter写入
    private RedStoneControlMode clientWorking = RedStoneControlMode.IGNORE;
    private boolean workingEditable;
    private FeederMode clientFeeding = FeederMode.NORMAL;
    private boolean feedingEditable;

    private static final List<RedStoneControlMode> WORKING_OPTIONS = List.of(RedStoneControlMode.IGNORE, RedStoneControlMode.NOT_WORKING);

    private static final SyncSchema<NetFeederMenu> SCHEMA = SyncSchema.<NetFeederMenu>builder("beyonddimensions:feeder", 1)
            .field("value.working", SyncCodecs.enumeration(RedStoneControlMode.class),
                    m -> m.player.level().isClientSide() ? m.clientWorking : m.menuStack.getOrDefault(BDDataComponents.CONTROL_MODE, RedStoneControlMode.IGNORE),
                    (m, value) -> m.clientWorking = value)
            .field("editable.working", SyncCodecs.BOOLEAN,
                    m -> m.player.level().isClientSide() ? m.workingEditable : editable(m),
                    (m, value) -> m.workingEditable = value)
            .field("value.feeding", SyncCodecs.enumeration(FeederMode.class),
                    m -> m.player.level().isClientSide() ? m.clientFeeding : m.menuStack.getOrDefault(BDDataComponents.FEEDER_MODE, FeederMode.NORMAL),
                    (m, value) -> m.clientFeeding = value)
            .field("editable.feeding", SyncCodecs.BOOLEAN,
                    m -> m.player.level().isClientSide() ? m.feedingEditable : editable(m),
                    (m, value) -> m.feedingEditable = value)
            .build();

    private static final MenuAction<NetFeederMenu, RedStoneControlMode> SET_WORKING = MenuAction.of("set.working",
            SyncCodecs.enumeration(RedStoneControlMode.class), (m, player, value) -> {
                if (!editable(m) || !WORKING_OPTIONS.contains(value)) return false;
                var current = m.menuStack.getOrDefault(BDDataComponents.CONTROL_MODE, RedStoneControlMode.IGNORE);
                if (current != value) { m.menuStack.set(BDDataComponents.CONTROL_MODE, value); m.markItemChanged(); }
                return true;
            });

    private static final MenuAction<NetFeederMenu, FeederMode> SET_FEEDING = MenuAction.of("set.feeding",
            SyncCodecs.enumeration(FeederMode.class), (m, player, value) -> {
                if (!editable(m)) return false;
                var current = m.menuStack.getOrDefault(BDDataComponents.FEEDER_MODE, FeederMode.NORMAL);
                if (current != value) { m.menuStack.set(BDDataComponents.FEEDER_MODE, value); m.markItemChanged(); }
                return true;
            });

    private final MenuSync<NetFeederMenu> synchronization;

    @Override public MenuSync<NetFeederMenu> menuSync() { return synchronization; }

    public NetFeederMenu(int id, Inventory playerInventory, FriendlyByteBuf data)
    {
        this(id, playerInventory, playerInventory.player.getItemInHand(data.readEnum(InteractionHand.class)));
    }

    public NetFeederMenu(int containerId, Inventory playerInventory, ItemStack menuStack)
    {
        super(Net_Feeder_Menu.get(), containerId, playerInventory);
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
        synchronization = commands().inventory(BDMenuResources.bind(this, SCHEMA)).action(SET_WORKING).action(SET_FEEDING);
    }

    private static boolean editable(NetFeederMenu menu) { return !menu.player.isSpectator(); }

    private void markItemChanged() { player.getInventory().setChanged(); }

    // 客户端读取接口：本页只暴露自己这两个模式

    public RedStoneControlMode working() { return clientWorking; }
    public FeederMode feeding() { return clientFeeding; }
    public boolean workingEditable() { return workingEditable; }
    public boolean feedingEditable() { return feedingEditable; }

    public boolean ready() { return synchronization.hasSnapshot(); }

    public boolean requestWorking(int ordinal)
    {
        return workingEditable && ordinal >= 0 && ordinal < WORKING_OPTIONS.size()
                && synchronization.request(SET_WORKING, WORKING_OPTIONS.get(ordinal)).queued();
    }

    public boolean requestFeeding(int ordinal)
    {
        FeederMode[] modes = FeederMode.values();
        return feedingEditable && ordinal >= 0 && ordinal < modes.length && synchronization.request(SET_FEEDING, modes[ordinal]).queued();
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
