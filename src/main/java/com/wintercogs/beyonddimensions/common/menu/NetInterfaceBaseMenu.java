package com.wintercogs.beyonddimensions.common.menu;

import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.api.machine.Machine;
import com.wintercogs.beyonddimensions.api.machine.MachineSettings;
import com.wintercogs.beyonddimensions.api.menu.BdMenuActions;
import com.wintercogs.beyonddimensions.api.storage.handler.impl.StackHandler;
import com.wintercogs.beyonddimensions.client.gui.CommonTextures;
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
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

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
     * 客户端：把设置改成编辑声明里的第 index 个值
     */
    public boolean requestSetting(ResourceLocation setting, int index)
    {
        return MachineSettingsSync.request(this, synchronization, SET_SETTING, setting, index);
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

    private static final int slotStartY = 1 + CommonTextures.TOP_BASE_COMMON_HEIGHT;
    private static final int invSlotStartY = 6 + slotStartY + CommonTextures.COMMON_SLOTS_HEIGHT * 3 + CommonTextures.FILTER_SLOTS_HEIGHT * 3 + CommonTextures.COMMON_CONNECTION_HEIGHT;


    public final StackHandler storage;
    public final StackHandler flagStorage;

    private final NetInterfaceAccess access;

    // 构建注册用的信息
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(Registries.MENU, BDConstants.MODID);
    public static final Supplier<MenuType<NetInterfaceBaseMenu>> Net_Interface_Menu = MENU_TYPES.register("net_interface_menu", () -> IMenuTypeExtension.create(NetInterfaceBaseMenu::fromNetwork));


    /**
     * 客户端构造函数
     *
     * @param playerInventory 玩家背包
     */
    public NetInterfaceBaseMenu(int id, Inventory playerInventory, FriendlyByteBuf data)
    {
        this(id, playerInventory, data.readBlockPos());
    }

    private static NetInterfaceBaseMenu fromNetwork(int id, Inventory playerInventory, FriendlyByteBuf data)
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

    // 打开数据只携带槽位结构，资源内容与设置（包括较大的 NBT）由菜单同步负责
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
        final int cols = 9;                // 每行列数
        final int x0 = 8;                  // 起始 X
        final int y0 = slotStartY + 18;    // 起始 Y（保持原偏移）
        final int dx = 18;                 // 横向间距
        final int dy = 36;                 // 纵向间距（保持原来的 36）

        for (int i = 0; i < slotCount; i++)
        {
            int col = i % cols;
            int row = i / cols;
            int x = x0 + col * dx;
            int y = y0 + row * dy;

            this.addSlot(new OrderedStackTypedSlot(
                    this,
                    storage,
                    i, // 槽索引
                    inventoryStartIndex,
                    inventoryEndIndex,
                    x, y
            ));
        }

        vanillaQuickMoveEndIndex = this.slots.size();
    }

    private void addFlagSlots()
    {
        // 动态添加标记槽
        final int slotCount = flagStorage.getSlots();
        final int cols = 9;             // 每行列数
        final int x0 = 8;               // 起始 X
        final int y0 = slotStartY;      // 起始 Y（保持原定位）
        final int dx = 18;              // 横向间距
        final int dy = 36;              // 纵向间距

        for (int i = 0; i < slotCount; i++)
        {
            int col = i % cols;
            int row = i / cols;
            int x = x0 + col * dx;
            int y = y0 + row * dy;

            this.addSlot(new FlagStackTypedSlot(this, flagStorage, i, x, y));
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
        return access != null && access.isMenuValid();
    }

    // 客户端打开动态结构里的接口时的占位：设置由菜单同步
    private static final class ClientAccess implements NetInterfaceAccess
    {
        private final StackHandler stackHandler;
        private final StackHandler fakeStackHandler;

        private ClientAccess(FriendlyByteBuf data)
        {
            int slots = data.readVarInt();
            int flags = data.readVarInt();
            if (slots != 27 || flags != 27)
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
        public boolean canConfigurePopMode()
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
