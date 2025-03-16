package com.wintercogs.beyonddimensions.Registry;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.Common.Processor.Capability.ConditionalBlockCapabilityProcessor;
import com.wintercogs.beyonddimensions.Common.Processor.Capability.IBlockCapabilityProcessor;
import com.wintercogs.beyonddimensions.Common.Processor.Capability.SimpleBlockCapabilityProcessor;
import com.wintercogs.beyonddimensions.DataBase.Stack.ChemicalStackType;
import com.wintercogs.beyonddimensions.DataBase.Stack.FluidStackType;
import com.wintercogs.beyonddimensions.DataBase.Stack.ItemStackType;
import com.wintercogs.beyonddimensions.Integration.Mek.Capability.ChemicalCapabilityHelper;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.HashSet;
import java.util.Set;

public class BlockCapabilityProcessorRegister
{
    private static final Set<IBlockCapabilityProcessor<?>> Processors = new HashSet<>();

    // 注册处理器
    public static <T> void registerProcessor(IBlockCapabilityProcessor<T> processor)
    {
        Processors.add(processor);
    }

    // 获取所有处理器 用于遍历
    public static Set<IBlockCapabilityProcessor<?>> getProcessors()
    {
        return Processors;
    }

    // 使用静态块进行注册
    static
    {
        registerProcessor(new SimpleBlockCapabilityProcessor<IItemHandler>(
                Capabilities.ItemHandler.BLOCK,
                (stackType) -> stackType instanceof ItemStackType,
                (level, targetPos, direction) -> {
                    return level.getCapability(Capabilities.ItemHandler.BLOCK,targetPos,direction);
                },
                (handler, slot, stack, simulate) -> {
                    return handler.insertItem(slot,((ItemStackType)stack).copyStack(),simulate).getCount();
                },
                handler -> handler.getSlots()
        ));

        registerProcessor(new SimpleBlockCapabilityProcessor<IFluidHandler>(
                Capabilities.FluidHandler.BLOCK,
                (stackType) -> stackType instanceof FluidStackType,
                (level, targetPos, direction) -> {
                    return level.getCapability(Capabilities.FluidHandler.BLOCK,targetPos,direction);
                },
                (handler, slot, stack, simulate) -> {
                    long current = ((FluidStackType)stack).getStackAmount();
                    if(simulate)
                        return current - handler.fill(((FluidStackType)stack).copyStack(), IFluidHandler.FluidAction.SIMULATE);
                    else
                        return current - handler.fill(((FluidStackType)stack).copyStack(), IFluidHandler.FluidAction.EXECUTE);

                },
                handler -> handler.getTanks()
        ));

        registerProcessor(new ConditionalBlockCapabilityProcessor<>(
                () -> BeyondDimensions.MekLoaded,
                (BlockCapability<? super mekanism.api.chemical.IChemicalHandler, Direction>) ChemicalCapabilityHelper.CHEMICAL,
                (stackType) -> stackType instanceof ChemicalStackType,
                (level, targetPos, direction) -> {
                    return (mekanism.api.chemical.IChemicalHandler)level.getCapability(ChemicalCapabilityHelper.CHEMICAL,targetPos,direction);
                },
                (handler, slot, stack, simulate) -> {
                    if(simulate)
                        return ((mekanism.api.chemical.IChemicalHandler)handler).insertChemical(slot,((ChemicalStackType)stack).copyStack(), mekanism.api.Action.SIMULATE).getAmount();
                    else
                        return ((mekanism.api.chemical.IChemicalHandler)handler).insertChemical(slot,((ChemicalStackType)stack).copyStack(), mekanism.api.Action.EXECUTE).getAmount();
                },
                handler -> ((mekanism.api.chemical.IChemicalHandler)handler).getChemicalTanks()
        ));
    }

}
