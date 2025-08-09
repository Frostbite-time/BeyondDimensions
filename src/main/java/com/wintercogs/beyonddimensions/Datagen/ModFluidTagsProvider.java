package com.wintercogs.beyonddimensions.Datagen;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.Fluid.ModFluids;
import com.wintercogs.beyonddimensions.Tags.ModFluidTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.FluidTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class ModFluidTagsProvider extends FluidTagsProvider
{
    public ModFluidTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> provider, @Nullable ExistingFileHelper existingFileHelper)
    {
        super(output, provider, BeyondDimensions.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        // 把源体 + 流动体都塞进 c:experience
        tag(ModFluidTags.C_EXPERIENCE)
                .add(ModFluids.XP_FLUID.source().get())
                .add(ModFluids.XP_FLUID.flowing().get());
    }

    @Override
    public String getName() {
        return "BeyondDimensions Fluid Tags";
    }
}
