package com.wintercogs.beyonddimensions.BlockEntity.Custom;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.BlockEntity.ModBlockEntities;
import com.wintercogs.beyonddimensions.DataBase.DimensionsNet;
import com.wintercogs.beyonddimensions.DataBase.Handler.StackTypedHandler;
import com.wintercogs.beyonddimensions.DataBase.Stack.IStackType;
import com.wintercogs.beyonddimensions.DataBase.Stack.ItemStackType;
import com.wintercogs.beyonddimensions.Integration.Mek.Capability.ChemicalCapabilityHelper;
import com.wintercogs.beyonddimensions.Unit.CapabilityHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.EnumMap;
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
    
    // 缓存相邻方块的能力
    private final Map<Direction, IItemHandler> itemHandlerCache = new EnumMap<>(Direction.class);
    private final Map<Direction, IFluidHandler> fluidHandlerCache = new EnumMap<>(Direction.class);
    private final Map<Direction, Object> chemicalHandlerCache = new EnumMap<>(Direction.class);
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
        
        itemHandlerCache.clear();
        fluidHandlerCache.clear();
        chemicalHandlerCache.clear();
        
        for (Direction dir : directions) {
            BlockPos targetPos = this.getBlockPos().relative(dir);
            BlockEntity neighbor = level.getBlockEntity(targetPos);
            if (neighbor != null && !(neighbor instanceof NetedBlockEntity)) {
                // 缓存物品能力
                IItemHandler itemHandler = level.getCapability(Capabilities.ItemHandler.BLOCK, targetPos, dir.getOpposite());
                if (itemHandler != null) {
                    itemHandlerCache.put(dir, itemHandler);
                }
                
                // 缓存流体能力
                IFluidHandler fluidHandler = level.getCapability(Capabilities.FluidHandler.BLOCK, targetPos, dir.getOpposite());
                if (fluidHandler != null) {
                    fluidHandlerCache.put(dir, fluidHandler);
                }
                
                // 缓存化学品能力
                if (BeyondDimensions.MekLoaded) {
                    Object chemicalHandler = level.getCapability(ChemicalCapabilityHelper.CHEMICAL, targetPos, dir.getOpposite());
                    if (chemicalHandler != null) {
                        chemicalHandlerCache.put(dir, chemicalHandler);
                    }
                }
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
        for(Direction dir: directions)
        {
            // 使用缓存的物品处理器
            IItemHandler itemHandler = itemHandlerCache.get(dir);
            if(itemHandler != null)
            {
                for(int i = 0;i<9;i++)
                {
                    if(fakeStackHandler.getStackBySlot(i).getStack() instanceof ItemStack)
                    {
                        if(fakeStackHandler.getStackBySlot(i).isSameTypeSameComponents(stackHandler.getStackBySlot(i)))
                        {
                            ItemStack current = (ItemStack) stackHandler.getStackBySlot(i).copyStack();
                            for(int slot= 0;slot< itemHandler.getSlots();slot++)
                            {
                                ItemStack remaining = itemHandler.insertItem(slot,current.copy(),false);
                                int extract = current.getCount() - remaining.getCount();
                                stackHandler.extract(i,extract,false);
                                current.shrink(extract);
                                if(current.isEmpty())
                                    break;
                            }
                        }
                    }
                }
            }

            // 使用缓存的流体处理器
            IFluidHandler fluidHandler = fluidHandlerCache.get(dir);
            if(fluidHandler != null)
            {
                for(int i = 0;i<9;i++)
                {
                    if(fakeStackHandler.getStackBySlot(i).getStack() instanceof FluidStack)
                    {
                        if(fakeStackHandler.getStackBySlot(i).isSameTypeSameComponents(stackHandler.getStackBySlot(i)))
                        {
                            FluidStack current = (FluidStack) stackHandler.getStackBySlot(i).copyStack();

                            for(int slot= 0;slot< fluidHandler.getTanks();slot++)
                            {
                                int insert = fluidHandler.fill(current.copy(), IFluidHandler.FluidAction.EXECUTE);
                                if(insert>0)
                                {
                                    stackHandler.extract(i,insert,false);
                                    current.shrink(insert);
                                }
                                if(current.isEmpty())
                                    break;
                            }
                        }
                    }
                }
            }

            // 使用缓存的化学品处理器
            if(BeyondDimensions.MekLoaded)
            {
                Object tryChemicalHandler = chemicalHandlerCache.get(dir);
                if(tryChemicalHandler != null)
                {
                    mekanism.api.chemical.IChemicalHandler chemicalHandler = (mekanism.api.chemical.IChemicalHandler) tryChemicalHandler;

                    for(int i = 0;i<9;i++)
                    {
                        if(fakeStackHandler.getStackBySlot(i).getStack() instanceof mekanism.api.chemical.ChemicalStack)
                        {
                            if(fakeStackHandler.getStackBySlot(i).isSameTypeSameComponents(stackHandler.getStackBySlot(i)))
                            {
                                mekanism.api.chemical.ChemicalStack current = (mekanism.api.chemical.ChemicalStack) stackHandler.getStackBySlot(i).copyStack();

                                for(int slot= 0;slot< chemicalHandler.getChemicalTanks();slot++)
                                {
                                    mekanism.api.chemical.ChemicalStack remaining = chemicalHandler.insertChemical(slot,current.copy(), mekanism.api.Action.EXECUTE);
                                    long extract = current.getAmount() - remaining.getAmount();
                                    stackHandler.extract(i,extract,false);
                                    current.shrink(extract);
                                    if(current.isEmpty())
                                        break;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries)
    {
        super.loadAdditional(tag,registries);
        this.stackHandler.deserializeNBT(registries,tag.getCompound("inventory"));
        this.fakeStackHandler.deserializeNBT(registries,tag.getCompound("flags"));
        this.popMode = tag.getBoolean("popMode");
        // 加载后需要更新缓存
        setNeedsCapabilityUpdate();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries)
    {
        super.saveAdditional(tag, registries);
        tag.put("inventory", stackHandler.serializeNBT(registries));
        tag.put("flags",fakeStackHandler.serializeNBT(registries));
        tag.putBoolean("popMode",this.popMode);
    }
    
    // 在方块状态变化时重新缓存能力
    @Override
    public void setChanged() {
        super.setChanged();
    }
}
