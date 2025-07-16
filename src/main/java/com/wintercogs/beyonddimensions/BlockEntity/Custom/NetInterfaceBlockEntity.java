package com.wintercogs.beyonddimensions.BlockEntity.Custom;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.wintercogs.beyonddimensions.Api.DataBase.DimensionsNet;
import com.wintercogs.beyonddimensions.Api.DataBase.Handler.StackTypedHandler;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.IStackType;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.ItemStackType;
import com.wintercogs.beyonddimensions.Api.DataBase.StackHandlerWrapper.IStackHandlerWrapper;
import com.wintercogs.beyonddimensions.Api.Registry.CapabilityHelper;
import com.wintercogs.beyonddimensions.Api.Registry.StackHandlerWrapperHelper;
import com.wintercogs.beyonddimensions.BlockEntity.ModBlockEntities;
import com.wintercogs.beyonddimensions.DataComponents.ModDataComponents;
import com.wintercogs.beyonddimensions.Item.Custom.MatterCompressionBall;
import com.wintercogs.beyonddimensions.Item.ModItems;
import com.wintercogs.beyonddimensions.Machine.BaseMachine;
import com.wintercogs.beyonddimensions.Machine.PopMode;
import com.wintercogs.beyonddimensions.Machine.RedStoneControlMode;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class NetInterfaceBlockEntity extends NetedBlockEntity implements BaseMachine,MenuProvider
{
    public final int transHold = 9;
    public int transTime = 0;

    private static final int capacity = 27;

    // 用来标记物品或者流体的槽位，只由UI控制
    private final StackTypedHandler fakeStackHandler = new StackTypedHandler(capacity)
    {
        // 只触发方块自身的保存，但是不向周围发信
        @Override
        public void onChange()
        {
            if(!level.isClientSide())
                level.blockEntityChanged(worldPosition);
        }
    };

    private final StackTypedHandler stackHandler = new StackTypedHandler(capacity)
    {
        @Override
        public void onChange()
        {
            if(!level.isClientSide())
                level.blockEntityChanged(worldPosition);
        }
    };

    public PopMode popMode = PopMode.STOP;
    public RedStoneControlMode controlMode = RedStoneControlMode.IGNORE;

    private final Direction[] directions = Direction.values();
    

    // 存储相邻方块的能力
    // 按照 typedId -> 堆叠处理器 的结构存储，使用Multimap，因为一个typedId可以对应多个处理器
    private final Multimap<ResourceLocation,Object> handlerCache = ArrayListMultimap.create();
    private boolean needsCapabilityUpdate = true;

    public StackTypedHandler getStackHandler()
    {
        return this.stackHandler;
    }

    public StackTypedHandler getFakeStackHandler(){
        return this.fakeStackHandler;
    }

    public NetInterfaceBlockEntity(BlockPos pos, BlockState blockState)
    {
        super(ModBlockEntities.NET_INTERFACE_BLOCK_ENTITY.get(), pos, blockState);
    }

    // 此方法的签名与 BlockEntityTicker 函数接口的签名匹配.
    public static void tick(Level level, BlockPos pos, BlockState state, NetInterfaceBlockEntity blockEntity) {
        // 你希望在计时期间执行的任何操作.
        // 例如，你可以在这里更改一个制作进度值或消耗能量.
        if(level.isClientSide())
            return; // 客户端不执行任何操作


        blockEntity.transTime++;
        if(blockEntity.transTime>=blockEntity.transHold)
        {
            blockEntity.working();
            blockEntity.transTime = 0;
        }

    }

    @Override
    public boolean shouldWork()
    {
        return BaseMachine.super.shouldWork();
    }

    @Override
    public void workContent()
    {
        BaseMachine.super.workContent();
        if(getNetId() != -1)
        {
            transferToNet();
            transferFromNet();
        }
        // 尝试输出物品到周围
        if(popMode == PopMode.OPEN)
        {
            // 在使用缓存前确保它是最新的
            updateCapabilityCache();
            popStack();
        }
    }

    // 更新能力缓存
    public void updateCapabilityCache() {
        if (level == null || !needsCapabilityUpdate) return;

        handlerCache.clear();
        
        for (Direction dir : directions) {
            BlockPos targetPos = this.getBlockPos().relative(dir);
            BlockEntity neighbor = level.getBlockEntity(targetPos);
            if (neighbor != null && !(neighbor instanceof NetedBlockEntity)) {

                CapabilityHelper.BlockCapabilityMap.forEach(
                        (resourceLocation, cap) -> {
                            Object handler = level.getCapability(cap,targetPos, dir.getOpposite());
                            if (handler != null) {
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
    public static void registerCapability(RegisterCapabilitiesEvent event) {

        CapabilityHelper.BlockCapabilityMap.forEach(
                (resourceLocation, directionBlockCapability) -> {
                    Function handler = StackTypedHandler.typedHandlerMap.get(resourceLocation);
                    event.registerBlockEntity(
                            (BlockCapability<? super Object, ? extends Direction>)directionBlockCapability,
                            ModBlockEntities.NET_INTERFACE_BLOCK_ENTITY.get(),
                            (be, side) -> {
                                return handler.apply(be.stackHandler);
                            }
                    );
                }
        );
    }



    public void transferToNet()
    {
        // 只有不被标记的槽位才会被收纳进入网络
        DimensionsNet net = getNet();
        if(net != null)
        {
            for(int i=0; i<capacity; i++)
            {
                IStackType flag = fakeStackHandler.getStackBySlot(i);
                if(flag!= null && !flag.isEmpty())
                {
                    if (flag.isSameTypeSameComponents(stackHandler.getStackBySlot(i)))
                        continue;
                }
                IStackType stack = stackHandler.getStackBySlot(i);
                if(stack !=null &&!stack.isEmpty())
                {
                    net.getUnifiedStorage().insert(stack.copy(),false);
                    stackHandler.setStackDirectly(i, new ItemStackType());
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
        if(net != null)
        {
            for(int i=0; i<capacity; i++)
            {
                IStackType flag = fakeStackHandler.getStackBySlot(i);
                if(flag!=null && !flag.isEmpty())
                {
                    // 到达数量上限或者是不同物品则不尝试插入
                    IStackType current = stackHandler.getStorage().get(i);
                    if(current != null &&!current.isEmpty())
                    {
                        if(current.getVanillaMaxStackSize() >= current.getStackAmount())
                        {
                            continue;
                        }
                        if(!current.isSameTypeSameComponents(flag.copy()))
                        {
                            continue;
                        }
                    }

                    // 插入逻辑
                    IStackType stack = net.getUnifiedStorage().extract(flag.copyWithCount(flag.getVanillaMaxStackSize()),false);
                    if(stack !=null &&!stack.isEmpty())
                    {
                        IStackType remaining = stackHandler.insert(i,stack.copy(),false);
                        if(remaining.getStackAmount()<stack.getStackAmount())
                        {
                            net.getUnifiedStorage().insert(remaining.copy(),false);
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

                    IStackHandlerWrapper stackHandlerWrapper = (IStackHandlerWrapper)handlerGetter.apply(handler);

                    for(int i = 0;i<capacity;i++)
                    {
                        if(fakeStackHandler.getStackBySlot(i).getTypeId().equals(typeId))
                        {
                            if(fakeStackHandler.getStackBySlot(i).isSameTypeSameComponents(stackHandler.getStackBySlot(i)))
                            {
                                IStackType current = stackHandler.getStackBySlot(i).copy();
                                for(int slot= 0;slot< stackHandlerWrapper.getSlots();slot++)
                                {
                                    long remainging = stackHandlerWrapper.insert(slot,current.copyStack(),false);
                                    long extract = current.getStackAmount() - remainging;
                                    stackHandler.extract(i,extract,false);
                                    current.shrink(extract);
                                    if(current.isEmpty())
                                        break;
                                }
                            }
                        }
                    }
                }
        );

    }

    public void dropContent()
    {
        List<IStackType> dropList = new ArrayList<>();
        for(IStackType stack : stackHandler.getStorage())
        {
            if(!stack.isEmpty())
            {
                // 如果内含物质球，直接弹出，防止NBT套娃
                if(stack instanceof ItemStackType itemStackType)
                {
                    if(itemStackType.getStack().getItem() instanceof MatterCompressionBall)
                        Block.popResource(level,getBlockPos(),itemStackType.copyStack());
                    else
                        dropList.add(stack.copy());
                }
                else
                {
                    dropList.add(stack.copy());
                }
            }
        }
        ItemStack ball = new ItemStack(ModItems.MATTER_COMPRESS_BALL.get(), 1);
        if(!dropList.isEmpty())
        {
            ball.set(ModDataComponents.ISTACK_SLOTS, dropList);
            Block.popResource(level,getBlockPos(),ball);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries)
    {
        super.loadAdditional(tag,registries);
        this.stackHandler.deserializeNBT(registries,tag.getCompound("inventory"));
        this.fakeStackHandler.deserializeNBT(registries,tag.getCompound("flags"));

        this.controlMode = RedStoneControlMode.valueOf(tag.getString("controlMode"));

        // 旧数据兼容
        String popModeNew = tag.getString("popMode");
        if(!popModeNew.isEmpty())
        {
            this.popMode = PopMode.valueOf(popModeNew);
        }
        else if(tag.getBoolean("popMode"))
        {
            this.popMode = PopMode.OPEN;
        }
        else
        {
            this.popMode = PopMode.STOP;
        }
        // 加载后需要更新缓存
        setNeedsCapabilityUpdate();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries)
    {
        super.saveAdditional(tag, registries);
        tag.put("inventory", stackHandler.serializeNBT(registries));
        tag.put("flags",fakeStackHandler.serializeNBT(registries));
        tag.putString("popMode",this.popMode.name());
        tag.putString("controlMode",this.controlMode.name());
    }
    
    // 在方块状态变化时重新缓存能力
    @Override
    public void setChanged() {
        super.setChanged();
    }

    @Override
    public Component getDisplayName()
    {
        return Component.translatable("menu.title.beyonddimensions.net_interface_menu");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player)
    {
        return new NetInterfaceBaseMenu(containerId,player.getInventory(),this.getStackHandler() ,this.getFakeStackHandler(),this);
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
}
