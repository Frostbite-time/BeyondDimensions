package com.wintercogs.beyonddimensions.datagen.util;

import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public abstract class BDBlockTagsProvider extends BlockTagsProvider
{

    public BDBlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider)
    {
        super(output, lookupProvider, BDConstants.MODID);
    }

    @Override
    public abstract @NotNull String getName();
}
