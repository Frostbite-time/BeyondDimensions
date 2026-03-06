package com.wintercogs.beyonddimensions.datagen;

import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.common.init.BDFluidTags;
import com.wintercogs.beyonddimensions.common.init.BDFluids;
import com.wintercogs.beyonddimensions.integration.IIntegrationModule;
import com.wintercogs.beyonddimensions.integration.IntegrationManager;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.FluidTagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.data.ExistingFileHelper;

import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;

public class ModFluidTagsProvider extends FluidTagsProvider
{
    public ModFluidTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> provider, @Nullable ExistingFileHelper existingFileHelper)
    {
        super(output, provider, BDConstants.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider)
    {
        // 把源体 + 流动体都塞进 forge:experience
        tag(BDFluidTags.C_EXPERIENCE)
                .add((Fluid) BDFluids.XP_FLUID.source().get())
                .add((Fluid) BDFluids.XP_FLUID.flowing().get());

        IntegrationManager.onFluidTagDatagen(provider, new IIntegrationModule.FluidTagAppender()
        {
            @Override
            public void add(TagKey<Fluid> tag, Fluid... fluids)
            {
                ModFluidTagsProvider.this.tag(tag).add(fluids);
            }

            @Override
            public void addTag(TagKey<Fluid> tag, TagKey<Fluid> nestedTag)
            {
                ModFluidTagsProvider.this.tag(tag).addTag(nestedTag);
            }

            @Override
            public void addOptional(TagKey<Fluid> tag, ResourceLocation fluidId)
            {
                ModFluidTagsProvider.this.tag(tag).addOptional(fluidId);
            }

            @Override
            public void addOptionalTag(TagKey<Fluid> tag, ResourceLocation nestedTagId)
            {
                ModFluidTagsProvider.this.tag(tag).addOptionalTag(nestedTagId);
            }
        });
    }

    @Override
    public String getName()
    {
        return "BeyondDimensions Fluid Tags";
    }
}
