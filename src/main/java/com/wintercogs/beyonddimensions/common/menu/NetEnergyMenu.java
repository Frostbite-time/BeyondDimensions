package com.wintercogs.beyonddimensions.common.menu;

import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet;
import com.wintercogs.beyonddimensions.api.dimensionnet.UnifiedStorage;
import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.api.storage.key.impl.EnergyStackKey;
import com.wintercogs.beyonddimensions.common.block.entity.NetEnergyPathwayBlockEntity;
import com.wintercogs.beyonddimensions.common.machine.PopMode;
import com.wintercogs.beyonddimensions.common.machine.RedStoneControlMode;
import dev.compixel.forge.sync.MenuAction;
import dev.compixel.forge.sync.MenuSync;
import dev.compixel.sync.state.SyncCodecs;
import dev.compixel.sync.state.SyncSchema;
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

/** 维度网络能量通道：只同步本页自己的两个模式与三个能量读数。 */
public class NetEnergyMenu extends BDBaseMenu
{
    // 构建注册用的信息
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(Registries.MENU, BDConstants.MODID);
    public static final Supplier<MenuType<NetEnergyMenu>> Net_Energy_Menu = MENU_TYPES.register("net_energy_menu", () -> IMenuTypeExtension.create(NetEnergyMenu::new));
    // 我们的辅助函数
    // 我们需要通过IMenuTypeExtension的.create方法才能返回一个menutype，
    // create方法需要传入一个IContainerFactory的内容，而正好我们的构造函数就是IContainerFactory一样的参数。
    // 因为就是这样设计的， 所以传入new就可以了。

    public final NetEnergyPathwayBlockEntity be;

    /** 服务端每tick采样的网络能量读数。 */
    private long lastEnergyCapacity = 0;
    private long lastEnergyStored = 0;
    private long lastEnergySpeedState = 0;
    private boolean sampledEnergy;

    // 以下字段仅用于客户端镜像，由同步schema的setter写入
    private PopMode clientOutput = PopMode.STOP;
    private RedStoneControlMode clientRedstone = RedStoneControlMode.IGNORE;
    private boolean outputEditable;
    private boolean redstoneEditable;
    private long clientStored;
    private long clientCapacity;
    private long clientRate;

    private static final SyncSchema<NetEnergyMenu> SCHEMA = SyncSchema.<NetEnergyMenu>builder("beyonddimensions:energy", 1)
            .field("value.output", SyncCodecs.enumeration(PopMode.class),
                    m -> m.player.level().isClientSide() ? m.clientOutput : m.be.getPopMode(),
                    (m, value) -> m.clientOutput = value)
            .field("editable.output", SyncCodecs.BOOLEAN,
                    m -> m.player.level().isClientSide() ? m.outputEditable : editable(m),
                    (m, value) -> m.outputEditable = value)
            .field("value.redstone", SyncCodecs.enumeration(RedStoneControlMode.class),
                    m -> m.player.level().isClientSide() ? m.clientRedstone : m.be.controlMode,
                    (m, value) -> m.clientRedstone = value)
            .field("editable.redstone", SyncCodecs.BOOLEAN,
                    m -> m.player.level().isClientSide() ? m.redstoneEditable : editable(m),
                    (m, value) -> m.redstoneEditable = value)
            .field("number.stored", SyncCodecs.LONG,
                    m -> m.player.level().isClientSide() ? m.clientStored : m.lastEnergyStored,
                    (m, value) -> m.clientStored = value)
            .field("number.capacity", SyncCodecs.LONG,
                    m -> m.player.level().isClientSide() ? m.clientCapacity : m.lastEnergyCapacity,
                    (m, value) -> m.clientCapacity = value)
            .field("number.rate", SyncCodecs.LONG,
                    m -> m.player.level().isClientSide() ? m.clientRate : m.lastEnergySpeedState,
                    (m, value) -> m.clientRate = value)
            .build();

    private static final MenuAction<NetEnergyMenu, PopMode> SET_OUTPUT = MenuAction.of("set.output",
            SyncCodecs.enumeration(PopMode.class), (m, player, value) -> {
                if (!editable(m) || m.be == null) return false;
                if (m.be.getPopMode() != value) { m.be.setPopMode(value); m.markBoardChanged(); }
                return true;
            });

    private static final MenuAction<NetEnergyMenu, RedStoneControlMode> SET_REDSTONE = MenuAction.of("set.redstone",
            SyncCodecs.enumeration(RedStoneControlMode.class), (m, player, value) -> {
                if (!editable(m) || m.be == null) return false;
                if (m.be.controlMode != value) { m.be.controlMode = value; m.markBoardChanged(); }
                return true;
            });

    private final MenuSync<NetEnergyMenu> synchronization;

    @Override public MenuSync<NetEnergyMenu> menuSync() { return synchronization; }

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
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 93 + row * 18));
            }
        }

        for (int col = 0; col < 9; ++col)
        {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 151));
        }
        inventoryEndIndex = slots.size();

        synchronization = MenuSync.bind(this, SCHEMA).action(SET_OUTPUT).action(SET_REDSTONE);
    }

    private static boolean editable(NetEnergyMenu menu) { return !menu.player.isSpectator(); }

    private void markBoardChanged()
    {
        be.setChanged();
        player.level().invalidateCapabilities(be.getBlockPos());
        player.level().sendBlockUpdated(be.getBlockPos(), be.getBlockState(), be.getBlockState(), 2);
    }

    // 客户端读取接口：本页只暴露自己这两个模式的当前值

    public PopMode output() { return clientOutput; }
    public RedStoneControlMode redstone() { return clientRedstone; }
    public boolean outputEditable() { return outputEditable; }
    public boolean redstoneEditable() { return redstoneEditable; }
    public long stored() { return clientStored; }
    public long capacity() { return clientCapacity; }
    public long rate() { return clientRate; }

    public boolean ready() { return synchronization.hasSnapshot(); }

    public boolean requestOutput(int ordinal)
    {
        PopMode[] modes = PopMode.values();
        return outputEditable && ordinal >= 0 && ordinal < modes.length && synchronization.request(SET_OUTPUT, modes[ordinal]).queued();
    }

    public boolean requestRedstone(int ordinal)
    {
        RedStoneControlMode[] modes = RedStoneControlMode.values();
        return redstoneEditable && ordinal >= 0 && ordinal < modes.length && synchronization.request(SET_REDSTONE, modes[ordinal]).queued();
    }

    @Override
    protected void updateChange()
    {
        DimensionsNet network = be == null ? null : be.getNet();
        long stored = network == null ? 0 : getEnergyStored(network.getUnifiedStorage());
        lastEnergyCapacity = network == null ? 0 : network.getUnifiedStorage().getSlotCapacity(0);
        lastEnergySpeedState = sampledEnergy ? stored - lastEnergyStored : 0;
        lastEnergyStored = stored;
        sampledEnergy = true;
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
