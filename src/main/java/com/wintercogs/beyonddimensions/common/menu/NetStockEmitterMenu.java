package com.wintercogs.beyonddimensions.common.menu;

import com.wintercogs.beyonddimensions.api.machine.Machine;
import com.wintercogs.beyonddimensions.api.menu.BdMenuActions;
import com.wintercogs.beyonddimensions.api.storage.handler.IStackHandler;
import com.wintercogs.beyonddimensions.api.storage.handler.impl.StackHandler;
import com.wintercogs.beyonddimensions.common.block.entity.NetStockEmitterBlockEntity;
import com.wintercogs.beyonddimensions.common.menu.machine.MachineMenu;
import com.wintercogs.beyonddimensions.common.menu.machine.MachineSettingsSync;
import com.wintercogs.beyonddimensions.common.menu.sync.BDMenuResources;
import com.wintercogs.beyonddimensions.common.menu.widget.slot.FlagStackTypedSlot;
import dev.compixel.forge.sync.MenuAction;
import dev.compixel.forge.sync.MenuSync;
import dev.compixel.sync.state.SyncCodecs;
import dev.compixel.sync.state.SyncSchema;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.wintercogs.beyonddimensions.common.init.BDMenus.Net_Stock_Emitter_Menu;

/**
 * 存量发信器：一个标记槽与玩家背包，同步设定值、网络里的数量与是否在发信
 */
public class NetStockEmitterMenu extends BDBaseMenu implements MachineMenu
{
    private final IStackHandler storage;

    public final NetStockEmitterBlockEntity be;

    // 客户端收到的值；服务端直接读方块实体
    private long threshold;
    private long amount;
    private boolean emitting;

    private static final SyncSchema<NetStockEmitterMenu> SCHEMA =
            MachineSettingsSync.field(SyncSchema.<NetStockEmitterMenu>builder("beyonddimensions:stock_emitter", 1))
                    .field("value_threshold", SyncCodecs.LONG,
                            m -> m.player.level().isClientSide() ? m.threshold : m.be.getThreshold(),
                            (m, value) -> m.threshold = value)
                    .field("value_amount", SyncCodecs.LONG,
                            m -> m.player.level().isClientSide() ? m.amount : m.be.amount(),
                            (m, value) -> m.amount = value)
                    .field("value_emitting", SyncCodecs.BOOLEAN,
                            m -> m.player.level().isClientSide() ? m.emitting : m.be.shouldEmit(),
                            (m, value) -> m.emitting = value)
                    .build();

    private static final MenuAction<NetStockEmitterMenu, MachineSettingsSync.Change> SET_SETTING = MachineSettingsSync.action();

    private static final MenuAction<NetStockEmitterMenu, Long> SET_THRESHOLD = MenuAction.of("set_threshold",
            SyncCodecs.LONG, (m, player, value) -> {
                if (player.isSpectator() || value < 0)
                    return false;
                m.be.setThreshold(value);
                return true;
            });

    private final MachineSettingsSync.Mirror machineSettings = new MachineSettingsSync.Mirror();

    private final MenuSync<NetStockEmitterMenu> synchronization;

    @Override
    public MenuSync<NetStockEmitterMenu> menuSync()
    {
        return synchronization;
    }

    @Override
    public @Nullable Machine machine()
    {
        return be == null || be.isRemoved() ? null : be;
    }

    @Override
    public MachineSettingsSync.Mirror machineSettings()
    {
        return machineSettings;
    }

    public NetStockEmitterMenu(int id, Inventory playerInventory, FriendlyByteBuf data)
    {
        this(id, playerInventory, new StackHandler(1), (NetStockEmitterBlockEntity) playerInventory.player.level().getBlockEntity(data.readBlockPos()));
    }

    public NetStockEmitterMenu(int containerId, Inventory playerInventory, IStackHandler storage, NetStockEmitterBlockEntity be)
    {
        super(Net_Stock_Emitter_Menu.get(), containerId, playerInventory);

        this.be = be;
        this.storage = playerInventory.player.level().isClientSide() ? new StackHandler(1) : storage;

        addPlayerInv(playerInventory);
        this.addSlot(new FlagStackTypedSlot(this, this.storage, 0));
        synchronization = BdMenuActions.attach(this,
                commands().inventory(BDMenuResources.bind(this, SCHEMA)).action(SET_SETTING).action(SET_THRESHOLD));
    }

    /**
     * 客户端：把设置改成编辑声明里的第 index 个值
     */
    public boolean requestSetting(ResourceLocation setting, int index)
    {
        return MachineSettingsSync.request(this, synchronization, SET_SETTING, setting, index);
    }

    /**
     * 客户端：修改设定值
     */
    public boolean requestThreshold(long value)
    {
        return value >= 0 && synchronization.request(SET_THRESHOLD, value).queued();
    }

    public long threshold()
    {
        return threshold;
    }

    /**
     * 标记的资源在网络里的数量
     */
    public long amount()
    {
        return amount;
    }

    public boolean emitting()
    {
        return emitting;
    }

    private void addPlayerInv(Inventory playerInventory)
    {
        // 添加背包以及快捷栏
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
        return be != null && !be.isRemoved();
    }
}
