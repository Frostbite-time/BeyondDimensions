package com.wintercogs.beyonddimensions.datagen;

import com.mojang.math.Quadrant;
import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.common.block.NetFurnaceBlock;
import com.wintercogs.beyonddimensions.common.block.NetTerminalBlock;
import com.wintercogs.beyonddimensions.common.init.BDBlocks;
import com.wintercogs.beyonddimensions.common.init.BDFluids;
import com.wintercogs.beyonddimensions.common.init.BDItems;
import com.wintercogs.beyonddimensions.datagen.util.BDModelProvider;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.renderer.block.dispatch.VariantMutator;
import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

public class ModModelProvider extends BDModelProvider
{

    public ModModelProvider(PackOutput output)
    {
        super(output);
    }

    @Override
    public @NotNull String getName()
    {
        return "BeyondDimensions Model Provider";
    }

    @Override
    protected void registerModels(@NotNull BlockModelGenerators blockModels, @NotNull ItemModelGenerators itemModels)
    {
        blockWithItem(blockModels, BDBlocks.NET_CONTROL);
        blockWithItem(blockModels, BDBlocks.NET_INTERFACE);
        blockWithItem(blockModels, BDBlocks.NET_PATHWAY);
        blockWithItem(blockModels, BDBlocks.NET_ENERGY_PATHWAY);
        blockWithItem(blockModels, BDBlocks.DIMENSIONAL_CONNECT_BLOCK);

        customModelBlockWithItem(blockModels, BDBlocks.NET_PUMP_BLOCK.get(), "net_pump_block");
        customModelBlockWithItem(blockModels, BDBlocks.NET_HOPPER_BLOCK.get(), "net_hopper_block");
        customTerminalBlockWithItem(blockModels);
        customFurnaceBlockWithItem(blockModels);

        blockModels.createParticleOnlyBlock(BDFluids.XP_FLUID.block().get());

        itemModels.generateFlatItem(BDItems.NET_CREATER.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(BDItems.NET_MEMBER_INVITER.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(BDItems.NET_MANAGER_INVITER.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(BDItems.UNSTABLE_SPACE_TIME_FRAGMENT.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(BDItems.STABLE_SPACE_TIME_FRAGMENT.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(BDItems.SPACE_TIME_STABLE_FRAME.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(BDItems.SHATTERED_SPACE_TIME_CRYSTALLIZATION.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(BDItems.SPACE_TIME_BAR.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(BDItems.NET_TERMINAL_ITEM.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(BDItems.NET_GIFTER.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(BDItems.NET_DESTROYER.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(BDItems.MATTER_COMPRESS_BALL.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(BDItems.NET_MAGNET_ITEM.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(BDItems.NET_FEEDER_ITEM.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(BDItems.NET_RESTOCKER_ITEM.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(BDItems.XP_EXCHANGE_ITEM.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(BDItems.TEST_ITEM_GENERATE.get(), ModelTemplates.FLAT_ITEM);

        generateFluidBucketModels(itemModels);
    }

    /**
     * 为所有流体生成桶模型（基于我们封装的 ModFluids.ALL）
     */
    private void generateFluidBucketModels(ItemModelGenerators itemModels)
    {
        for (BDFluids.FluidEntry e : BDFluids.ALL)
        {
            fluidBucketModel(itemModels, e);
        }
    }

    private void customTerminalBlockWithItem(BlockModelGenerators blockModels)
    {
        Identifier modelId = Identifier.fromNamespaceAndPath(BDConstants.MODID, "block/net_terminal_block");
        Block block = BDBlocks.NET_TERMINAL_BLOCK.get();

        blockModels.blockStateOutput.accept(
                MultiVariantGenerator.dispatch(block, BlockModelGenerators.plainVariant(modelId))
                        .with(PropertyDispatch.modify(NetTerminalBlock.FACING)
                                .select(Direction.DOWN,
                                        VariantMutator.Y_ROT.withValue(Quadrant.R180)
                                                .then(VariantMutator.X_ROT.withValue(Quadrant.R90)))
                                .select(Direction.UP, VariantMutator.X_ROT.withValue(Quadrant.R270))
                                .select(Direction.NORTH, BlockModelGenerators.NOP)
                                .select(Direction.SOUTH, VariantMutator.Y_ROT.withValue(Quadrant.R180))
                                .select(Direction.WEST, VariantMutator.Y_ROT.withValue(Quadrant.R270))
                                .select(Direction.EAST, VariantMutator.Y_ROT.withValue(Quadrant.R90)))
        );

        blockModels.registerSimpleItemModel(block, modelId);
    }

    private void customFurnaceBlockWithItem(BlockModelGenerators blockModels)
    {
        Identifier offModelId = Identifier.fromNamespaceAndPath(BDConstants.MODID, "block/net_furnace_block");
        Identifier onModelId = Identifier.fromNamespaceAndPath(BDConstants.MODID, "block/net_furnace_block_on");
        Block block = BDBlocks.NET_FURNACE_BLOCK.get();

        blockModels.blockStateOutput.accept(
                MultiVariantGenerator.dispatch(block)
                        .with(PropertyDispatch.initial(NetFurnaceBlock.FACING, NetFurnaceBlock.LIT)
                                .select(Direction.NORTH, false, BlockModelGenerators.plainVariant(offModelId))
                                .select(Direction.NORTH, true, BlockModelGenerators.plainVariant(onModelId))
                                .select(Direction.EAST, false, BlockModelGenerators.plainVariant(offModelId).with(BlockModelGenerators.Y_ROT_90))
                                .select(Direction.EAST, true, BlockModelGenerators.plainVariant(onModelId).with(BlockModelGenerators.Y_ROT_90))
                                .select(Direction.SOUTH, false, BlockModelGenerators.plainVariant(offModelId).with(BlockModelGenerators.Y_ROT_180))
                                .select(Direction.SOUTH, true, BlockModelGenerators.plainVariant(onModelId).with(BlockModelGenerators.Y_ROT_180))
                                .select(Direction.WEST, false, BlockModelGenerators.plainVariant(offModelId).with(BlockModelGenerators.Y_ROT_270))
                                .select(Direction.WEST, true, BlockModelGenerators.plainVariant(onModelId).with(BlockModelGenerators.Y_ROT_270)))
        );

        blockModels.registerSimpleItemModel(block, offModelId);
    }

}
