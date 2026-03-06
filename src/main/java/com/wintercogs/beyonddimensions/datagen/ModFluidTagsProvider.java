package com.wintercogs.beyonddimensions.datagen;

import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.common.init.BDFluids;
import com.wintercogs.beyonddimensions.integration.IIntegrationModule;
import com.wintercogs.beyonddimensions.integration.IntegrationManager;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.FluidTagsProvider;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.common.Tags;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class ModFluidTagsProvider extends FluidTagsProvider
{
    public ModFluidTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> provider)
    {
        super(output, provider, BDConstants.MODID);
    }

    @Override
    public @NotNull String getName()
    {
        return "BeyondDimensions FluidTag Provider";
    }

    @Override
    protected void addTags(@NotNull HolderLookup.Provider provider)
    {
        // 把源体 + 流动体都塞进 c:experience
        tag(Tags.Fluids.EXPERIENCE)
                .add(BDFluids.XP_FLUID.source().get())
                .add(BDFluids.XP_FLUID.flowing().get());

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
            public void addOptional(TagKey<Fluid> tag, Fluid fluid)
            {
                ModFluidTagsProvider.this.tag(tag).addOptional(fluid);
            }

            @Override
            public void addOptionalTag(TagKey<Fluid> tag, TagKey<Fluid> nestedTag)
            {
                ModFluidTagsProvider.this.tag(tag).addOptionalTag(nestedTag);
            }
        });
    }
}
