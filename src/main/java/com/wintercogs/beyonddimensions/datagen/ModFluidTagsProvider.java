package com.wintercogs.beyonddimensions.datagen;

import com.wintercogs.beyonddimensions.common.init.BDFluids;
import com.wintercogs.beyonddimensions.datagen.util.BDFluidTagsProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.Tags;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class ModFluidTagsProvider extends BDFluidTagsProvider
{
    public ModFluidTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> provider)
    {
        super(output, provider);
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
    }
}
