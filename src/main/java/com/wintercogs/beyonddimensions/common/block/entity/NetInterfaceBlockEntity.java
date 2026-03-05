package com.wintercogs.beyonddimensions.common.block.entity;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet;
import com.wintercogs.beyonddimensions.api.storage.handler.impl.StackHandler;
import com.wintercogs.beyonddimensions.api.storage.key.impl.EmptyStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.impl.ItemStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.api.capability.helper.wrapper.IStackHandlerWrapper;
import com.wintercogs.beyonddimensions.api.capability.helper.CapabilityHelper;
import com.wintercogs.beyonddimensions.api.capability.helper.wrapper.StackHandlerWrapperHelper;
import com.wintercogs.beyonddimensions.api.util.CapCtx;
import com.wintercogs.beyonddimensions.api.util.CommonHandler;
import com.wintercogs.beyonddimensions.config.CommonConfigRuntime;
import com.wintercogs.beyonddimensions.common.item.MatterCompressionBall;
import com.wintercogs.beyonddimensions.common.machine.FuzzyMode;
import com.wintercogs.beyonddimensions.common.machine.PopMode;
import com.wintercogs.beyonddimensions.common.menu.NetInterfaceBaseMenu;
import com.wintercogs.beyonddimensions.common.init.BDBlockEntities;
import com.wintercogs.beyonddimensions.common.init.BDDataComponents;
import com.wintercogs.beyonddimensions.common.init.BDItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class NetInterfaceBlockEntity extends BaseMachineBlockEntity implements MenuProvider
{

    private static final int capacity = CommonConfigRuntime.interfaceUsableCapacity;

    // 用来标记物品或者流体的槽位，只由UI控制
    private final StackHandler fakeStackHandler = new StackHandler(capacity)
    {
        // 只触发方块自身的保存，但是不向周围发信
        @Override
        public void onChange()
        {
            if (level != null && !level.isClientSide())
                level.blockEntityChanged(worldPosition);
        }
    };

    private final StackHandler stackHandler = new StackHandler(capacity)
    {
        @Override
        public void onChange()
        {
            if (level != null && !level.isClientSide())
                level.blockEntityChanged(worldPosition);
        }
    };

    public PopMode popMode = PopMode.STOP;

    public FuzzyMode fuzzyMode = FuzzyMode.DISABLE;

    private final Direction[] directions = Direction.values();

    private int redstoneLevel = 0;

    // 存储相邻方块的能力
    // 按照 typedId -> 堆叠处理器 的结构存储，使用Multimap，因为一个typedId可以对应多个处理器
    private final Multimap<Identifier, Object> handlerCache = ArrayListMultimap.create();
    private boolean needsCapabilityUpdate = true;

    public StackHandler getStackHandler()
    {
        return this.stackHandler;
    }

    public StackHandler getFakeStackHandler()
    {
        return this.fakeStackHandler;
    }

    public int getRedstoneLevel()
    {
        return redstoneLevel;
    }

    public NetInterfaceBlockEntity(BlockPos pos, BlockState blockState)
    {
        super(BDBlockEntities.NET_INTERFACE_BLOCK_ENTITY.get(), pos, blockState);
    }

    @Override
    public boolean shouldWork()
    {
        if (level == null) return false;

        // 无论接口是否工作，更新红石信号
        int empty = stackHandler.getBucket(EmptyStackKey.INSTANCE).map(StackHandler.SlotBucket::size).orElse(stackHandler.getSlots());
        int notEmpty = stackHandler.getSlots() - empty;

        int newRedstoneLevel = (int) (((float) notEmpty / stackHandler.getSlots()) * 15);
        if (redstoneLevel != newRedstoneLevel)
        {
            redstoneLevel = newRedstoneLevel;
            level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        }

        return super.shouldWork(); // 接口方块使用内部缓存进行弹出，因此不需要检测getNet
    }

    @Override
    public void workContent()
    {
        super.workContent();

        if (getNet() != null)
        {
            if (CommonConfigRuntime.interfaceCanReceiveResource)
                transferToNet();
            if (CommonConfigRuntime.interfaceCanOutputResource)
                transferFromNet();
        }

        if (CommonConfigRuntime.interfaceCanPopResource)
        {
            // 尝试输出物品到周围
            if (popMode == PopMode.OPEN)
            {
                // 在使用缓存前确保它是最新的
                updateCapabilityCache();
                popStack();
            }
        }
    }

    // 更新能力缓存
    public void updateCapabilityCache()
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
                            Object handler = level.getCapability(cap, targetPos, dir.getOpposite());
                            if (handler != null)
                            {
                                handlerCache.put(resourceLocation, handler);
                            }
                        }
                );

            }
        }

        needsCapabilityUpdate = false;
    }

    public void setNeedsCapabilityUpdate()
    {
        needsCapabilityUpdate = true;
    }

    @Override
    public void invalidateCapabilities()
    {
        super.invalidateCapabilities();
        setNeedsCapabilityUpdate();
    }

    //--- 能力注册 (通过事件) ---
    public static void registerCapability(RegisterCapabilitiesEvent event)
    {

        CapabilityHelper.BlockCapabilityMap.forEach(
                (resourceLocation, directionBlockCapability) -> {
                    CommonHandler handler = CapabilityHelper.CommonHandlerMap.get(resourceLocation);
                    event.registerBlockEntity(
                            (BlockCapability<? super Object, ? extends Direction>) directionBlockCapability,
                            BDBlockEntities.NET_INTERFACE_BLOCK_ENTITY.get(),
                            (be, side) -> {
                                if (handler != null)
                                {
                                    if (handler.isContextual())
                                        return handler.apply(be.stackHandler, new CapCtx(be.level, be.getBlockPos(), be));
                                    else
                                        return handler.apply(be.stackHandler, null);
                                }
                                return null; // 如果handler是null，那么必然返回null
                            }
                    );
                }
        );
    }


    public void transferToNet()
    {
        // 只有不被标记的槽位才会被收纳进入网络
        DimensionsNet net = getNet();
        if (net != null)
        {
            for (int i = 0; i < capacity; i++)
            {
                KeyAmount flag = fakeStackHandler.getStackBySlot(i);
                if (!flag.isEmpty())
                {
                    if (flag.key().isSameTypeSameComponents(stackHandler.getStackBySlot(i).key()))
                        continue;
                }
                KeyAmount stack = stackHandler.getStackBySlot(i);
                if (!stack.isEmpty())
                {
                    KeyAmount extracted = stackHandler.extract(i, stack.amount(), false);
                    KeyAmount remaining = net.getUnifiedStorage().insert(extracted.key(), extracted.amount(), false);
                    if (!remaining.isEmpty())
                        stackHandler.insert(i, remaining.key(), remaining.amount(), false);
                }
            }
        }
    }

    // 从网络中获取物品，然后转移到槽位
    public void transferFromNet()
    {
        // 首先检测标记
        // 然后从网络提取适当标记物
        // 插入物品槽
        // 将剩余插回网络
        DimensionsNet net = getNet();
        if (net != null)
        {
            for (int i = 0; i < capacity; i++)
            {
                KeyAmount flag = fakeStackHandler.getStackBySlot(i);
                if (!flag.isEmpty())
                {
                    // 到达数量上限或者是不同物品则不尝试插入
                    KeyAmount current = stackHandler.getStackBySlot(i);
                    if (!current.isEmpty())
                    {
                        if (current.key().getVanillaMaxStackSize() >= current.amount())
                        {
                            continue;
                        }
                        if (!current.key().isSameTypeSameComponents(flag.key()))
                        {
                            continue;
                        }
                    }

                    // 插入逻辑
                    KeyAmount stack = net.getUnifiedStorage().extract(
                            flag.key(),
                            flag.key().getVanillaMaxStackSize(),
                            false,
                            fuzzyMode == FuzzyMode.ENABLE
                    );
                    if (!stack.isEmpty())
                    {
                        KeyAmount remaining = stackHandler.insert(i, stack.key(), stack.amount(), false);
                        if (!remaining.isEmpty())
                        {
                            net.getUnifiedStorage().insert(remaining.key(), remaining.amount(), false);
                        }
                    }
                }

            }
        }
    }

    public void popStack()
    {

        handlerCache.forEach(
                (typeId, handler) -> {
                    Function handlerGetter = StackHandlerWrapperHelper.stackWrappers.get(typeId);

                    IStackHandlerWrapper<Object> stackHandlerWrapper = (IStackHandlerWrapper) handlerGetter.apply(handler);

                    for (int i = 0; i < capacity; i++)
                    {
                        if (fakeStackHandler.getStackBySlot(i).key().getTypeId().equals(typeId))
                        {
                            if (this.fuzzyMode == FuzzyMode.ENABLE
                                    && !fakeStackHandler.getStackBySlot(i).key().isSame(stackHandler.getStackBySlot(i).key()))
                            {
                                continue;
                            }
                            if (this.fuzzyMode == FuzzyMode.DISABLE
                                    && !fakeStackHandler.getStackBySlot(i).key().isSameTypeSameComponents(stackHandler.getStackBySlot(i).key()))
                            {
                                continue;
                            }

                            KeyAmount current = stackHandler.getStackBySlot(i);
                            for (int slot = 0; slot < stackHandlerWrapper.getSlots(); slot++)
                            {
                                long remainging = stackHandlerWrapper.insert(slot, current.toStack(), false);
                                long extract = current.amount() - remainging;
                                stackHandler.extract(i, extract, false);
                                current = new KeyAmount(current.key(), current.amount() - extract);
                                if (current.isEmpty())
                                    break;
                            }
                        }
                    }
                }
        );

    }

    public void dropContent()
    {
        if (level == null) return;

        List<KeyAmount> dropList = new ArrayList<>();
        for (KeyAmount stack : stackHandler.getStorage())
        {
            if (!stack.isEmpty())
            {
                // 如果内含物质球，直接弹出，防止NBT套娃
                if (stack.key() instanceof ItemStackKey itemStackKey)
                {
                    if (itemStackKey.getSource() instanceof MatterCompressionBall)
                        Block.popResource(level, getBlockPos(), itemStackKey.copyStackWithCount(stack.amount()));
                    else
                        dropList.add(stack);
                }
                else
                {
                    dropList.add(stack);
                }
            }
        }
        ItemStack ball = new ItemStack(BDItems.MATTER_COMPRESS_BALL.get(), 1);
        if (!dropList.isEmpty())
        {
            ball.set(BDDataComponents.ISTACK_SLOTS, dropList);
            Block.popResource(level, getBlockPos(), ball);
        }
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state)
    {
        super.preRemoveSideEffects(pos, state);
        if (level instanceof ServerLevel)
        {
            dropContent();
        }
    }

    @Override
    protected void loadAdditional(@NotNull ValueInput input)
    {
        super.loadAdditional(input);
        this.stackHandler.deserializeNBT(input.lookup(), input.read("inventory", CompoundTag.CODEC).orElseGet(CompoundTag::new));
        this.fakeStackHandler.deserializeNBT(input.lookup(), input.read("flags", CompoundTag.CODEC).orElseGet(CompoundTag::new));

        String popModeNew = input.getStringOr("pop_mode", "");
        if (!popModeNew.isEmpty())
        {
            this.popMode = PopMode.valueOf(popModeNew);
        }

        String fuzzyModeNew = input.getStringOr("fuzzy_mode", "");
        if (!fuzzyModeNew.isEmpty())
        {
            this.fuzzyMode = FuzzyMode.valueOf(fuzzyModeNew);
        }
        // 加载后需要更新缓存
        setNeedsCapabilityUpdate();
    }

    @Override
    protected void saveAdditional(@NotNull ValueOutput output)
    {
        super.saveAdditional(output);
        output.store("inventory", CompoundTag.CODEC, stackHandler.serializeNBT(lookupProvider()));
        output.store("flags", CompoundTag.CODEC, fakeStackHandler.serializeNBT(lookupProvider()));
        output.putString("pop_mode", this.popMode.name());
        output.putString("fuzzy_mode", this.fuzzyMode.name());
    }

    @Override
    public @NotNull Component getDisplayName()
    {
        return Component.translatable("menu.title.beyonddimensions.net_interface_menu");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, @NotNull Inventory inventory, Player player)
    {
        return new NetInterfaceBaseMenu(containerId, player.getInventory(), this);
    }

    @Override
    public int getTicksPerWork()
    {
        return 9;
    }
}
