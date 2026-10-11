package com.wintercogs.beyonddimensions.common.menu;

import com.wintercogs.beyonddimensions.api.machine.Machine;
import com.wintercogs.beyonddimensions.api.menu.BdMenuActions;
import com.wintercogs.beyonddimensions.api.storage.handler.IStackHandler;
import com.wintercogs.beyonddimensions.api.storage.handler.impl.StackHandler;
import com.wintercogs.beyonddimensions.common.block.entity.NetPumpBlockEntity;
import com.wintercogs.beyonddimensions.common.menu.machine.MachineMenu;
import com.wintercogs.beyonddimensions.common.menu.machine.MachineSettingsSync;
import com.wintercogs.beyonddimensions.common.menu.sync.BDMenuResources;
import com.wintercogs.beyonddimensions.common.menu.widget.slot.FlagStackTypedSlot;
import dev.compixel.forge.sync.MenuAction;
import dev.compixel.forge.sync.MenuSync;
import dev.compixel.sync.state.SyncSchema;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.wintercogs.beyonddimensions.common.init.BDMenus.Net_Pump_Menu;

/**
 * 维度网络泵：过滤槽与玩家背包
 */
public class NetPumpMenu extends BDBaseMenu implements MachineMenu
{
    private final IStackHandler storage;

    public final NetPumpBlockEntity be;

    private static final SyncSchema<NetPumpMenu> SCHEMA =
            MachineSettingsSync.field(SyncSchema.<NetPumpMenu>builder("beyonddimensions:pump", 2)).build();

    private static final MenuAction<NetPumpMenu, MachineSettingsSync.Change> SET_SETTING = MachineSettingsSync.action();

    private final MachineSettingsSync.Mirror machineSettings = new MachineSettingsSync.Mirror();

    private final MenuSync<NetPumpMenu> synchronization;

    @Override
    public MenuSync<NetPumpMenu> menuSync()
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

    /**
     * 客户端：把设置改成编辑声明里的第 index 个值
     */
    public boolean requestSetting(ResourceLocation setting, int index)
    {
        return MachineSettingsSync.request(this, synchronization, SET_SETTING, setting, index);
    }

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
        synchronization = BdMenuActions.attach(this, commands().inventory(BDMenuResources.bind(this, SCHEMA)).action(SET_SETTING));
    }

    private void addFlagSlots()
    {
        for (int row = 0; row < 4; row++)
        {
            for (int col = 0; col < 9; col++)
            {
                FlagStackTypedSlot flagSlot = new FlagStackTypedSlot(this, storage, row * 9 + col);
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
