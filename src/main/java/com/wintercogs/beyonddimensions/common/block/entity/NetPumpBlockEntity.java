package com.wintercogs.beyonddimensions.common.block.entity;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.wintercogs.beyonddimensions.api.capability.helper.CapabilityHelper;
import com.wintercogs.beyonddimensions.api.capability.helper.wrapper.IStackHandlerWrapper;
import com.wintercogs.beyonddimensions.api.capability.helper.wrapper.StackHandlerWrapperHelper;
import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet;
import com.wintercogs.beyonddimensions.api.dimensionnet.UnifiedStorage;
import com.wintercogs.beyonddimensions.api.storage.handler.impl.StackHandler;
import com.wintercogs.beyonddimensions.api.storage.key.IStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.api.storage.key.StackKeyRegistry;
import com.wintercogs.beyonddimensions.common.init.BDBlockEntities;
import com.wintercogs.beyonddimensions.common.machine.FilterMode;
import com.wintercogs.beyonddimensions.common.menu.NetPumpMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.function.Function;

public class NetPumpBlockEntity extends BaseMachineBlockEntity implements MenuProvider
{
    // 存储相邻方块的能力
    // 按照 typedId -> 堆叠处理器 的结构存储，使用Multimap，因为一个typedId可以对应多个处理器
    private final Multimap<ResourceLocation, Object> handlerCache = ArrayListMultimap.create();
    private boolean needsCapabilityUpdate = true;
    private final Direction[] directions = Direction.values();

    private static final int capacity = 36;
    private final StackHandler filterSlots = new StackHandler(capacity)
    {
        @Override
        public void onChange()
        {
            if (level != null && !level.isClientSide())
                level.blockEntityChanged(worldPosition);
        }
    };

    public FilterMode filterMode = FilterMode.BLACK;

    public NetPumpBlockEntity(BlockPos pos, BlockState blockState)
    {
        super(BDBlockEntities.NET_PUMP_BLOCK_ENTITY.get(), pos, blockState);
    }

    @Override
    public boolean shouldWork()
    {
        return super.shouldWork() && getNet() != null;
    }

    @Override
    public int getTicksPerWork()
    {
        return 10;
    }

    @Override
    public void workStart()
    {
        if (level == null || !needsCapabilityUpdate) return;

        handlerCache.clear();

        for (Direction dir : directions)
        {
            BlockPos targetPos = this.getBlockPos().relative(dir);
            BlockEntity neighbor = level.getBlockEntity(targetPos);
            if (neighbor != null && !(neighbor instanceof NetedBlockEntity))
            {

                CapabilityHelper.BlockCapabilityMap.forEach(
                        (resourceLocation, cap) -> {
                            LazyOptional<?> handler = neighbor.getCapability(cap, dir.getOpposite());
                            handler.ifPresent(delegate -> handlerCache.put(resourceLocation, delegate));
                        }
                );

            }
        }

        needsCapabilityUpdate = false;
    }

    @Override
    public void workContent()
    {
        handlerCache.forEach(
                (typeId, handler) -> {
                    Function handlerGetter = StackHandlerWrapperHelper.stackWrappers.get(typeId);

                    IStackHandlerWrapper<Object> stackHandlerWrapper = (IStackHandlerWrapper) handlerGetter.apply(handler);

                    for (int slot = 0; slot < stackHandlerWrapper.getSlots(); slot++)
                    {
                        Object stack = stackHandlerWrapper.getStackInSlot(slot);
                        IStackKey<?> typeKey = StackKeyRegistry.getType(typeId);
                        KeyAmount ka = typeKey.fromStackObject(stack);
                        if (ka != null && !ka.key().isEmpty() && matchesFilter(ka.key()))
                        {
                            DimensionsNet net = getNet();
                            if (net != null)
                            {
                                UnifiedStorage storage = net.getUnifiedStorage();

                                long canInsert = ka.amount() - storage.insert(ka.key(), ka.amount(), true).amount();
                                canInsert = Math.min(canInsert, ka.amount());
                                long extract = stackHandlerWrapper.extract(slot, canInsert, false);
                                net.getUnifiedStorage().insert(ka.key(), extract, false);
                            }
                        }
                    }
                }
        );
    }

    private boolean matchesFilter(IStackKey<?> otherStack)
    {
        switch (filterMode)
        {
            case BLACK ->
            {
                for (KeyAmount stack : filterSlots.getStorage())
                {
                    if (stack.key().isSame(otherStack))
                        return false;
                }
                return true;
            }
            case WHITE ->
            {
                for (KeyAmount stack : filterSlots.getStorage())
                {
                    if (stack.key().isSame(otherStack))
                        return true;
                }
                return false;
            }
            case IGNORE ->
            {
                return true;
            }

        }
        return false;
    }

    public void setNeedsCapabilityUpdate()
    {
        needsCapabilityUpdate = true;
    }

    @Override
    public void invalidateCaps()
    {
        super.invalidateCaps();
        setNeedsCapabilityUpdate();
    }

    @Override
    public void load(CompoundTag tag)
    {
        super.load(tag);
        filterSlots.deserializeNBT(tag.getCompound("filter_slots"));
        filterMode = FilterMode.valueOf(tag.getString("filter_type"));
        setNeedsCapabilityUpdate();
    }

    @Override
    protected void saveAdditional(CompoundTag tag)
    {
        super.saveAdditional(tag);
        tag.put("filter_slots", filterSlots.serializeNBT());
        tag.putString("filter_type", filterMode.name());
    }

    @Override
    public @NotNull Component getDisplayName()
    {
        return Component.translatable("menu.title.beyonddimensions.pump_menu");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, @NotNull Inventory inventory, @NotNull Player player)
    {
        return new NetPumpMenu(containerId, inventory, filterSlots, this);
    }
}
