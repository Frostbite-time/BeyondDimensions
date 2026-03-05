package com.wintercogs.beyonddimensions.common.menu;


import com.wintercogs.beyonddimensions.api.storage.handler.IStackHandler;
import com.wintercogs.beyonddimensions.api.storage.handler.impl.StackHandler;
import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.common.block.entity.NetFurnaceBlockEntity;
import com.wintercogs.beyonddimensions.machine.AutoSortMode;
import com.wintercogs.beyonddimensions.machine.PopMode;
import com.wintercogs.beyonddimensions.machine.ReceiveMode;
import com.wintercogs.beyonddimensions.machine.RedStoneControlMode;
import com.wintercogs.beyonddimensions.common.menu.widget.slot.FlagStackTypedSlot;
import com.wintercogs.beyonddimensions.common.menu.widget.slot.OrderedStackTypedSlot;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public class NetFurnaceMenu extends BDBaseMenu
{

    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(Registries.MENU, BeyondDimensions.MODID);
    public static final Supplier<MenuType<NetFurnaceMenu>> Net_Furnace_Menu = MENU_TYPES.register("net_furnace_menu", () -> IMenuTypeExtension.create(NetFurnaceMenu::new));


    private static final int invSlotStartY = 128;

    private final IStackHandler inputFilterSlots;
    private final IStackHandler fuelFilterSlots;
    private final IStackHandler inputStorageSlots;
    private final IStackHandler outputStorageSlots;
    private final IStackHandler fuelStorageSlots;
    private final IStackHandler fuelReturnSlots;

    // 用于对比上一tick所用的信息缓存
    private List<Integer> lastLitTime = new ArrayList<>();
    private List<Integer> lastLitDuration = new ArrayList<>();
    private List<Integer> lastCookTime = new ArrayList<>();
    private List<Integer> lastCookTimeTotal = new ArrayList<>();

    public final NetFurnaceBlockEntity be;

    public NetFurnaceMenu(int id, Inventory playerInventory, FriendlyByteBuf data)
    {
        this(id, playerInventory, (NetFurnaceBlockEntity) playerInventory.player.level().getBlockEntity(data.readBlockPos()));
    }

    public NetFurnaceMenu(int containerId, Inventory playerInventory, NetFurnaceBlockEntity be)
    {
        super(Net_Furnace_Menu.get(), containerId, playerInventory);

        this.be = be;

        if (playerInventory.player.level().isClientSide())
        {
            this.inputFilterSlots = new StackHandler(be.getFilterCapacity());
            this.fuelFilterSlots = new StackHandler(be.getFilterCapacity());
            this.inputStorageSlots = new StackHandler(be.getCapacity());
            this.outputStorageSlots = new StackHandler(be.getCapacity());
            this.fuelStorageSlots = new StackHandler(be.getFuelCapacity());
            this.fuelReturnSlots = new StackHandler(be.getFuelCapacity());
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

    }

    private void addFilterSlots()
    {
        for (int i = 0; i < 8; i++)
        {
            FlagStackTypedSlot flagSlot = new FlagStackTypedSlot(this, inputFilterSlots, i, 7, 38 + i * 18);
            this.addSlot(flagSlot);
        }
        for (int i = 0; i < 8; i++)
        {
            FlagStackTypedSlot flagSlot = new FlagStackTypedSlot(this, fuelFilterSlots, i, 207, 38 + i * 18);
            this.addSlot(flagSlot);
        }
    }

    private void addStorageSlots()
    {
        vanillaQuickMoveStartIndex = slots.size();
        for (int i = 0; i < 9; i++)
        {
            OrderedStackTypedSlot storageSlot = new OrderedStackTypedSlot(this, inputStorageSlots, i, inventoryStartIndex, inventoryEndIndex, 31 + i * 19, 38);
            this.addSlot(storageSlot);
        }
        //燃料
        this.addSlot(new OrderedStackTypedSlot(this, fuelStorageSlots, 0, inventoryStartIndex, inventoryEndIndex, 207, 186));
        vanillaQuickMoveEndIndex = slots.size();
        // 燃料返回物槽
        this.addSlot(new OrderedStackTypedSlot(this, fuelReturnSlots, 0, inventoryStartIndex, inventoryEndIndex, 7, 186));
        // 输出槽
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

    // 服务端在数值不同时主动发送消息
    @Override
    protected boolean shouldSendQuickData()
    {
        boolean shouldSendQuickData = super.shouldSendQuickData()
                || !Objects.equals(lastLitTime, be.getLitTime())
                || !Objects.equals(lastLitDuration, be.getLitDuration())
                || !Objects.equals(lastCookTime, be.getCookTime())
                || !Objects.equals(lastCookTimeTotal, be.getCookTimeTotal());
        if (shouldSendQuickData)
        {
            lastLitTime = new ArrayList<>(be.getLitTime());
            lastLitDuration = new ArrayList<>(be.getLitDuration());
            lastCookTime = new ArrayList<>(be.getCookTime());
            lastCookTimeTotal = new ArrayList<>(be.getCookTimeTotal());
        }
        return shouldSendQuickData;
    }

    @Override
    protected void writeQuickDataTag(CompoundTag tag)
    {
        super.writeQuickDataTag(tag);
        tag.putString("pop_mode", be.popMode.name());
        tag.putString("receive_mode", be.receiveMode.name());
        tag.putString("control_mode", be.controlMode.name());
        tag.putString("sort_mode", be.sortMode.name());
        tag.putIntArray("lit_time", be.getLitTime().stream().mapToInt(Integer::intValue).toArray());
        tag.putIntArray("lit_duration", be.getLitDuration().stream().mapToInt(Integer::intValue).toArray());
        tag.putIntArray("cook_time", be.getCookTime().stream().mapToInt(Integer::intValue).toArray());
        tag.putIntArray("cook_time_total", be.getCookTimeTotal().stream().mapToInt(Integer::intValue).toArray());
    }

    @Override
    public void readQuickDataTag(CompoundTag tag)
    {
        super.readQuickDataTag(tag);
        if (!player.level().isClientSide()) // 服务端读取按钮信息并广播到所有玩家
        {
            be.popMode = PopMode.valueOf(tag.getString("pop_mode").orElse(PopMode.STOP.name()));
            be.receiveMode = ReceiveMode.valueOf(tag.getString("receive_mode").orElse(ReceiveMode.STOP.name()));
            be.controlMode = RedStoneControlMode.valueOf(tag.getString("control_mode").orElse(RedStoneControlMode.IGNORE.name()));
            be.sortMode = AutoSortMode.valueOf(tag.getString("sort_mode").orElse(AutoSortMode.STOP.name()));

            // 服务端接收到更新信息后立刻通知保存
            player.level().blockEntityChanged(be.getBlockPos());
            player.level().sendBlockUpdated(be.getBlockPos(), be.getBlockState(), be.getBlockState(), 2);
        }
        else // 客户端读取全部信息
        {
            be.popMode = PopMode.valueOf(tag.getString("pop_mode").orElse(PopMode.STOP.name()));
            be.receiveMode = ReceiveMode.valueOf(tag.getString("receive_mode").orElse(ReceiveMode.STOP.name()));
            be.controlMode = RedStoneControlMode.valueOf(tag.getString("control_mode").orElse(RedStoneControlMode.IGNORE.name()));
            be.sortMode = AutoSortMode.valueOf(tag.getString("sort_mode").orElse(AutoSortMode.STOP.name()));
            be.setLitTime(Arrays.stream(tag.getIntArray("lit_time").orElseGet(() -> new int[0])).boxed().toList());
            be.setLitDuration(Arrays.stream(tag.getIntArray("lit_duration").orElseGet(() -> new int[0])).boxed().toList());
            be.setCookTime(Arrays.stream(tag.getIntArray("cook_time").orElseGet(() -> new int[0])).boxed().toList());
            be.setCookTimeTotal(Arrays.stream(tag.getIntArray("cook_time_total").orElseGet(() -> new int[0])).boxed().toList());
        }

    }
}
