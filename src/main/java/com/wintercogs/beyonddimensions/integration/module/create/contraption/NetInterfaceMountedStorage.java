package com.wintercogs.beyonddimensions.integration.module.create.contraption;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.api.contraption.storage.item.MountedItemStorage;
import com.simibubi.create.api.contraption.storage.item.MountedItemStorageType;
import com.simibubi.create.content.contraptions.Contraption;
import com.wintercogs.beyonddimensions.api.capability.helper.ordered.ItemStackTypedHandler;
import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet;
import com.wintercogs.beyonddimensions.api.machine.MachineSettings;
import com.wintercogs.beyonddimensions.api.storage.handler.impl.StackHandler;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.api.storage.key.impl.EmptyStackKey;
import com.wintercogs.beyonddimensions.common.block.entity.NetInterfaceBlockEntity;
import com.wintercogs.beyonddimensions.common.init.BDMachineSettings;
import com.wintercogs.beyonddimensions.common.machine.FuzzyMode;
import com.wintercogs.beyonddimensions.common.machine.PopMode;
import com.wintercogs.beyonddimensions.common.machine.RedStoneControlMode;
import com.wintercogs.beyonddimensions.common.menu.NetInterfaceAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class NetInterfaceMountedStorage extends MountedItemStorage implements NetInterfaceAccess
{
    // 旧版本的动态结构把三项设置分开保存；读到坏值时当作没有，取默认值
    private static final Codec<PopMode> POP_MODE_CODEC = legacyEnum(PopMode.class);
    private static final Codec<FuzzyMode> FUZZY_MODE_CODEC = legacyEnum(FuzzyMode.class);
    private static final Codec<RedStoneControlMode> CONTROL_MODE_CODEC = legacyEnum(RedStoneControlMode.class);

    public static final MapCodec<NetInterfaceMountedStorage> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            StackHandler.CODEC.fieldOf("inventory").forGetter(NetInterfaceMountedStorage::getStackHandler),
            StackHandler.CODEC.fieldOf("flags").forGetter(NetInterfaceMountedStorage::getFakeStackHandler),
            Codec.INT.fieldOf("net_id").forGetter(NetInterfaceMountedStorage::getNetId),
            MachineSettings.CODEC.optionalFieldOf(SETTINGS).forGetter(storage -> Optional.of(storage.settings)),
            POP_MODE_CODEC.lenientOptionalFieldOf("pop_mode").forGetter(storage -> Optional.empty()),
            FUZZY_MODE_CODEC.lenientOptionalFieldOf("fuzzy_mode").forGetter(storage -> Optional.empty()),
            CONTROL_MODE_CODEC.lenientOptionalFieldOf("control_mode").forGetter(storage -> Optional.empty()),
            Codec.INT.fieldOf("step_tick").forGetter(NetInterfaceMountedStorage::getStepTick)
    ).apply(instance, NetInterfaceMountedStorage::decoded));

    private final StackHandler stackHandler;
    private final ItemStackTypedHandler itemHandlerWrapper;
    private final StackHandler fakeStackHandler;
    private final int netId;
    private MachineSettings settings;
    private int stepTick;
    private boolean valid = true;
    private boolean dirty;

    public NetInterfaceMountedStorage(StackHandler stackHandler, StackHandler fakeStackHandler, int netId,
                                      MachineSettings settings, int stepTick)
    {
        this(NetInterfaceMountedStorageType.NET_INTERFACE.get(), stackHandler, fakeStackHandler, netId, settings, stepTick);
    }

    public NetInterfaceMountedStorage(MountedItemStorageType<?> type, StackHandler stackHandler, StackHandler fakeStackHandler,
                                      int netId, MachineSettings settings, int stepTick)
    {
        super(type);
        this.stackHandler = stackHandler;
        this.itemHandlerWrapper = new ItemStackTypedHandler(stackHandler);
        this.fakeStackHandler = fakeStackHandler;
        this.netId = netId;
        this.settings = settings;
        this.stepTick = stepTick;
    }

    private static NetInterfaceMountedStorage decoded(StackHandler stackHandler, StackHandler fakeStackHandler, int netId,
                                                      Optional<MachineSettings> settings, Optional<PopMode> popMode,
                                                      Optional<FuzzyMode> fuzzyMode, Optional<RedStoneControlMode> controlMode,
                                                      int stepTick)
    {
        MachineSettings resolved = settings.orElseGet(() -> {
            MachineSettings legacy = MachineSettings.EMPTY;
            if (popMode.isPresent()) legacy = legacy.with(BDMachineSettings.POP.get(), popMode.get());
            if (fuzzyMode.isPresent()) legacy = legacy.with(BDMachineSettings.FUZZY.get(), fuzzyMode.get());
            if (controlMode.isPresent()) legacy = legacy.with(BDMachineSettings.REDSTONE.get(), controlMode.get());
            return legacy;
        });
        return new NetInterfaceMountedStorage(stackHandler, fakeStackHandler, netId, resolved, stepTick);
    }

    private static <E extends Enum<E>> Codec<E> legacyEnum(Class<E> type)
    {
        return Codec.STRING.comapFlatMap(name -> {
            try
            {
                return DataResult.success(Enum.valueOf(type, name));
            }
            catch (IllegalArgumentException e)
            {
                return DataResult.error(() -> "Unknown " + type.getSimpleName() + ": " + name);
            }
        }, Enum::name);
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
    public MachineSettings settings()
    {
        return this.settings;
    }

    @Override
    public void setSettings(MachineSettings settings)
    {
        if (this.valid)
            this.settings = settings;
    }

    // 动态结构里的接口不能推送缓存
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
        if (!this.valid)
        {
            return;
        }
        if (!shouldWorkMounted())
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

        return switch (getControlMode())
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
        player.openMenu(new SimpleMenuProvider(
                (containerId, inventory, serverPlayer) -> new com.wintercogs.beyonddimensions.common.menu.NetInterfaceBaseMenu(containerId, inventory, this),
                info.state().getBlock().getName()
        ), buf -> com.wintercogs.beyonddimensions.common.menu.NetInterfaceBaseMenu.writeMountedOpeningData(buf, this));
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
            netInterface.setSettings(this.settings);
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
                be.settings(),
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
