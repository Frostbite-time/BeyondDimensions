package com.wintercogs.beyonddimensions.datagen;

import com.wintercogs.beyonddimensions.common.init.BDFluids;
import com.wintercogs.beyonddimensions.datagen.util.BDFluidTagsProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class ModFluidTagsProvider extends BDFluidTagsProvider
{
    public ModFluidTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> provider, @Nullable ExistingFileHelper existingFileHelper)
    {
        super(output, provider, existingFileHelper);
    }

    @Override
    public @NotNull String getName()
    {
        return "BeyondDimensions FluidTag Provider";
    }

    @Override
    protected void addTags(HolderLookup.@NotNull Provider provider)
    {
        tag(Tags.Fluids.EXPERIENCE)
                .add(BDFluids.XP_FLUID.source().get())
                .add(BDFluids.XP_FLUID.flowing().get());
    }
}
