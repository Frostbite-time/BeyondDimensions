package com.wintercogs.beyonddimensions.BlockEntity.Custom;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.wintercogs.beyonddimensions.BlockEntity.ModBlockEntities;
import com.wintercogs.beyonddimensions.DataBase.DimensionsNet;
import com.wintercogs.beyonddimensions.DataBase.Handler.StackTypedHandler;
import com.wintercogs.beyonddimensions.DataBase.Stack.IStackType;
import com.wintercogs.beyonddimensions.DataBase.Stack.ItemStackType;
import com.wintercogs.beyonddimensions.DataBase.StackHandlerWrapper.IStackHandlerWrapper;
import com.wintercogs.beyonddimensions.Unit.CapabilityHelper;
import com.wintercogs.beyonddimensions.Unit.StackHandlerWrapperHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

import java.util.Map;
import java.util.function.Function;

public class NetInterfaceBlockEntity extends NetedBlockEntity
{
    public final int transHold = 9;
    public int transTime = 0;

    // 用来标记物品或者流体的槽位，只由UI控制
    private final StackTypedHandler fakeStackHandler = new StackTypedHandler(9)
    {
        // 只触发方块自身的保存，但是不向周围发信
        @Override
        public void onChange()
        {
            if(!level.isClientSide())
                level.blockEntityChanged(worldPosition);
        }
    };

    private final StackTypedHandler stackHandler = new StackTypedHandler(9)
    {
        @Override
        public void onChange()
        {
            if(!level.isClientSide())
                level.blockEntityChanged(worldPosition);
        }
    };

    public boolean popMode = false;

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

//    //--- 能力注册 (通过事件) ---
//    public static void registerCapability(RegisterCapabilitiesEvent event) {
//
//        CapabilityHelper.BlockCapabilityMap.forEach(
//                (resourceLocation, directionBlockCapability) -> {
//                    Function handler = StackTypedHandler.typedHandlerMap.get(resourceLocation);
//                    event.registerBlockEntity(
//                            (BlockCapability<? super Object, ? extends Direction>)directionBlockCapability,
//                            ModBlockEntities.NET_INTERFACE_BLOCK_ENTITY.get(),
//                            (be, side) -> {
//                                return handler.apply(be.stackHandler);
//                            }
//                    );
//                }
//        );
//    }


    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side)
    {
        // 遍历注册的能力映射表
        for (Map.Entry<ResourceLocation, Capability<?>> entry : CapabilityHelper.BlockCapabilityMap.entrySet()) {
            // 检查当前请求的能力是否匹配注册的能力
            if (entry.getValue() == cap) {
                // 从类型映射表中获取对应的处理器构造函数
                Function<StackTypedHandler,Object> handlerConstructor = StackTypedHandler.typedHandlerMap.get(entry.getKey());

                if (handlerConstructor != null) {
                    // 创建处理器实例并转换为请求的能力类型
                    Object handler = handlerConstructor.apply(this.stackHandler);
                    // 安全类型转换后包装为 LazyOptional
                    return LazyOptional.of(() -> handler).cast();
                }
            }
        }
        // 未找到匹配能力则调用父类实现
        return super.getCapability(cap, side);
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
            if(blockEntity.getNetId() != -1)
            {
                blockEntity.transferToNet();
                blockEntity.transferFromNet();
            }
            // 尝试输出物品到周围
            if(blockEntity.popMode)
            {
                // 在使用缓存前确保它是最新的
                blockEntity.updateCapabilityCache();
                blockEntity.popStack();
            }

            blockEntity.transTime = 0;
        }

    }

    public void transferToNet()
    {
        // 只有不被标记的槽位才会被收纳进入网络
        DimensionsNet net = getNet();
        if(net != null)
        {
            for(int i=0; i<9; i++)
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
            for(int i=0; i<9; i++)
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

                    for(int i = 0;i<9;i++)
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

    @Override
    public void load(CompoundTag tag)
    {
        super.load(tag);
        this.stackHandler.deserializeNBT(tag.getCompound("inventory"));
        this.fakeStackHandler.deserializeNBT(tag.getCompound("flags"));
        this.popMode = tag.getBoolean("popMode");
        // 加载后需要更新缓存
        setNeedsCapabilityUpdate();
    }

    @Override
    protected void saveAdditional(CompoundTag tag)
    {
        super.saveAdditional(tag);
        tag.put("inventory", stackHandler.serializeNBT());
        tag.put("flags",fakeStackHandler.serializeNBT());
        tag.putBoolean("popMode",this.popMode);
    }
    
    // 在方块状态变化时重新缓存能力
    @Override
    public void setChanged() {
        super.setChanged();
    }
}
