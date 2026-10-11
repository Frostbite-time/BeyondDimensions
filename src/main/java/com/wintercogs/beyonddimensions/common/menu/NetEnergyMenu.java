package com.wintercogs.beyonddimensions.common.menu;

import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet;
import com.wintercogs.beyonddimensions.api.dimensionnet.UnifiedStorage;
import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.api.machine.Machine;
import com.wintercogs.beyonddimensions.api.menu.BdMenuActions;
import com.wintercogs.beyonddimensions.api.storage.key.impl.EnergyStackKey;
import com.wintercogs.beyonddimensions.common.block.entity.NetEnergyPathwayBlockEntity;
import com.wintercogs.beyonddimensions.common.menu.machine.MachineMenu;
import com.wintercogs.beyonddimensions.common.menu.machine.MachineSettingsSync;
import dev.compixel.forge.sync.MenuAction;
import dev.compixel.forge.sync.MenuSync;
import dev.compixel.sync.state.SyncCodecs;
import dev.compixel.sync.state.SyncSchema;
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

/**
 * 维度网络能量通道：同步三个能量读数
 */
public class NetEnergyMenu extends BDBaseMenu implements MachineMenu
{
    // 构建注册用的信息
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(Registries.MENU, BDConstants.MODID);
    public static final Supplier<MenuType<NetEnergyMenu>> Net_Energy_Menu = MENU_TYPES.register("net_energy_menu", () -> IMenuTypeExtension.create(NetEnergyMenu::new));

    public final NetEnergyPathwayBlockEntity be;

    private long energyStored = 0;
    private long energyCapacity = 0;
    private long energyRate = 0;
    private boolean sampledEnergy;


    private static final SyncSchema<NetEnergyMenu> SCHEMA = MachineSettingsSync.field(SyncSchema.<NetEnergyMenu>builder("beyonddimensions:energy", 2))
            .field("number.stored", SyncCodecs.LONG,
                    m -> m.energyStored,
                    (m, value) -> m.energyStored = value)
            .field("number.capacity", SyncCodecs.LONG,
                    m -> m.energyCapacity,
                    (m, value) -> m.energyCapacity = value)
            .field("number.rate", SyncCodecs.LONG,
                    m -> m.energyRate,
                    (m, value) -> m.energyRate = value)
            .build();

    private static final MenuAction<NetEnergyMenu, MachineSettingsSync.Change> SET_SETTING = MachineSettingsSync.action();

    private final MachineSettingsSync.Mirror machineSettings = new MachineSettingsSync.Mirror();

    private final MenuSync<NetEnergyMenu> synchronization;

    @Override
    public MenuSync<NetEnergyMenu> menuSync()
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

    /**
     * 客户端构造函数
     *
     * @param playerInventory 玩家背包
     */
    public NetEnergyMenu(int id, Inventory playerInventory, FriendlyByteBuf data)
    {
        this(id, playerInventory, (NetEnergyPathwayBlockEntity) playerInventory.player.level().getBlockEntity(data.readBlockPos()));
    }

    /**
     * 服务端构造函数
     *
     * @param playerInventory 玩家背包
     */
    public NetEnergyMenu(int id, Inventory playerInventory, NetEnergyPathwayBlockEntity be)
    {
        super(Net_Energy_Menu.get(), id, playerInventory);

        this.be = be;

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

        synchronization = BdMenuActions.attach(this, MenuSync.bind(this, SCHEMA).action(SET_SETTING));
    }

    /**
     * 更新服务端状态
     */
    @Override
    public void broadcastChanges()
    {
        super.broadcastChanges();

        if (player.level().isClientSide()) return;

        DimensionsNet net = be.getNet();

        long stored = net == null ? 0 : getEnergyStored(net.getUnifiedStorage());
        this.energyRate = sampledEnergy ? stored - this.energyStored : 0;
        this.energyStored = stored;
        sampledEnergy = true;
        this.energyCapacity = net == null ? 0 : net.getUnifiedStorage().getSlotCapacity(0);
    }

    public long energyStored()
    {
        return energyStored;
    }

    public long energyCapacity()
    {
        return energyCapacity;
    }

    public long energyRate()
    {
        return energyRate;
    }

    @Override
    public boolean stillValid(@NotNull Player player)
    {
        return be != null && !be.isRemoved();
    }

    long getEnergyStored(UnifiedStorage storage)
    {
        return storage.getStackByKey(EnergyStackKey.INSTANCE).amount();
    }
}
