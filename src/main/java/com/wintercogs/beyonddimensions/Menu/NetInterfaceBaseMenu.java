package com.wintercogs.beyonddimensions.Menu;

import com.wintercogs.beyonddimensions.Api.DataBase.Handler.StackHandler;
import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.BlockEntity.Custom.NetInterfaceBlockEntity;
import com.wintercogs.beyonddimensions.GUI.CommonTextures;
import com.wintercogs.beyonddimensions.Machine.FuzzyMode;
import com.wintercogs.beyonddimensions.Machine.PopMode;
import com.wintercogs.beyonddimensions.Machine.RedStoneControlMode;
import com.wintercogs.beyonddimensions.Menu.Slot.FlagStackTypedSlot;
import com.wintercogs.beyonddimensions.Menu.Slot.OrderedStackTypedSlot;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

// 网络接口的UI
// 管理一组虚拟槽、以及一组
public class NetInterfaceBaseMenu extends BDBaseMenu
{
    private static final int slotStartY = 1 + CommonTextures.TOP_BASE_COMMON_HEIGHT;
    private static final int invSlotStartY = 6 + slotStartY + CommonTextures.COMMON_SLOTS_HEIGHT * 3 + CommonTextures.FILTER_SLOTS_HEIGHT * 3 + CommonTextures.COMMON_CONNECTION_HEIGHT;


    public final StackHandler storage;
    public final StackHandler flagStorage;

    public NetInterfaceBlockEntity be;

    // 构建注册用的信息
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(Registries.MENU, BeyondDimensions.MODID);
    public static final Supplier<MenuType<NetInterfaceBaseMenu>> Net_Interface_Menu = MENU_TYPES.register("net_interface_menu", () -> IMenuTypeExtension.create(NetInterfaceBaseMenu::new));


    /**
     * 客户端构造函数
     *
     * @param playerInventory 玩家背包
     */
    public NetInterfaceBaseMenu(int id, Inventory playerInventory, FriendlyByteBuf data)
    {
        this(id, playerInventory, (NetInterfaceBlockEntity) playerInventory.player.level().getBlockEntity(data.readBlockPos()));
    }

    /**
     * 服务端构造函数
     *
     * @param playerInventory 玩家背包
     */
    public NetInterfaceBaseMenu(int id, Inventory playerInventory, NetInterfaceBlockEntity be)
    {
        super(Net_Interface_Menu.get(), id, playerInventory);

        // 初始化标记容器（slot负责同步）
        this.storage = be.getStackHandler();
        this.flagStorage = be.getFakeStackHandler();

        this.be = be;

        addPlayerInv(playerInventory);
        addStorageSlots();
        addFlagSlots();

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
        tag.putString("popMode", be.popMode.name());
        tag.putString("controlMode", be.controlMode.name());
        tag.putString("fuzzyMode", be.fuzzyMode.name());
    }

    @Override
    public void readQuickDataTag(CompoundTag tag)
    {
        super.readQuickDataTag(tag);
        be.popMode = PopMode.valueOf(tag.getString("popMode"));
        be.controlMode = RedStoneControlMode.valueOf(tag.getString("controlMode"));
        be.fuzzyMode = FuzzyMode.valueOf(tag.getString("fuzzyMode"));
        // 服务端读取新数据之后利用sendBlockUpdated将数据发送给附近所有玩家
        if (!player.level().isClientSide())
        {
            player.level().blockEntityChanged(be.getBlockPos());
            player.level().sendBlockUpdated(be.getBlockPos(), be.getBlockState(), be.getBlockState(), 2);
        }
    }


    @Override
    public boolean stillValid(Player player)
    {
        return be != null && !be.isRemoved();
    }

}
