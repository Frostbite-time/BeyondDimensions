package com.wintercogs.beyonddimensions.BlockEntity.Custom;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.wintercogs.beyonddimensions.DataBase.DimensionsNet;
import com.wintercogs.beyonddimensions.DataBase.Handler.StackTypedHandler;
import com.wintercogs.beyonddimensions.DataBase.Stack.IStackType;
import com.wintercogs.beyonddimensions.DataBase.Stack.ItemStackType;
import com.wintercogs.beyonddimensions.DataBase.StackHandlerWrapper.IStackHandlerWrapper;
import com.wintercogs.beyonddimensions.Unit.CapabilityHelper;
import com.wintercogs.beyonddimensions.Unit.StackHandlerWrapperHelper;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;

import java.util.Map;
import java.util.function.Function;

public class NetInterfaceBlockEntity extends NetedBlockEntity implements ITickable
{
    public final int transHold = 9;
    public int transTime = 0;

    // 用来标记物品或者流体的槽位，只由UI控制
    private final StackTypedHandler fakeStackHandler = new StackTypedHandler(9)
    {
        // 只触发方块自身的保存，但是不向周围发信
        @Override
        public void onChange() {
            if (!world.isRemote) {
                //world.mark(pos); // 替代高版本的 blockEntityChanged
            }
        }
    };

    private final StackTypedHandler stackHandler = new StackTypedHandler(9)
    {
        @Override
        public void onChange() {
            if (!world.isRemote) {
                //world.markTileEntityForUpdate(pos); // 替代高版本的 blockEntityChanged
            }
        }
    };

    public boolean popMode = false;

    // 1.12.2 使用 EnumFacing
    private final EnumFacing[] directions = EnumFacing.values();


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

    public NetInterfaceBlockEntity()
    {
        super();
    }

    // 1.12.2 Tick 入口
    @Override
    public void update() {
        if (world.isRemote) return;
        transTime++;
        if (transTime >= transHold) {
            if (getNetId() != -1) {
                transferToNet();
                transferFromNet();
            }
            if (popMode) {
                updateCapabilityCache();
                popStack();
            }
            transTime = 0;
        }
    }


    /// 更新能力缓存
    public void updateCapabilityCache() {
        if (!needsCapabilityUpdate) return;
        handlerCache.clear();
        for (EnumFacing dir : directions) {
            BlockPos targetPos = pos.offset(dir);
            TileEntity neighbor = world.getTileEntity(targetPos);
            if (neighbor == null || neighbor instanceof NetedBlockEntity) continue;
            CapabilityHelper.BlockCapabilityMap.forEach((resLoc, cap) -> {
                Object handler = neighbor.getCapability(cap, dir.getOpposite());
                if (handler != null) {
                    handlerCache.put(resLoc, handler);
                }
            });
        }
        needsCapabilityUpdate = false;
    }

    public void setNeedsCapabilityUpdate()
    {
        needsCapabilityUpdate = true;
    }


    @Override
    public <T> T getCapability(Capability<T> cap, EnumFacing side) {
        for (Map.Entry<ResourceLocation, Capability<?>> entry : CapabilityHelper.BlockCapabilityMap.entrySet()) {
            if (entry.getValue() == cap) {
                Function<StackTypedHandler, Object> constructor = StackTypedHandler.typedHandlerMap.get(entry.getKey());
                if (constructor != null) {
                    return (T) constructor.apply(stackHandler);
                }
            }
        }
        return super.getCapability(cap, side);
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
                                    // 未完全移植警告！！！！！！
                                    // 说明:旧版mek接口需求一个方向，此处硬编码
                                    long remainging = stackHandlerWrapper.insert(EnumFacing.NORTH,slot,current.copyStack(),false);
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
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        stackHandler.deserializeNBT(compound.getCompoundTag("inventory"));
        fakeStackHandler.deserializeNBT(compound.getCompoundTag("flags"));
        popMode = compound.getBoolean("popMode");
        setNeedsCapabilityUpdate();
    }
    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setTag("inventory", stackHandler.serializeNBT());
        compound.setTag("flags", fakeStackHandler.serializeNBT());
        compound.setBoolean("popMode", popMode);
        return compound;
    }

}
