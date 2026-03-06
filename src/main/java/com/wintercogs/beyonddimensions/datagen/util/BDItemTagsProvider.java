package com.wintercogs.beyonddimensions.datagen.util;

import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.ItemTagsProvider;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public abstract class BDItemTagsProvider extends ItemTagsProvider
{
    public BDItemTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider)
    {
        super(output, lookupProvider, BDConstants.MODID);
    }

    @Override
    public abstract @NotNull String getName();
}
