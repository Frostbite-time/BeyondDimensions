package com.wintercogs.beyonddimensions.common.menu;

import com.wintercogs.beyonddimensions.api.machine.Machine;
import com.wintercogs.beyonddimensions.api.machine.MachineSettings;
import com.wintercogs.beyonddimensions.api.menu.BdMenuActions;
import com.wintercogs.beyonddimensions.api.storage.handler.impl.StackHandler;
import com.wintercogs.beyonddimensions.common.menu.machine.MachineMenu;
import com.wintercogs.beyonddimensions.common.menu.machine.MachineSettingsSync;
import com.wintercogs.beyonddimensions.common.menu.sync.BDMenuResources;
import com.wintercogs.beyonddimensions.common.menu.widget.slot.FlagStackTypedSlot;
import com.wintercogs.beyonddimensions.common.menu.widget.slot.OrderedStackTypedSlot;
import dev.compixel.forge.sync.MenuAction;
import dev.compixel.forge.sync.MenuSync;
import dev.compixel.slots.SlotTransferRoutes;
import dev.compixel.sync.state.SyncSchema;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.wintercogs.beyonddimensions.common.init.BDMenus.Net_Interface_Menu;

// 网络接口的UI
// 管理一组虚拟槽、以及一组标记槽
public class NetInterfaceBaseMenu extends BDBaseMenu implements MachineMenu
{
    private static final SyncSchema<NetInterfaceBaseMenu> SYNC_SCHEMA =
            MachineSettingsSync.field(SyncSchema.<NetInterfaceBaseMenu>builder("beyonddimensions:interface", 2)).build();

    private static final MenuAction<NetInterfaceBaseMenu, MachineSettingsSync.Change> SET_SETTING = MachineSettingsSync.action();

    private final MachineSettingsSync.Mirror machineSettings = new MachineSettingsSync.Mirror();

    private final MenuSync<NetInterfaceBaseMenu> synchronization = BdMenuActions.attach(this, commands().inventory(BDMenuResources.bind(this, SYNC_SCHEMA)).action(SET_SETTING));

    @Override
    public MenuSync<NetInterfaceBaseMenu> menuSync()
    {
        return synchronization;
    }

    @Override
    public @Nullable Machine machine()
    {
        return player.level().isClientSide() || access instanceof ClientAccess ? null : access;
    }

    @Override
    public MachineSettingsSync.Mirror machineSettings()
    {
        return machineSettings;
    }

    /**
     * 客户端：按控件给出的值修改设置，见 {@link MachineSettingsSync#valueAt}
     */
    public boolean requestSetting(ResourceLocation setting, long pick)
    {
        return MachineSettingsSync.request(this, synchronization, SET_SETTING, setting, pick);
    }

    @Override
    protected SlotTransferRoutes createQuickMoveRoutes()
    {
        return SlotTransferRoutes.builder(slots.size())
                .group("player", inventoryStartIndex, inventoryEndIndex)
                .group("storage", vanillaQuickMoveStartIndex, vanillaQuickMoveEndIndex)
                .route("player", "storage")
                .route("storage", "player")
                .build();
    }


    public final StackHandler storage;
    public final StackHandler flagStorage;

    private final NetInterfaceAccess access;

    /**
     * 客户端构造函数
     *
     * @param playerInventory 玩家背包
     */
    public NetInterfaceBaseMenu(int id, Inventory playerInventory, FriendlyByteBuf data)
    {
        this(id, playerInventory, data.readBlockPos());
    }

    public static NetInterfaceBaseMenu fromNetwork(int id, Inventory playerInventory, FriendlyByteBuf data)
    {
        if (data.readableBytes() == Long.BYTES)
        {
            return new NetInterfaceBaseMenu(id, playerInventory, data.readBlockPos());
        }

        if (data.readBoolean())
        {
            return NetInterfaceBaseMenu.mounted(id, playerInventory, data);
        }
        return new NetInterfaceBaseMenu(id, playerInventory, data.readBlockPos());
    }

    public static NetInterfaceBaseMenu mounted(int id, Inventory playerInventory, FriendlyByteBuf data)
    {
        return new NetInterfaceBaseMenu(id, playerInventory, new ClientAccess(data));
    }

    // 打开数据只携带槽位结构，资源内容与设置由菜单同步负责
    public static void writeMountedOpeningData(FriendlyByteBuf data, NetInterfaceAccess access)
    {
        data.writeBoolean(true);
        data.writeVarInt(access.getStackHandler().getSlots());
        data.writeVarInt(access.getFakeStackHandler().getSlots());
    }

    public NetInterfaceBaseMenu(int id, Inventory playerInventory, BlockPos pos)
    {
        this(id, playerInventory, (NetInterfaceAccess) playerInventory.player.level().getBlockEntity(pos));
    }

    /**
     * 服务端构造函数
     *
     * @param playerInventory 玩家背包
     */
    public NetInterfaceBaseMenu(int id, Inventory playerInventory, NetInterfaceAccess access)
    {
        super(Net_Interface_Menu.get(), id, playerInventory);

        // 初始化标记容器（slot负责同步）
        this.storage = access.getStackHandler();
        this.flagStorage = access.getFakeStackHandler();

        this.access = access;

        addPlayerInv(playerInventory);
        addStorageSlots();
        addFlagSlots();

    }

    public NetInterfaceAccess getAccess()
    {
        return this.access;
    }

    private void addStorageSlots()
    {
        // 动态添加存储槽
        vanillaQuickMoveStartIndex = this.slots.size();

        final int slotCount = storage.getSlots();
        for (int i = 0; i < slotCount; i++)
        {
            this.addSlot(new OrderedStackTypedSlot(this, storage, i, inventoryStartIndex, inventoryEndIndex, 0, 0));
        }

        vanillaQuickMoveEndIndex = this.slots.size();
    }

    private void addFlagSlots()
    {
        // 动态添加标记槽
        final int slotCount = flagStorage.getSlots();
        for (int i = 0; i < slotCount; i++)
        {
            this.addSlot(new FlagStackTypedSlot(this, flagStorage, i));
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
        return access != null && access.isMenuValid();
    }

    // 客户端打开动态结构里的接口时的占位：设置由菜单同步
    private static final class ClientAccess implements NetInterfaceAccess
    {
        private final StackHandler stackHandler;
        private final StackHandler fakeStackHandler;

        private ClientAccess(FriendlyByteBuf data)
        {
            // 槽位数来自服务端配置的接口容量，标记槽与资源槽一一对应
            int slots = data.readVarInt();
            int flags = data.readVarInt();
            if (slots < 1 || slots > 27 || flags != slots)
                throw new IllegalArgumentException("Invalid interface slot structure");
            this.stackHandler = new StackHandler(slots);
            this.fakeStackHandler = new StackHandler(flags);
        }

        @Override
        public StackHandler getStackHandler()
        {
            return this.stackHandler;
        }

        @Override
        public StackHandler getFakeStackHandler()
        {
            return this.fakeStackHandler;
        }

        @Override
        public MachineSettings settings()
        {
            return MachineSettings.EMPTY;
        }

        @Override
        public void setSettings(MachineSettings settings)
        {
        }

        @Override
        public boolean canConfigureSides()
        {
            return false;
        }

        @Override
        public boolean isMenuValid()
        {
            return true;
        }

        @Override
        public void onMenuDataChanged()
        {
        }

    }

}
