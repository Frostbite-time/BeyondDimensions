package com.wintercogs.beyonddimensions.BlockEntity.Custom;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.BlockEntity.ModBlockEntities;
import com.wintercogs.beyonddimensions.DataBase.DimensionsNet;
import com.wintercogs.beyonddimensions.DataBase.Handler.StackTypedHandler;
import com.wintercogs.beyonddimensions.DataBase.Stack.IStackType;
import com.wintercogs.beyonddimensions.DataBase.Stack.ItemStackType;
import com.wintercogs.beyonddimensions.DataBase.Storage.TypedHandlerManager;
import com.wintercogs.beyonddimensions.Integration.Mek.Capability.ChemicalCapabilityHelper;
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

import java.util.function.Function;

public class NetInterfaceBlockEntity extends NetedBlockEntity
{
    public final int transHold = 20;
    public int transTime = 0;

    // 用来标记物品或者流体的槽位，只由UI控制
    private final StackTypedHandler fakeStackHandler = new StackTypedHandler(9)
    {
        @Override
        public void onChange()
        {
            setChanged();
        }
    };

    private final StackTypedHandler stackHandler = new StackTypedHandler(9)
    {
        @Override
        public void onChange()
        {
            setChanged();
        }
    };

    public boolean popMode = false;

    private final Direction[] directions = Direction.values();

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

    //--- 能力注册 (通过事件) ---
    public static void registerCapability(RegisterCapabilitiesEvent event) {
        TypedHandlerManager.BlockCommonCapHandlerMap.forEach(
                (cap,handlerF)->{
                    event.registerBlockEntity(
                            (BlockCapability<? super Object, ? extends Direction>) cap, // 标准物品能力
                            ModBlockEntities.NET_INTERFACE_BLOCK_ENTITY.get(),
                            (be, side) -> {
                                Function handler = TypedHandlerManager.getCommonHandler(cap,StackTypedHandler.class);
                                return handler.apply(be.stackHandler);
                            } // 根据方向返回处理器
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

        if(blockEntity.getNetId() != -1)
        {
            blockEntity.transTime++;
            if(blockEntity.transTime>=blockEntity.transHold)
            {
                blockEntity.transTime = 0;
                blockEntity.transferToNet();
            }
            blockEntity.transferFromNet();
        }

        // 尝试输出物品到周围
        if(blockEntity.popMode)
        {
            blockEntity.popStack();
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
                            return;
                        }
                        if(!current.isSameTypeSameComponents(flag.copy()))
                        {
                            return;
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
            BlockPos targetPos = this.getBlockPos().relative(dir);
            BlockEntity neighbor = level.getBlockEntity(targetPos);
            if(neighbor != null && !(neighbor instanceof NetedBlockEntity))
            {
                // 开始查询能力 记住，你获取你上方的方块，一定是获取其下方的能力
                IItemHandler itemHandler = level.getCapability(Capabilities.ItemHandler.BLOCK,targetPos,dir.getOpposite());
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

                // 流体能力
                IFluidHandler fluidHandler = level.getCapability(Capabilities.FluidHandler.BLOCK,targetPos,dir.getOpposite());
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

                // 读取化学品能力
                if(BeyondDimensions.MekLoaded)
                {
                    Object tryChemicalHandler = level.getCapability(ChemicalCapabilityHelper.CHEMICAL,targetPos,dir.getOpposite());
                    mekanism.api.chemical.IChemicalHandler chemicalHandler;
                    if(tryChemicalHandler != null)
                    {
                        chemicalHandler = (mekanism.api.chemical.IChemicalHandler) tryChemicalHandler;

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
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries)
    {
        super.loadAdditional(tag,registries);
        this.stackHandler.deserializeNBT(registries,tag.getCompound("inventory"));
        this.fakeStackHandler.deserializeNBT(registries,tag.getCompound("flags"));
        this.popMode = tag.getBoolean("popMode");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries)
    {
        super.saveAdditional(tag, registries);
        tag.put("inventory", stackHandler.serializeNBT(registries));
        tag.put("flags",fakeStackHandler.serializeNBT(registries));
        tag.putBoolean("popMode",this.popMode);
    }
}
