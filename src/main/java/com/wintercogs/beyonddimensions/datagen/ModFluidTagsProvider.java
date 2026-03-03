package com.wintercogs.beyonddimensions.datagen;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.Tags.ModFluidTags;
import com.wintercogs.beyonddimensions.common.init.BDFluids;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.FluidTagsProvider;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class ModFluidTagsProvider extends FluidTagsProvider
{
    public ModFluidTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> provider)
    {
        super(output, provider, BeyondDimensions.MODID);
    }

    @Override
    protected void addTags(@NotNull HolderLookup.Provider provider)
    {
        // 把源体 + 流动体都塞进 c:experience
        tag(ModFluidTags.C_EXPERIENCE)
                .add(BDFluids.XP_FLUID.source().get())
                .add(BDFluids.XP_FLUID.flowing().get());
    }

    @Override
    public @NotNull String getName()
    {
        return "BeyondDimensions Fluid Tags";
    }
}
