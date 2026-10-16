package com.wintercogs.beyonddimensions.common.menu;

import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet;
import com.wintercogs.beyonddimensions.api.dimensionnet.UnifiedStorage;
import com.wintercogs.beyonddimensions.api.machine.Machine;
import com.wintercogs.beyonddimensions.api.menu.BdMenuActions;
import com.wintercogs.beyonddimensions.api.storage.key.impl.EnergyStackKey;
import com.wintercogs.beyonddimensions.common.block.entity.NetEnergyPathwayBlockEntity;
import com.wintercogs.beyonddimensions.common.init.BDNetDataTypes;
import com.wintercogs.beyonddimensions.common.menu.machine.MachineMenu;
import com.wintercogs.beyonddimensions.common.menu.machine.MachineSettingsSync;
import com.wintercogs.beyonddimensions.common.menu.state.EnergyHistory;
import dev.compixel.forge.sync.MenuAction;
import dev.compixel.forge.sync.MenuSync;
import dev.compixel.sync.state.SyncCodec;
import dev.compixel.sync.state.SyncCodecs;
import dev.compixel.sync.state.SyncSchema;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import static com.wintercogs.beyonddimensions.common.init.BDMenus.Net_Energy_Menu;

/**
 * 维度网络能量通道：同步存量、容量、最近一秒的速率，以及网络在服务端记下的近期各格
 */
public class NetEnergyMenu extends BDBaseMenu implements MachineMenu
{
    public final NetEnergyPathwayBlockEntity be;

    private long energyStored = 0;
    private long energyCapacity = 0;
    private double energyRate = 0;
    private EnergyHistory.Bars energyBars = EnergyHistory.Bars.EMPTY;

    private static final SyncCodec<Double> RATE_CODEC = SyncCodec.of("beyonddimensions:energy_rate/1",
            DataOutput::writeDouble, DataInput::readDouble);

    private static final SyncCodec<EnergyHistory.Bars> BARS_CODEC = SyncCodec.of("beyonddimensions:energy_bars/1",
            (out, bars) -> {
                out.writeShort(bars.flows().length);
                for (long flow : bars.flows())
                    out.writeLong(flow);
                for (long stored : bars.stored())
                    out.writeLong(stored);
            },
            in -> {
                int count = in.readUnsignedShort();
                if (count > EnergyHistory.BARS)
                    throw new IOException("Too many energy bars: " + count);
                long[] flows = new long[count];
                long[] stored = new long[count];
                for (int i = 0; i < count; i++)
                    flows[i] = in.readLong();
                for (int i = 0; i < count; i++)
                    stored[i] = in.readLong();
                return new EnergyHistory.Bars(flows, stored);
            });

    private static final SyncSchema<NetEnergyMenu> SCHEMA = MachineSettingsSync.field(SyncSchema.<NetEnergyMenu>builder("beyonddimensions:energy", 3))
            .field("number_stored", SyncCodecs.LONG,
                    m -> m.energyStored,
                    (m, value) -> m.energyStored = value)
            .field("number_capacity", SyncCodecs.LONG,
                    m -> m.energyCapacity,
                    (m, value) -> m.energyCapacity = value)
            .field("rate", RATE_CODEC,
                    m -> m.energyRate,
                    (m, value) -> m.energyRate = value)
            .field("bars", BARS_CODEC,
                    m -> m.energyBars,
                    (m, value) -> m.energyBars = value)
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

        this.energyStored = net == null ? 0 : getEnergyStored(net.getUnifiedStorage());
        this.energyCapacity = net == null ? 0 : net.getUnifiedStorage().getSlotCapacity(0);
        EnergyHistory history = net == null ? null : net.getOrCreateData(BDNetDataTypes.ENERGY_HISTORY.get());
        this.energyRate = history == null ? 0 : history.recentRate();
        this.energyBars = history == null ? EnergyHistory.Bars.EMPTY : history.bars();
    }

    public long energyStored()
    {
        return energyStored;
    }

    public long energyCapacity()
    {
        return energyCapacity;
    }

    /**
     * 最近一秒平均每刻的变化，正数为流入
     */
    public double energyRate()
    {
        return energyRate;
    }

    /**
     * 网络近期的各格，从旧到新
     */
    public EnergyHistory.Bars energyBars()
    {
        return energyBars;
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
