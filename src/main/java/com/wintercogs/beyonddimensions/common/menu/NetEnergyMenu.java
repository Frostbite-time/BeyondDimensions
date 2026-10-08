package com.wintercogs.beyonddimensions.common.menu;

import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet;
import com.wintercogs.beyonddimensions.api.dimensionnet.UnifiedStorage;
import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.api.menu.BdMenuActions;
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

/**
 * 维度网络能量通道：只同步本页自己的两个模式与三个能量读数。
 */
public class NetEnergyMenu extends BDBaseMenu
{
    // 构建注册用的信息
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(Registries.MENU, BDConstants.MODID);
    public static final Supplier<MenuType<NetEnergyMenu>> Net_Energy_Menu = MENU_TYPES.register("net_energy_menu", () -> IMenuTypeExtension.create(NetEnergyMenu::new));

    public final NetEnergyPathwayBlockEntity be;

    private PopMode popMode = PopMode.STOP;
    private RedStoneControlMode redStoneMode = RedStoneControlMode.IGNORE;
    private long energyStored = 0;
    private long energyCapacity = 0;
    private long energyRate = 0;
    private boolean sampledEnergy;


    private static final SyncSchema<NetEnergyMenu> SCHEMA = SyncSchema.<NetEnergyMenu>builder("beyonddimensions:energy", 1)
            .field("value.output", SyncCodecs.enumeration(PopMode.class),
                    m -> m.popMode,
                    (m, value) -> m.popMode = value)
            .field("value.redstone", SyncCodecs.enumeration(RedStoneControlMode.class),
                    m -> m.redStoneMode,
                    (m, value) -> m.redStoneMode = value)
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

    private static final MenuAction<NetEnergyMenu, PopMode> SET_OUTPUT = MenuAction.of("set.output",
            SyncCodecs.enumeration(PopMode.class), (m, player, value) -> {
                if (!editable(m) || m.be == null) return false;
                if (m.be.getPopMode() != value)
                {
                    m.be.setPopMode(value);
                    m.markBoardChanged();
                }
                return true;
            });

    private static final MenuAction<NetEnergyMenu, RedStoneControlMode> SET_REDSTONE = MenuAction.of("set.redstone",
            SyncCodecs.enumeration(RedStoneControlMode.class), (m, player, value) -> {
                if (!editable(m) || m.be == null) return false;
                if (m.be.controlMode != value)
                {
                    m.be.controlMode = value;
                    m.markBoardChanged();
                }
                return true;
            });

    private final MenuSync<NetEnergyMenu> synchronization;

    @Override
    public MenuSync<NetEnergyMenu> menuSync()
    {
        return synchronization;
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
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 93 + row * 18));
            }
        }

        for (int col = 0; col < 9; ++col)
        {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 151));
        }
        inventoryEndIndex = slots.size();

        synchronization = BdMenuActions.attach(this, MenuSync.bind(this, SCHEMA).action(SET_OUTPUT).action(SET_REDSTONE));
    }

    /**
     * 更新服务端状态
     */
    @Override
    public void broadcastChanges()
    {
        super.broadcastChanges();

        if (player.level().isClientSide()) return;

        this.popMode = be.getPopMode();
        this.redStoneMode = be.getControlMode();

        DimensionsNet net = be.getNet();

        long stored = net == null ? 0 : getEnergyStored(net.getUnifiedStorage());
        this.energyRate = sampledEnergy ? stored - this.energyStored : 0;
        this.energyStored = stored;
        sampledEnergy = true;
        this.energyCapacity = net == null ? 0 : net.getUnifiedStorage().getSlotCapacity(0);
    }

    private static boolean editable(NetEnergyMenu menu)
    {
        return !menu.player.isSpectator();
    }

    private void markBoardChanged()
    {
        be.setChanged();
        player.level().invalidateCapabilities(be.getBlockPos());
        player.level().sendBlockUpdated(be.getBlockPos(), be.getBlockState(), be.getBlockState(), 2);
    }

    // 客户端读取接口：本页只暴露自己这两个模式的当前值

    public PopMode popMode()
    {
        return popMode;
    }

    public RedStoneControlMode redStoneMode()
    {
        return redStoneMode;
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

    public boolean requestOutput(int ordinal)
    {
        PopMode[] modes = PopMode.values();
        return ordinal >= 0 && ordinal < modes.length && synchronization.request(SET_OUTPUT, modes[ordinal]).queued();
    }

    public boolean requestRedstone(int ordinal)
    {
        RedStoneControlMode[] modes = RedStoneControlMode.values();
        return ordinal >= 0 && ordinal < modes.length && synchronization.request(SET_REDSTONE, modes[ordinal]).queued();
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
