package com.wintercogs.beyonddimensions.datagen.util;

import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.common.init.BDFluids;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.client.model.generators.ItemModelBuilder;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.client.model.generators.loaders.DynamicFluidContainerModelBuilder;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.NotNull;

public abstract class BDItemModelProvider extends ItemModelProvider
{
    public BDItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper)
    {
        super(output, BDConstants.MODID, existingFileHelper);
    }

    @Override
    @NotNull
    public abstract String getName();

    protected void fluidBucketModel(BDFluids.FluidEntry e)
    {
        final String modelName = e.bucket().getId().getPath();

        ItemModelBuilder builder = getBuilder(modelName)
                .parent(new ModelFile.UncheckedModelFile(ResourceLocation.tryBuild("forge", "item/bucket")));

        builder.customLoader(DynamicFluidContainerModelBuilder::begin)
                .fluid((Fluid) e.source().get())
                .end();
    }
}
