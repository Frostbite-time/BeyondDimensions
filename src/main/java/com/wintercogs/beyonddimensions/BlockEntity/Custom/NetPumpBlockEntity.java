package com.wintercogs.beyonddimensions.BlockEntity.Custom;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.wintercogs.beyonddimensions.Api.DataBase.DimensionsNet;
import com.wintercogs.beyonddimensions.Api.DataBase.Handler.StackTypedHandler;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.IStackType;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.StackCreater;
import com.wintercogs.beyonddimensions.Api.DataBase.StackHandlerWrapper.IStackHandlerWrapper;
import com.wintercogs.beyonddimensions.Api.DataBase.Storage.UnifiedStorage;
import com.wintercogs.beyonddimensions.Api.Registry.CapabilityHelper;
import com.wintercogs.beyonddimensions.Api.Registry.StackHandlerWrapperHelper;
import com.wintercogs.beyonddimensions.BlockEntity.ModBlockEntities;
import com.wintercogs.beyonddimensions.Machine.BaseMachine;
import com.wintercogs.beyonddimensions.Machine.FilterMode;
import com.wintercogs.beyonddimensions.Machine.RedStoneControlMode;
import com.wintercogs.beyonddimensions.Menu.NetPumpMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nullable;
import java.util.function.Function;

public class NetPumpBlockEntity extends NetedBlockEntity implements BaseMachine , MenuProvider
{
    // 存储相邻方块的能力
    // 按照 typedId -> 堆叠处理器 的结构存储，使用Multimap，因为一个typedId可以对应多个处理器
    private final Multimap<ResourceLocation,Object> handlerCache = ArrayListMultimap.create();
    private boolean needsCapabilityUpdate = true;
    private final Direction[] directions = Direction.values();

    private static final int capacity = 36;
    private final StackTypedHandler filterSlots = new StackTypedHandler(capacity)
    {
        @Override
        public void onChange()
        {
            if(!level.isClientSide())
                level.blockEntityChanged(worldPosition);
        }
    };

    public FilterMode filterMode = FilterMode.BLACK;
    public RedStoneControlMode controlMode = RedStoneControlMode.IGNORE;

    public NetPumpBlockEntity(BlockPos pos, BlockState blockState)
    {
        super(ModBlockEntities.NET_PUMP_BLOCK_ENTITY.get(), pos, blockState);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, BlockEntity blockEntity)
    {
        if(blockEntity instanceof NetPumpBlockEntity netPump)
        {
            netPump.working();
        }
    }

    @Override
    public boolean shouldWork()
    {
        return BaseMachine.super.shouldWork() && getNet() != null;
    }

    @Override
    public void workStart()
    {
        if (level == null || !needsCapabilityUpdate) return;

        handlerCache.clear();

        for (Direction dir : directions) {
            BlockPos targetPos = this.getBlockPos().relative(dir);
            BlockEntity neighbor = level.getBlockEntity(targetPos);
            if (neighbor != null && !(neighbor instanceof NetedBlockEntity)) {

                CapabilityHelper.BlockCapabilityMap.forEach(
                        (resourceLocation, cap) -> {
                            LazyOptional handler = neighbor.getCapability(cap, dir.getOpposite());
                            if (handler.isPresent()) {
                                handlerCache.put(resourceLocation, handler.resolve().get());
                            }
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

                    IStackHandlerWrapper stackHandlerWrapper = (IStackHandlerWrapper)handlerGetter.apply(handler);

                    for(int slot= 0;slot< stackHandlerWrapper.getSlots();slot++)
                    {
                        Object stack = stackHandlerWrapper.getStackInSlot(slot);
                        IStackType typedStack = StackCreater.CreateEmpty(typeId);
                        typedStack.setStack(stack);
                        if(!typedStack.isEmpty() && matchesFilter(typedStack))
                        {
                            DimensionsNet net = getNet();
                            if(net != null)
                            {
                                UnifiedStorage storage = net.getUnifiedStorage();

                                long canInsert = typedStack.getStackAmount() - storage.insert(typedStack,true).getStackAmount();
                                canInsert = Math.min(canInsert,typedStack.getStackAmount());
                                long extract = stackHandlerWrapper.extract(slot,canInsert,false);
                                typedStack.setStackAmount(extract);
                                net.getUnifiedStorage().insert(typedStack,false);
                            }
                        }
                    }
                }
        );
    }

    public FilterMode getFilterMode()
    {
        return filterMode;
    }

    public void setFilterMode(FilterMode filterMode)
    {
        this.filterMode = filterMode;
    }

    private boolean matchesFilter(IStackType otherStack)
    {
        switch (filterMode)
        {
            case BLACK -> {
                for(IStackType stack : filterSlots.getStorage())
                {
                    if(stack.isSame(otherStack))
                        return false;
                }
                return true;
            }
            case WHITE -> {
                for(IStackType stack : filterSlots.getStorage())
                {
                    if(stack.isSame(otherStack))
                        return true;
                }
                return false;
            }
            case IGNORE -> {
                return true;
            }

        }
        return false;
    }

    @Override
    public RedStoneControlMode getControlMode()
    {
        return controlMode;
    }

    @Override
    public boolean hasRedStoneSignal()
    {
        return level.getBestNeighborSignal(worldPosition) > 0;
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
        controlMode = RedStoneControlMode.valueOf(tag.getString("control_mode"));
        setNeedsCapabilityUpdate();
    }

    @Override
    protected void saveAdditional(CompoundTag tag)
    {
        super.saveAdditional(tag);
        tag.put("filter_slots", filterSlots.serializeNBT());
        tag.putString("filter_type", filterMode.name());
        tag.putString("control_mode", controlMode.name());
    }

    @Override
    public Component getDisplayName()
    {
        return Component.translatable("menu.title.beyonddimensions.pump_menu");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player)
    {
        return new NetPumpMenu(containerId,inventory, filterSlots, this);
    }
}
