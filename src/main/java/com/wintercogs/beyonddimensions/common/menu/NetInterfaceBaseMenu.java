package com.wintercogs.beyonddimensions.common.menu;

import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.api.storage.handler.impl.StackHandler;
import com.wintercogs.beyonddimensions.client.gui.CommonTextures;
import com.wintercogs.beyonddimensions.common.machine.FuzzyMode;
import com.wintercogs.beyonddimensions.common.machine.PopMode;
import com.wintercogs.beyonddimensions.common.machine.RedStoneControlMode;
import com.wintercogs.beyonddimensions.common.menu.widget.slot.FlagStackTypedSlot;
import com.wintercogs.beyonddimensions.common.menu.widget.slot.OrderedStackTypedSlot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

// 网络接口的UI
// 管理一组虚拟槽、以及一组
public class NetInterfaceBaseMenu extends BDBaseMenu
{
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
        return new NetInterfaceBaseMenu(id, playerInventory, new ClientAccess(playerInventory.player.level().registryAccess(), data));
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
    protected boolean shouldSendQuickData()
    {
        // 阻止服务端主动同步，将同步权交给sendBlockUpdated
        return false;
    }

    @Override
    protected void writeQuickDataTag(CompoundTag tag)
    {
        super.writeQuickDataTag(tag);
        tag.putString("popMode", access.getPopMode().name());
        tag.putString("controlMode", access.getControlMode().name());
        tag.putString("fuzzyMode", access.getFuzzyMode().name());
    }

    @Override
    public void readQuickDataTag(CompoundTag tag)
    {
        super.readQuickDataTag(tag);
        if (access == null || !access.isMenuValid())
        {
            return;
        }
        if (access.canConfigurePopMode())
        {
            access.setPopMode(PopMode.valueOf(tag.getString("popMode")));
        }
        access.setControlMode(RedStoneControlMode.valueOf(tag.getString("controlMode")));
        access.setFuzzyMode(FuzzyMode.valueOf(tag.getString("fuzzyMode")));
        // 服务端读取新数据之后利用sendBlockUpdated将数据发送给附近所有玩家
        if (!player.level().isClientSide())
        {
            access.onMenuDataChanged();
        }
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
        private RedStoneControlMode controlMode;

        private ClientAccess(HolderLookup.Provider registries, FriendlyByteBuf data)
        {
            CompoundTag inventory = data.readNbt();
            CompoundTag flags = data.readNbt();
            this.stackHandler = decodeStackHandler(registries, inventory);
            this.fakeStackHandler = decodeStackHandler(registries, flags);
            settings.setPopMode(PopMode.valueOf(data.readUtf()));
            settings.setFuzzyMode(FuzzyMode.valueOf(data.readUtf()));
            this.controlMode = RedStoneControlMode.valueOf(data.readUtf());
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

        private static StackHandler decodeStackHandler(HolderLookup.Provider registries, CompoundTag tag)
        {
            if (tag == null)
            {
                return new StackHandler(0);
            }
            RegistryOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, registries);
            return StackHandler.CODEC.parse(ops, tag)
                    .getOrThrow(IllegalStateException::new);
        }
    }

}
