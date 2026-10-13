package com.wintercogs.beyonddimensions.datagen;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.common.block.BaseNetFurnaceBlock;
import com.wintercogs.beyonddimensions.common.block.NetStockEmitterBlock;
import com.wintercogs.beyonddimensions.common.init.BDBlocks;
import com.wintercogs.beyonddimensions.common.init.BDFluids;
import com.wintercogs.beyonddimensions.datagen.util.BDBlockStateProvider;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredBlock;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ModBlockStateProvider extends BDBlockStateProvider
{

    public ModBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper)
    {
        super(output, exFileHelper);
    }

    @Override
    public @NotNull String getName()
    {
        return "BeyondDimensions BlockState Provider";
    }

    @Override
    protected void registerStatesAndModels()
    {
        // 六面完整方块：贴图在 textures/block/<方块 id>/ 下按面分开，见 SixFacedBlock
        sixFaced(BDBlocks.NET_CONTROL).build();
        sixFaced(BDBlocks.NET_INTERFACE).build();
        sixFaced(BDBlocks.NET_PATHWAY).build();
        sixFaced(BDBlocks.NET_ENERGY_PATHWAY).build();
        sixFaced(BDBlocks.DIMENSIONAL_CONNECT_BLOCK).build();
        for (DeferredBlock<?> furnace : List.of(BDBlocks.NET_FURNACE_BLOCK, BDBlocks.NET_BLAST_FURNACE_BLOCK, BDBlocks.NET_SMOKER_BLOCK))
            sixFaced(furnace).appearance(state -> state.getValue(BaseNetFurnaceBlock.LIT) ? "on" : "off").build();
        sixFaced(BDBlocks.NET_STOCK_EMITTER).appearance(state -> state.getValue(NetStockEmitterBlock.POWERED) ? "on" : "off").build();
        // 终端是一块薄板，模型自己写，北面贴着方块
        ModelFile terminal = models().getExistingFile(BeyondDimensions.makeId("net_terminal_block"));
        orientedBlock(BDBlocks.NET_TERMINAL_BLOCK.get(), state -> terminal);
        simpleBlockItem(BDBlocks.NET_TERMINAL_BLOCK.get(), terminal);
        // 其他模型自己写的方块只注册物品
        simpleBlockItem(BDBlocks.NET_PUMP_BLOCK.get(), models().getExistingFile(BeyondDimensions.makeId("net_pump_block")));
        simpleBlockItem(BDBlocks.NET_HOPPER_BLOCK.get(), models().getExistingFile(BeyondDimensions.makeId("net_hopper_block")));
        // 流体方块的模型只提供粒子贴图，与原版的水、岩浆相同
        for (BDFluids.FluidEntry fluid : BDFluids.ALL)
        {
            simpleBlock(fluid.block().get(), models().getBuilder(fluid.name()).texture("particle", modLoc("block/" + fluid.name() + "_still")));
        }
    }
}
