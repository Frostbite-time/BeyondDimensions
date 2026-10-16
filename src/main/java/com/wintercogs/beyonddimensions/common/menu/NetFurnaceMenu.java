package com.wintercogs.beyonddimensions.common.menu;

import com.wintercogs.beyonddimensions.api.machine.Machine;
import com.wintercogs.beyonddimensions.api.menu.BdMenuActions;
import com.wintercogs.beyonddimensions.api.storage.handler.IStackHandler;
import com.wintercogs.beyonddimensions.api.storage.handler.impl.StackHandler;
import com.wintercogs.beyonddimensions.common.block.entity.BaseNetFurnaceBlockEntity;
import com.wintercogs.beyonddimensions.common.menu.machine.MachineMenu;
import com.wintercogs.beyonddimensions.common.menu.machine.MachineSettingsSync;
import com.wintercogs.beyonddimensions.common.menu.sync.BDMenuResources;
import com.wintercogs.beyonddimensions.common.menu.widget.slot.FlagStackTypedSlot;
import com.wintercogs.beyonddimensions.common.menu.widget.slot.OrderedStackTypedSlot;
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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static com.wintercogs.beyonddimensions.common.init.BDMenus.Net_Furnace_Menu;

/**
 * 维度网络熔炉：九条产线的实时进度
 */
public class NetFurnaceMenu extends BDBaseMenu implements MachineMenu
{

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

    private static final SyncSchema<NetFurnaceMenu> SCHEMA = MachineSettingsSync.field(SyncSchema.<NetFurnaceMenu>builder("beyonddimensions:furnace", 2))
            .keyedCollection("lanes", SyncCodecs.INT, LANE_CODEC, Lane::index, NetFurnaceMenu::captureLanes, (m, value) -> m.lanes = value)
            .build();

    private static final MenuAction<NetFurnaceMenu, MachineSettingsSync.Change> SET_SETTING = MachineSettingsSync.action();

    private final MachineSettingsSync.Mirror machineSettings = new MachineSettingsSync.Mirror();

    private final MenuSync<NetFurnaceMenu> synchronization;

    @Override
    public MenuSync<NetFurnaceMenu> menuSync()
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
     * 客户端：按控件给出的值修改设置，见 {@link MachineSettingsSync#valueAt}
     */
    public boolean requestSetting(ResourceLocation setting, long pick)
    {
        return MachineSettingsSync.request(this, synchronization, SET_SETTING, setting, pick);
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
        synchronization = BdMenuActions.attach(this, commands().inventory(BDMenuResources.bind(this, SCHEMA)).action(SET_SETTING));
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
            FlagStackTypedSlot flagSlot = new FlagStackTypedSlot(this, inputFilterSlots, i);
            this.addSlot(flagSlot);
        }
        fuelFilterStart = slots.size();
        for (int i = 0; i < 8; i++)
        {
            FlagStackTypedSlot flagSlot = new FlagStackTypedSlot(this, fuelFilterSlots, i);
            this.addSlot(flagSlot);
        }
    }

    private void addStorageSlots()
    {
        inputStorageStart = slots.size();
        vanillaQuickMoveStartIndex = slots.size();
        for (int i = 0; i < 9; i++)
        {
            OrderedStackTypedSlot storageSlot = new OrderedStackTypedSlot(this, inputStorageSlots, i, inventoryStartIndex, inventoryEndIndex, 0, 0);
            this.addSlot(storageSlot);
        }
        //燃料
        fuelStorageIndex = slots.size();
        this.addSlot(new OrderedStackTypedSlot(this, fuelStorageSlots, 0, inventoryStartIndex, inventoryEndIndex, 0, 0));
        vanillaQuickMoveEndIndex = slots.size();
        // 燃料返回物槽
        fuelReturnIndex = slots.size();
        this.addSlot(new OrderedStackTypedSlot(this, fuelReturnSlots, 0, inventoryStartIndex, inventoryEndIndex, 0, 0));
        // 输出槽
        outputStorageStart = slots.size();
        for (int i = 0; i < 9; i++)
        {
            OrderedStackTypedSlot storageSlot = new OrderedStackTypedSlot(this, outputStorageSlots, i, inventoryStartIndex, inventoryEndIndex, 0, 0);
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
