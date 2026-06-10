package com.wintercogs.beyonddimensions.integration.module.create.contraption;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.api.contraption.storage.item.MountedItemStorage;
import com.simibubi.create.api.contraption.storage.item.MountedItemStorageType;
import com.simibubi.create.content.contraptions.Contraption;
import com.wintercogs.beyonddimensions.api.capability.helper.ordered.ItemStackTypedHandler;
import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet;
import com.wintercogs.beyonddimensions.api.storage.handler.impl.StackHandler;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.api.storage.key.impl.EmptyStackKey;
import com.wintercogs.beyonddimensions.common.block.entity.NetInterfaceBlockEntity;
import com.wintercogs.beyonddimensions.common.machine.FuzzyMode;
import com.wintercogs.beyonddimensions.common.machine.PopMode;
import com.wintercogs.beyonddimensions.common.machine.RedStoneControlMode;
import com.wintercogs.beyonddimensions.common.menu.NetInterfaceAccess;
import com.wintercogs.beyonddimensions.common.menu.NetInterfaceBaseMenu;
import com.wintercogs.beyonddimensions.common.menu.NetInterfaceSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NetInterfaceMountedStorage extends MountedItemStorage implements NetInterfaceAccess
{
    private static final Codec<PopMode> POP_MODE_CODEC = Codec.STRING.xmap(PopMode::valueOf, PopMode::name);
    private static final Codec<FuzzyMode> FUZZY_MODE_CODEC = Codec.STRING.xmap(FuzzyMode::valueOf, FuzzyMode::name);
    private static final Codec<RedStoneControlMode> CONTROL_MODE_CODEC = Codec.STRING.xmap(RedStoneControlMode::valueOf, RedStoneControlMode::name);

    public static final Codec<NetInterfaceMountedStorage> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            StackHandler.CODEC.fieldOf("inventory").forGetter(NetInterfaceMountedStorage::getStackHandler),
            StackHandler.CODEC.fieldOf("flags").forGetter(NetInterfaceMountedStorage::getFakeStackHandler),
            Codec.INT.fieldOf("net_id").forGetter(NetInterfaceMountedStorage::getNetId),
            POP_MODE_CODEC.fieldOf("pop_mode").forGetter(NetInterfaceMountedStorage::getPopMode),
            FUZZY_MODE_CODEC.fieldOf("fuzzy_mode").forGetter(NetInterfaceMountedStorage::getFuzzyMode),
            CONTROL_MODE_CODEC.fieldOf("control_mode").forGetter(NetInterfaceMountedStorage::getControlMode),
            Codec.INT.fieldOf("step_tick").forGetter(NetInterfaceMountedStorage::getStepTick)
    ).apply(instance, NetInterfaceMountedStorage::new));

    private final StackHandler stackHandler;
    private final ItemStackTypedHandler itemHandlerWrapper;
    private final StackHandler fakeStackHandler;
    private final int netId;
    private final NetInterfaceSettings settings = new NetInterfaceSettings();
    private RedStoneControlMode controlMode;
    private int stepTick;
    private boolean valid = true;

    public NetInterfaceMountedStorage(StackHandler stackHandler, StackHandler fakeStackHandler, int netId,
                                      PopMode popMode, FuzzyMode fuzzyMode, RedStoneControlMode controlMode,
                                      int stepTick)
    {
        this(NetInterfaceMountedStorageType.NET_INTERFACE.get(), stackHandler, fakeStackHandler, netId, popMode, fuzzyMode, controlMode, stepTick);
    }

    public NetInterfaceMountedStorage(MountedItemStorageType<?> type, StackHandler stackHandler, StackHandler fakeStackHandler,
                                      int netId, PopMode popMode, FuzzyMode fuzzyMode, RedStoneControlMode controlMode,
                                      int stepTick)
    {
        super(type);
        this.stackHandler = stackHandler;
        this.itemHandlerWrapper = new ItemStackTypedHandler(stackHandler);
        this.fakeStackHandler = fakeStackHandler;
        this.netId = netId;
        this.settings.setPopMode(popMode);
        this.settings.setFuzzyMode(fuzzyMode);
        this.controlMode = controlMode;
        this.stepTick = stepTick;
    }

    @Override
    public StackHandler getStackHandler()
    {
        return this.stackHandler;
    }

    @Override
    public StackHandler getFakeStackHandler()
    {
        return this.fakeStackHandler;
    }

    public int getNetId()
    {
        return this.netId;
    }

    @Override
    public NetInterfaceSettings getNetInterfaceSettings()
    {
        return this.settings;
    }

    @Override
    public RedStoneControlMode getControlMode()
    {
        return this.controlMode;
    }

    @Override
    public void setControlMode(RedStoneControlMode controlMode)
    {
        if (!this.valid || this.controlMode == controlMode)
        {
            return;
        }
        this.controlMode = controlMode;
    }

    @Override
    public void setPopMode(PopMode popMode)
    {
        if (!this.valid || getPopMode() == popMode)
        {
            return;
        }
        NetInterfaceAccess.super.setPopMode(popMode);
    }

    @Override
    public void setFuzzyMode(FuzzyMode fuzzyMode)
    {
        if (!this.valid || getFuzzyMode() == fuzzyMode)
        {
            return;
        }
        NetInterfaceAccess.super.setFuzzyMode(fuzzyMode);
    }

    @Override
    public boolean canConfigurePopMode()
    {
        return false;
    }

    public int getStepTick()
    {
        return this.stepTick;
    }

    @Override
    public boolean isMenuValid()
    {
        return this.valid;
    }

    @Override
    public void onMenuDataChanged()
    {
    }

    @Override
    public int getSlots()
    {
        return this.itemHandlerWrapper.getSlots();
    }

    @Override
    public @NotNull ItemStack getStackInSlot(int slot)
    {
        return this.itemHandlerWrapper.getStackInSlot(slot);
    }

    @Override
    public void setStackInSlot(int slot, @NotNull ItemStack stack)
    {
        if (!this.valid)
        {
            return;
        }
        this.itemHandlerWrapper.setStackInSlot(slot, stack);
    }

    @Override
    public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate)
    {
        if (!this.valid)
        {
            return stack;
        }
        return this.itemHandlerWrapper.insertItem(slot, stack, simulate);
    }

    @Override
    public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate)
    {
        if (!this.valid)
        {
            return ItemStack.EMPTY;
        }
        return this.itemHandlerWrapper.extractItem(slot, amount, simulate);
    }

    @Override
    public int getSlotLimit(int slot)
    {
        return this.itemHandlerWrapper.getSlotLimit(slot);
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack)
    {
        return this.itemHandlerWrapper.isItemValid(slot, stack);
    }

    public void tickCoreTransfer()
    {
        if (!this.valid || !shouldWorkMounted())
        {
            return;
        }
        transferToNetMounted();
        transferFromNetMounted();
    }

    private boolean shouldWorkMounted()
    {
        this.stepTick++;
        if (this.stepTick < 9)
        {
            return false;
        }
        this.stepTick = 0;

        return switch (this.controlMode)
        {
            case IGNORE, UNPOWERED -> true;
            case NOT_WORKING, POWERED -> false;
        };
    }

    private @Nullable DimensionsNet getMountedNet()
    {
        if (this.netId < 0)
        {
            return null;
        }
        DimensionsNet net = DimensionsNet.getNetFromId(this.netId);
        return net != null && !net.deleted ? net : null;
    }

    private boolean transferToNetMounted()
    {
        return NetInterfaceAccess.transferToNet(getMountedNet(), stackHandler, fakeStackHandler, stackHandler.getSlots());
    }

    private boolean transferFromNetMounted()
    {
        return NetInterfaceAccess.transferFromNet(getMountedNet(), stackHandler, fakeStackHandler, stackHandler.getSlots(), getFuzzyMode());
    }

    @Override
    public boolean handleInteraction(ServerPlayer player, Contraption contraption, StructureBlockInfo info)
    {
        NetworkHooks.openScreen(player, new SimpleMenuProvider(
                (containerId, inventory, serverPlayer) -> new NetInterfaceBaseMenu(containerId, inventory, this),
                info.state().getBlock().getName()
        ), buf -> {
            buf.writeBoolean(true);
            buf.writeNbt(this.stackHandler.serializeNBT());
            buf.writeNbt(this.fakeStackHandler.serializeNBT());
            buf.writeUtf(getPopMode().name());
            buf.writeUtf(getFuzzyMode().name());
            buf.writeUtf(this.controlMode.name());
        });
        return true;
    }

    @Override
    public void unmount(Level level, BlockState state, BlockPos pos, @Nullable BlockEntity be)
    {
        this.valid = false;
        if (be instanceof NetInterfaceBlockEntity netInterface)
        {
            copyInto(this.stackHandler, netInterface.getStackHandler());
            copyInto(this.fakeStackHandler, netInterface.getFakeStackHandler());
            netInterface.setPopMode(getPopMode());
            netInterface.setFuzzyMode(getFuzzyMode());
            netInterface.setControlMode(this.controlMode);
            netInterface.setNeedsCapabilityUpdate();
            netInterface.setChanged();
            netInterface.onMenuDataChanged();
        }
    }

    public static NetInterfaceMountedStorage fromBlockEntity(NetInterfaceBlockEntity be)
    {
        return new NetInterfaceMountedStorage(
                copyOf(be.getStackHandler()),
                copyOf(be.getFakeStackHandler()),
                be.getNetId(),
                be.getPopMode(),
                be.getFuzzyMode(),
                be.getControlMode(),
                be.getStepTick()
        );
    }

    private static StackHandler copyOf(StackHandler source)
    {
        StackHandler copy = new StackHandler(source.getSlots());
        copyInto(source, copy);
        return copy;
    }

    private static void copyInto(StackHandler source, StackHandler target)
    {
        int slots = Math.min(source.getSlots(), target.getSlots());
        for (int i = 0; i < slots; i++)
        {
            KeyAmount stack = source.getStackBySlot(i);
            target.setStackDirectly(i, stack.key(), stack.amount());
        }
        for (int i = slots; i < target.getSlots(); i++)
        {
            target.setStackDirectly(i, EmptyStackKey.INSTANCE, 0);
        }
    }
}
