package com.wintercogs.beyonddimensions.common.menu;

import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.api.storage.handler.IStackHandler;
import com.wintercogs.beyonddimensions.api.storage.handler.impl.StackHandler;
import com.wintercogs.beyonddimensions.common.block.entity.BaseNetFurnaceBlockEntity;
import com.wintercogs.beyonddimensions.common.machine.AutoSortMode;
import com.wintercogs.beyonddimensions.common.machine.PopMode;
import com.wintercogs.beyonddimensions.common.machine.ReceiveMode;
import com.wintercogs.beyonddimensions.common.machine.RedStoneControlMode;
import com.wintercogs.beyonddimensions.common.menu.sync.BDMenuResources;
import com.wintercogs.beyonddimensions.common.menu.widget.slot.FlagStackTypedSlot;
import com.wintercogs.beyonddimensions.common.menu.widget.slot.OrderedStackTypedSlot;
import dev.compixel.forge.sync.MenuAction;
import dev.compixel.forge.sync.MenuSync;
import dev.compixel.sync.state.SyncCodec;
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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.IntStream;

/**
 * 维度网络熔炉：本页四个模式 + 九条产线的实时进度。
 */
public class NetFurnaceMenu extends BDBaseMenu
{

    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(Registries.MENU, BDConstants.MODID);
    public static final Supplier<MenuType<NetFurnaceMenu>> Net_Furnace_Menu = MENU_TYPES.register("net_furnace_menu", () -> IMenuTypeExtension.create(NetFurnaceMenu::new));

    private static final int invSlotStartY = 128;

    private final IStackHandler inputFilterSlots;
    private final IStackHandler fuelFilterSlots;
    private final IStackHandler inputStorageSlots;
    private final IStackHandler outputStorageSlots;
    private final IStackHandler fuelStorageSlots;
    private final IStackHandler fuelReturnSlots;

    // 各槽位组的起始索引，由构造过程记录，供客户端页面分区显示
    private int inputFilterStart, fuelFilterStart, inputStorageStart, fuelStorageIndex, fuelReturnIndex, outputStorageStart;

    public record Lane(int index, int cooking, int cookingTotal, int burning, int burningTotal)
    {
    }

    private static final SyncCodec<Lane> LANE_CODEC = SyncCodec.of("beyonddimensions:furnace_lane/1",
            (out, lane) -> {
                out.writeInt(lane.index());
                out.writeInt(lane.cooking());
                out.writeInt(lane.cookingTotal());
                out.writeInt(lane.burning());
                out.writeInt(lane.burningTotal());
            },
            in -> {
                var lane = new Lane(in.readInt(), in.readInt(), in.readInt(), in.readInt(), in.readInt());
                if (lane.index() < 0 || lane.index() >= 9 || lane.cooking() < 0 || lane.cookingTotal() < 0 || lane.burning() < 0 || lane.burningTotal() < 0)
                    throw new java.io.IOException("Invalid furnace progress");
                return lane;
            });
    private List<Lane> lanes = IntStream.range(0, 9).mapToObj(i -> new Lane(i, 0, 0, 0, 0)).toList();

    public List<Lane> lanes()
    {
        return lanes;
    }

    private List<Lane> captureLanes()
    {
        if (player.level().isClientSide()) return lanes;
        var current = new ArrayList<Lane>(9);
        for (int i = 0; i < 9; i++)
            current.add(new Lane(i, be.getCookTime().get(i), be.getCookTimeTotal().get(i), be.getLitTime().get(i), be.getLitDuration().get(i)));
        if (!current.equals(lanes)) lanes = List.copyOf(current);
        return lanes;
    }

    public final BaseNetFurnaceBlockEntity<?> be;

    // 以下字段仅用于客户端镜像，由同步schema的setter写入
    private PopMode clientOutput = PopMode.STOP;
    private boolean outputEditable;
    private ReceiveMode clientReceive = ReceiveMode.STOP;
    private boolean receiveEditable;
    private RedStoneControlMode clientRedstone = RedStoneControlMode.IGNORE;
    private boolean redstoneEditable;
    private AutoSortMode clientSorting = AutoSortMode.OPEN;
    private boolean sortingEditable;

    private static final SyncSchema<NetFurnaceMenu> SCHEMA = SyncSchema.<NetFurnaceMenu>builder("beyonddimensions:furnace", 1)
            .field("value.output", SyncCodecs.enumeration(PopMode.class),
                    m -> m.player.level().isClientSide() ? m.clientOutput : m.be.popMode,
                    (m, value) -> m.clientOutput = value)
            .field("editable.output", SyncCodecs.BOOLEAN,
                    m -> m.player.level().isClientSide() ? m.outputEditable : editable(m),
                    (m, value) -> m.outputEditable = value)
            .field("value.receive", SyncCodecs.enumeration(ReceiveMode.class),
                    m -> m.player.level().isClientSide() ? m.clientReceive : m.be.receiveMode,
                    (m, value) -> m.clientReceive = value)
            .field("editable.receive", SyncCodecs.BOOLEAN,
                    m -> m.player.level().isClientSide() ? m.receiveEditable : editable(m),
                    (m, value) -> m.receiveEditable = value)
            .field("value.redstone", SyncCodecs.enumeration(RedStoneControlMode.class),
                    m -> m.player.level().isClientSide() ? m.clientRedstone : m.be.controlMode,
                    (m, value) -> m.clientRedstone = value)
            .field("editable.redstone", SyncCodecs.BOOLEAN,
                    m -> m.player.level().isClientSide() ? m.redstoneEditable : editable(m),
                    (m, value) -> m.redstoneEditable = value)
            .field("value.sorting", SyncCodecs.enumeration(AutoSortMode.class),
                    m -> m.player.level().isClientSide() ? m.clientSorting : m.be.sortMode,
                    (m, value) -> m.clientSorting = value)
            .field("editable.sorting", SyncCodecs.BOOLEAN,
                    m -> m.player.level().isClientSide() ? m.sortingEditable : editable(m),
                    (m, value) -> m.sortingEditable = value)
            .keyedCollection("lanes", SyncCodecs.INT, LANE_CODEC, Lane::index, NetFurnaceMenu::captureLanes, (m, value) -> m.lanes = value)
            .build();

    private static final MenuAction<NetFurnaceMenu, PopMode> SET_OUTPUT = MenuAction.of("set.output",
            SyncCodecs.enumeration(PopMode.class), (m, player, value) -> {
                if (!editable(m) || m.be == null) return false;
                if (m.be.popMode != value)
                {
                    m.be.popMode = value;
                    m.markBoardChanged();
                }
                return true;
            });

    private static final MenuAction<NetFurnaceMenu, ReceiveMode> SET_RECEIVE = MenuAction.of("set.receive",
            SyncCodecs.enumeration(ReceiveMode.class), (m, player, value) -> {
                if (!editable(m) || m.be == null) return false;
                if (m.be.receiveMode != value)
                {
                    m.be.receiveMode = value;
                    m.markBoardChanged();
                }
                return true;
            });

    private static final MenuAction<NetFurnaceMenu, RedStoneControlMode> SET_REDSTONE = MenuAction.of("set.redstone",
            SyncCodecs.enumeration(RedStoneControlMode.class), (m, player, value) -> {
                if (!editable(m) || m.be == null) return false;
                if (m.be.controlMode != value)
                {
                    m.be.controlMode = value;
                    m.markBoardChanged();
                }
                return true;
            });

    private static final MenuAction<NetFurnaceMenu, AutoSortMode> SET_SORTING = MenuAction.of("set.sorting",
            SyncCodecs.enumeration(AutoSortMode.class), (m, player, value) -> {
                if (!editable(m) || m.be == null) return false;
                if (m.be.sortMode != value)
                {
                    m.be.sortMode = value;
                    m.markBoardChanged();
                }
                return true;
            });

    private final MenuSync<NetFurnaceMenu> synchronization;

    @Override
    public MenuSync<NetFurnaceMenu> menuSync()
    {
        return synchronization;
    }

    public NetFurnaceMenu(int id, Inventory playerInventory, FriendlyByteBuf data)
    {
        this(id, playerInventory, (BaseNetFurnaceBlockEntity<?>) playerInventory.player.level().getBlockEntity(data.readBlockPos()));
    }

    public NetFurnaceMenu(int containerId, Inventory playerInventory, BaseNetFurnaceBlockEntity<?> be)
    {
        super(Net_Furnace_Menu.get(), containerId, playerInventory);

        this.be = be;

        if (playerInventory.player.level().isClientSide())
        {
            this.inputFilterSlots = new StackHandler(8);
            this.fuelFilterSlots = new StackHandler(8);
            this.inputStorageSlots = new StackHandler(9);
            this.outputStorageSlots = new StackHandler(9);
            this.fuelStorageSlots = new StackHandler(1);
            this.fuelReturnSlots = new StackHandler(1);
        }
        else
        {
            this.inputFilterSlots = be.getInputFilterSlots();
            this.fuelFilterSlots = be.getFuelFilterSlots();
            this.inputStorageSlots = be.getInputStorageSlots();
            this.outputStorageSlots = be.getOutputStorageSlots();
            this.fuelStorageSlots = be.getFuelStorageSlots();
            this.fuelReturnSlots = be.getFuelReturnSlots();
        }

        addPlayerInv(playerInventory);
        addFilterSlots();
        addStorageSlots();
        synchronization = commands().inventory(BDMenuResources.bind(this, SCHEMA))
                .action(SET_OUTPUT).action(SET_RECEIVE).action(SET_REDSTONE).action(SET_SORTING);
    }

    private static boolean editable(NetFurnaceMenu menu)
    {
        return !menu.player.isSpectator();
    }

    private void markBoardChanged()
    {
        be.setChanged();
        player.level().sendBlockUpdated(be.getBlockPos(), be.getBlockState(), be.getBlockState(), 2);
    }

    // 客户端读取接口：本页只暴露自己这四个模式

    public PopMode output()
    {
        return clientOutput;
    }

    public ReceiveMode receive()
    {
        return clientReceive;
    }

    public RedStoneControlMode redstone()
    {
        return clientRedstone;
    }

    public AutoSortMode sorting()
    {
        return clientSorting;
    }

    public boolean outputEditable()
    {
        return outputEditable;
    }

    public boolean receiveEditable()
    {
        return receiveEditable;
    }

    public boolean redstoneEditable()
    {
        return redstoneEditable;
    }

    public boolean sortingEditable()
    {
        return sortingEditable;
    }

    public boolean ready()
    {
        return synchronization.hasSnapshot();
    }

    public boolean requestOutput(int ordinal)
    {
        return request(SET_OUTPUT, PopMode.values(), ordinal, outputEditable);
    }

    public boolean requestReceive(int ordinal)
    {
        return request(SET_RECEIVE, ReceiveMode.values(), ordinal, receiveEditable);
    }

    public boolean requestRedstone(int ordinal)
    {
        return request(SET_REDSTONE, RedStoneControlMode.values(), ordinal, redstoneEditable);
    }

    public boolean requestSorting(int ordinal)
    {
        return request(SET_SORTING, AutoSortMode.values(), ordinal, sortingEditable);
    }

    private <E extends Enum<E>> boolean request(MenuAction<NetFurnaceMenu, E> action, E[] modes, int ordinal, boolean enabled)
    {
        return enabled && ordinal >= 0 && ordinal < modes.length && synchronization.request(action, modes[ordinal]).queued();
    }

    // 槽位分组：由本页建立，也由本页回答各自的下标范围

    public List<Integer> inputFilterSlotIds()
    {
        return ids(inputFilterStart, 8);
    }

    public List<Integer> fuelFilterSlotIds()
    {
        return ids(fuelFilterStart, 8);
    }

    public List<Integer> inputStorageSlotIds()
    {
        return ids(inputStorageStart, 9);
    }

    public List<Integer> fuelStorageSlotIds()
    {
        return List.of(fuelStorageIndex);
    }

    public List<Integer> fuelReturnSlotIds()
    {
        return List.of(fuelReturnIndex);
    }

    public List<Integer> outputStorageSlotIds()
    {
        return ids(outputStorageStart, 9);
    }

    private static List<Integer> ids(int start, int count)
    {
        return IntStream.range(start, start + count).boxed().toList();
    }

    private void addFilterSlots()
    {
        inputFilterStart = slots.size();
        for (int i = 0; i < 8; i++)
        {
            FlagStackTypedSlot flagSlot = new FlagStackTypedSlot(this, inputFilterSlots, i, 7, 38 + i * 18);
            this.addSlot(flagSlot);
        }
        fuelFilterStart = slots.size();
        for (int i = 0; i < 8; i++)
        {
            FlagStackTypedSlot flagSlot = new FlagStackTypedSlot(this, fuelFilterSlots, i, 207, 38 + i * 18);
            this.addSlot(flagSlot);
        }
    }

    private void addStorageSlots()
    {
        inputStorageStart = slots.size();
        vanillaQuickMoveStartIndex = slots.size();
        for (int i = 0; i < 9; i++)
        {
            OrderedStackTypedSlot storageSlot = new OrderedStackTypedSlot(this, inputStorageSlots, i, inventoryStartIndex, inventoryEndIndex, 31 + i * 19, 38);
            this.addSlot(storageSlot);
        }
        //燃料
        fuelStorageIndex = slots.size();
        this.addSlot(new OrderedStackTypedSlot(this, fuelStorageSlots, 0, inventoryStartIndex, inventoryEndIndex, 207, 186));
        vanillaQuickMoveEndIndex = slots.size();
        // 燃料返回物槽
        fuelReturnIndex = slots.size();
        this.addSlot(new OrderedStackTypedSlot(this, fuelReturnSlots, 0, inventoryStartIndex, inventoryEndIndex, 7, 186));
        // 输出槽
        outputStorageStart = slots.size();
        for (int i = 0; i < 9; i++)
        {
            OrderedStackTypedSlot storageSlot = new OrderedStackTypedSlot(this, outputStorageSlots, i, inventoryStartIndex, inventoryEndIndex, 31 + i * 19, 90);
            this.addSlot(storageSlot);
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
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 35 + col * 18, invSlotStartY + row * 18));
            }
        }
        for (int col = 0; col < 9; ++col)
        {
            this.addSlot(new Slot(playerInventory, col, 35 + col * 18, 4 + invSlotStartY + 3 * 18));
        }
        inventoryEndIndex = slots.size();
    }

    @Override
    public boolean stillValid(@NotNull Player player)
    {
        return be != null && !be.isRemoved();
    }
}
