package com.wintercogs.beyonddimensions.BlockEntity.Custom;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.wintercogs.beyonddimensions.Api.DataBase.DimensionsNet;
import com.wintercogs.beyonddimensions.Api.DataBase.Handler.StackHandler;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.EmptyStackKey;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.ItemStackKey;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.KeyAmount;
import com.wintercogs.beyonddimensions.Api.DataBase.StackHandlerWrapper.IStackHandlerWrapper;
import com.wintercogs.beyonddimensions.Api.Registry.CapabilityHelper;
import com.wintercogs.beyonddimensions.Api.Registry.StackHandlerWrapperHelper;
import com.wintercogs.beyonddimensions.Api.Util.CapCtx;
import com.wintercogs.beyonddimensions.Api.Util.CommonHandler;
import com.wintercogs.beyonddimensions.Api.config.CommonConfigRuntime;
import com.wintercogs.beyonddimensions.BlockEntity.ModBlockEntities;
import com.wintercogs.beyonddimensions.DataComponents.ModDataComponents;
import com.wintercogs.beyonddimensions.Item.Custom.MatterCompressionBall;
import com.wintercogs.beyonddimensions.Item.ModItems;
import com.wintercogs.beyonddimensions.Machine.FuzzyMode;
import com.wintercogs.beyonddimensions.Machine.PopMode;
import com.wintercogs.beyonddimensions.Menu.NetInterfaceBaseMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
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
    private final Multimap<ResourceLocation, Object> handlerCache = ArrayListMultimap.create();
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
        super(ModBlockEntities.NET_INTERFACE_BLOCK_ENTITY.get(), pos, blockState);
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
                            ModBlockEntities.NET_INTERFACE_BLOCK_ENTITY.get(),
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
        ItemStack ball = new ItemStack(ModItems.MATTER_COMPRESS_BALL.get(), 1);
        if (!dropList.isEmpty())
        {
            ball.set(ModDataComponents.ISTACK_SLOTS, dropList);
            Block.popResource(level, getBlockPos(), ball);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries)
    {
        super.loadAdditional(tag, registries);
        this.stackHandler.deserializeNBT(registries, tag.getCompound("inventory"));
        this.fakeStackHandler.deserializeNBT(registries, tag.getCompound("flags"));

        // 旧数据兼容
        String popModeNew = tag.getString("pop_mode");
        if (!popModeNew.isEmpty())
        {
            this.popMode = PopMode.valueOf(popModeNew);
        }
        else if (!tag.getString("popMode").isEmpty())
        {
            this.popMode = PopMode.valueOf(tag.getString("popMode"));
        }
        else if (tag.getBoolean("popMode"))
        {
            this.popMode = PopMode.OPEN;
        }
        else
        {
            this.popMode = PopMode.STOP;
        }

        String fuzzyModeNew = tag.getString("fuzzy_mode");
        if (!fuzzyModeNew.isEmpty())
        {
            this.fuzzyMode = FuzzyMode.valueOf(fuzzyModeNew);
        }
        // 加载后需要更新缓存
        setNeedsCapabilityUpdate();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries)
    {
        super.saveAdditional(tag, registries);
        tag.put("inventory", stackHandler.serializeNBT(registries));
        tag.put("flags", fakeStackHandler.serializeNBT(registries));
        tag.putString("pop_mode", this.popMode.name());
        tag.putString("fuzzy_mode", this.fuzzyMode.name());
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
