package com.wintercogs.beyonddimensions.datagen;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.Fluid.ModFluids;
import com.wintercogs.beyonddimensions.common.init.BDItems;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.model.item.DynamicFluidContainerModel;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class ModItemModelProvider extends ModelProvider
{

    public ModItemModelProvider(PackOutput output)
    {
        super(output, BeyondDimensions.MODID);
    }

    @Override
    protected void registerModels(@NotNull BlockModelGenerators blockModels, @NotNull ItemModelGenerators itemModels)
    {
        super.registerModels(blockModels, itemModels);

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

        generateFluidBucketModels(itemModels);
    }

    /**
     * 为所有流体生成桶模型（基于我们封装的 ModFluids.ALL）
     */
    private void generateFluidBucketModels(ItemModelGenerators itemModels)
    {
        for (ModFluids.FluidEntry e : ModFluids.ALL)
        {
            fluidBucketModel(itemModels, e);
        }
    }

    /**
     * 生成单个流体桶模型：
     * {
     * "parent": "neoforge:item/bucket",
     * "loader": "neoforge:fluid_container",
     * "fluid": "<modid>:<fluidName>"
     * }
     */
    private void fluidBucketModel(ItemModelGenerators itemModels, ModFluids.FluidEntry e)
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

}
