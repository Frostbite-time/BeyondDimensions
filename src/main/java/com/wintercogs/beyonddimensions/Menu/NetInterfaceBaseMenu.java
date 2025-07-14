package com.wintercogs.beyonddimensions.Menu;

import com.wintercogs.beyonddimensions.Api.DataBase.Handler.StackTypedHandler;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.IStackType;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.ItemStackType;
import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.BlockEntity.Custom.NetInterfaceBlockEntity;
import com.wintercogs.beyonddimensions.GUI.CommonTextures;
import com.wintercogs.beyonddimensions.Menu.Slot.FlagStackTypedSlot;
import com.wintercogs.beyonddimensions.Menu.Slot.OrderedStackTypedSlot;
import com.wintercogs.beyonddimensions.Packet.PopModeButtonPacket;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

// 网络接口的UI
// 管理一组虚拟槽、以及一组
public class NetInterfaceBaseMenu extends BDOrderedContainerMenu
{
    private static final int slotStartY = 1 + CommonTextures.TOP_BASE_COMMON_HEIGHT;
    private static final int invSlotStartY = 6 + slotStartY + CommonTextures.COMMON_SLOTS_HEIGHT*3 + CommonTextures.FILTER_SLOTS_HEIGHT*3 + CommonTextures.COMMON_CONNECTION_HEIGHT;


    public final StackTypedHandler flagStorage;

    public boolean popMode;
    public NetInterfaceBlockEntity be;


    // 构建注册用的信息
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(Registries.MENU, BeyondDimensions.MODID);
    public static final Supplier<MenuType<NetInterfaceBaseMenu>> Net_Interface_Menu = MENU_TYPES.register("net_interface_menu", () -> IMenuTypeExtension.create(NetInterfaceBaseMenu::new));
    // 我们的辅助函数
    // 我们需要通过IMenuTypeExtension的.create方法才能返回一个menutype，
    // create方法需要传入一个IContainerFactory的内容，而正好我们的构造函数就是IContainerFactory一样的参数。
    // 因为就是这样设计的， 所以传入new就可以了。


    /**
     * 客户端构造函数
     * @param playerInventory 玩家背包
     */
    public NetInterfaceBaseMenu(int id, Inventory playerInventory, FriendlyByteBuf data)
    {
        this(id, playerInventory, new StackTypedHandler(27),new StackTypedHandler(27),null,new SimpleContainerData(0));
    }

    /**
     * 服务端构造函数
     * @param playerInventory 玩家背包
     * @param uselessContainer 此处无用，传入new SimpleContainerData(0)即可
     */
    public NetInterfaceBaseMenu(int id, Inventory playerInventory, StackTypedHandler storage , StackTypedHandler flagStorage, NetInterfaceBlockEntity be, SimpleContainerData uselessContainer)
    {
        super(Net_Interface_Menu.get(), id,playerInventory,storage);

        this.popMode = false;
        // 初始化标记容器
        this.flagStorage = flagStorage;
        if(!player.level().isClientSide())
        {
            this.popMode = be.popMode;
            this.be = be;
        }

        addPlayerInv(playerInventory);
        addStorageSlots();
        addFlagSlots();

    }

    private void addStorageSlots()
    {
        // 添加存储槽
        for(int row = 0; row < 3; row++)
        {
            for (int col = 0; col < 9; col++)
            {
                this.addSlot(new OrderedStackTypedSlot(this,storage, row*9+col,inventoryStartIndex,inventoryEndIndex, 8 + col * 18, slotStartY + 18 + row * 36));
            }
        }

    }

    private void addFlagSlots()
    {
        // 添加标记槽
        for(int row = 0; row < 3; row++)
        {
            for (int col = 0; col < 9; col++)
            {
                FlagStackTypedSlot flagSlot = new FlagStackTypedSlot(this, flagStorage, row*9+col, 8 + col * 18, slotStartY + row * 36);
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
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18,  4+invSlotStartY + 3 * 18));
        }
        inventoryEndIndex = slots.size();
    }


    @Override
    protected void updateChange()
    {

    }

    @Override
    protected void initUpdate()
    {
        PacketDistributor.sendToPlayer((ServerPlayer) player,new PopModeButtonPacket(popMode));
    }


    @Override
    public boolean stillValid(Player player)
    {
        return be != null && !be.isRemoved();
    }


    // 用于设置虚拟槽位的函数
    public void setFlagSlot(int slotIndex, IStackType clickStack, IStackType flagStack)
    {
        FlagStackTypedSlot slot = (FlagStackTypedSlot) this.slots.get(slotIndex);// clickHandle仅用于处理点击维度槽位的逻辑，如果转换失败，则证明调用逻辑出错

        // 处理虚拟槽位
        if(slot.isFake())
        {
            if(flagStack.isEmpty()&&getCarried().isEmpty())
            {
                flagStorage.setStackDirectly(slot.getSlotIndex(), new ItemStackType());
            }
            else
            {
                flagStorage.setStackDirectly(slot.getSlotIndex(),flagStack);
            }
            return; // 结束处理
        }
    }

}
