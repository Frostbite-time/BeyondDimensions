package com.wintercogs.beyonddimensions.common.menu;

import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.api.storage.handler.impl.StackHandler;
import com.wintercogs.beyonddimensions.client.gui.CommonTextures;
import com.wintercogs.beyonddimensions.common.machine.FuzzyMode;
import com.wintercogs.beyonddimensions.common.machine.PopMode;
import com.wintercogs.beyonddimensions.common.machine.RedStoneControlMode;
import com.wintercogs.beyonddimensions.common.menu.sync.BDMenuCommands;
import com.wintercogs.beyonddimensions.common.menu.sync.BDMenuResources;
import com.wintercogs.beyonddimensions.common.menu.widget.slot.FlagStackTypedSlot;
import com.wintercogs.beyonddimensions.common.menu.widget.slot.OrderedStackTypedSlot;
import dev.compixel.forge.sync.MenuSync;
import dev.compixel.slots.SlotTransferRoutes;
import dev.compixel.sync.state.SyncCodecs;
import dev.compixel.sync.state.SyncSchema;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

// 网络接口的UI
// 管理一组虚拟槽、以及一组标记槽
public class NetInterfaceBaseMenu extends BDBaseMenu
{
    /**
     * applyMode 的设置项
     */
    public static final int SETTING_POP = 0;
    public static final int SETTING_REDSTONE = 1;
    public static final int SETTING_FUZZY = 2;

    // 客户端镜像，由同步 schema 写入
    public PopMode synchronizedPopMode = PopMode.STOP;
    public RedStoneControlMode synchronizedControlMode = RedStoneControlMode.IGNORE;
    public FuzzyMode synchronizedFuzzyMode = FuzzyMode.DISABLE;
    public boolean synchronizedCanConfigurePop;

    private static final SyncSchema<NetInterfaceBaseMenu> SYNC_SCHEMA = SyncSchema.<NetInterfaceBaseMenu>builder("beyonddimensions:interface", 1)
            .field("pop", SyncCodecs.enumeration(PopMode.class),
                    m -> m.player.level().isClientSide() ? m.synchronizedPopMode : m.access.getPopMode(),
                    (m, value) -> m.synchronizedPopMode = value)
            .field("redstone", SyncCodecs.enumeration(RedStoneControlMode.class),
                    m -> m.player.level().isClientSide() ? m.synchronizedControlMode : m.access.getControlMode(),
                    (m, value) -> m.synchronizedControlMode = value)
            .field("fuzzy", SyncCodecs.enumeration(FuzzyMode.class),
                    m -> m.player.level().isClientSide() ? m.synchronizedFuzzyMode : m.access.getFuzzyMode(),
                    (m, value) -> m.synchronizedFuzzyMode = value)
            .field("configurable_pop", SyncCodecs.BOOLEAN,
                    m -> m.player.level().isClientSide() ? m.synchronizedCanConfigurePop : m.access.canConfigurePopMode(),
                    (m, value) -> m.synchronizedCanConfigurePop = value)
            .build();

    private final MenuSync<NetInterfaceBaseMenu> synchronization = commands().inventory(BDMenuResources.bind(this, SYNC_SCHEMA)).action(BDMenuCommands.MODE);

    @Override
    public MenuSync<NetInterfaceBaseMenu> menuSync()
    {
        return synchronization;
    }

    public boolean ready()
    {
        return synchronization.hasSnapshot();
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

    /**
     * 服务端：修改单个设置项。只接受单项修改，从不接受客户端提交的整份机器状态
     */
    public boolean applyMode(int setting, int value)
    {
        if (player.level().isClientSide() || player.containerMenu != this || !player.isAlive() || player.isSpectator() || !stillValid(player))
            return false;
        switch (setting)
        {
            case SETTING_POP ->
            {
                if (!access.canConfigurePopMode() || value < 0 || value >= PopMode.values().length)
                    return false;
                access.setPopMode(PopMode.values()[value]);
            }
            case SETTING_REDSTONE ->
            {
                if (value < 0 || value >= RedStoneControlMode.values().length)
                    return false;
                access.setControlMode(RedStoneControlMode.values()[value]);
            }
            case SETTING_FUZZY ->
            {
                if (value < 0 || value >= FuzzyMode.values().length)
                    return false;
                access.setFuzzyMode(FuzzyMode.values()[value]);
            }
            default ->
            {
                return false;
            }
        }
        access.onMenuDataChanged();
        return true;
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
        synchronizedPopMode = access.getPopMode();
        synchronizedControlMode = access.getControlMode();
        synchronizedFuzzyMode = access.getFuzzyMode();
        synchronizedCanConfigurePop = access.canConfigurePopMode();

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

    private static final class ClientAccess implements NetInterfaceAccess
    {
        private final StackHandler stackHandler;
        private final StackHandler fakeStackHandler;
        private final NetInterfaceSettings settings = new NetInterfaceSettings();
        private RedStoneControlMode controlMode = RedStoneControlMode.IGNORE;

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
        public NetInterfaceSettings getNetInterfaceSettings()
        {
            return this.settings;
        }

        @Override
        public RedStoneControlMode getControlMode()
        {
            return this.controlMode;
        }

        @Override
        public void setControlMode(RedStoneControlMode controlMode)
        {
            this.controlMode = controlMode;
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
