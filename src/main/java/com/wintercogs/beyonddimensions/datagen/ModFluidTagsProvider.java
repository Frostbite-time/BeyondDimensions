package com.wintercogs.beyonddimensions.datagen;

import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.common.init.BDFluidTags;
import com.wintercogs.beyonddimensions.common.init.BDFluids;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.FluidTagsProvider;
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
    }

    @Override
    public String getName()
    {
        return "BeyondDimensions Fluid Tags";
    }
}
