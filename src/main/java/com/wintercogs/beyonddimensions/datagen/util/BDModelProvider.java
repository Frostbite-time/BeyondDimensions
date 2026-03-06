package com.wintercogs.beyonddimensions.datagen.util;

import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.common.init.BDFluids;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.model.ModelLocationUtils;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.item.DynamicFluidContainerModel;
import net.neoforged.neoforge.registries.DeferredBlock;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public abstract class BDModelProvider extends ModelProvider
{
    public BDModelProvider(PackOutput output)
    {
        super(output, BDConstants.MODID);
    }

    @Override
    public abstract @NotNull String getName();

    /**
     * 生成单个流体桶模型：
     * {
     * "parent": "neoforge:item/bucket",
     * "loader": "neoforge:fluid_container",
     * "fluid": "<modid>:<fluidName>"
     * }
     */
    protected void fluidBucketModel(ItemModelGenerators itemModels, BDFluids.FluidEntry e)
    {
        itemModels.itemModelOutput.accept(
                e.bucket().get(),
                new DynamicFluidContainerModel.Unbaked(
                        new DynamicFluidContainerModel.Textures(
                                Optional.of(Identifier.withDefaultNamespace("item/bucket")),
                                Optional.of(Identifier.withDefaultNamespace("item/bucket")),
                                Optional.of(Identifier.fromNamespaceAndPath("neoforge", "item/mask/bucket_fluid")),
                                Optional.of(Identifier.fromNamespaceAndPath("neoforge", "item/mask/bucket_fluid_cover"))
                        ),
                        e.source().get(),
                        false, // flip_gas
                        true,  // cover_is_mask
                        true  // apply_fluid_luminosity
                )
        );
    }

    protected void blockWithItem(BlockModelGenerators blockModels, DeferredBlock<?> deferredBlock)
    {
        Block block = deferredBlock.get();
        blockModels.createTrivialCube(block);
        blockModels.registerSimpleItemModel(block, ModelLocationUtils.getModelLocation(block));
    }

    protected void customModelBlockWithItem(BlockModelGenerators blockModels, Block block, String modelName)
    {
        Identifier modelId = Identifier.fromNamespaceAndPath(BDConstants.MODID, "block/" + modelName);
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(block, BlockModelGenerators.plainVariant(modelId)));
        blockModels.registerSimpleItemModel(block, modelId);
    }
}
