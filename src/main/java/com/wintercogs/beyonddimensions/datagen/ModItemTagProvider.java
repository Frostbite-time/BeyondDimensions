package com.wintercogs.beyonddimensions.datagen;

import com.wintercogs.beyonddimensions.datagen.util.BDItemTagsProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class ModItemTagProvider extends BDItemTagsProvider
{
    public ModItemTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider)
    {
        super(output, lookupProvider);
    }

    @Override
    public @NotNull String getName()
    {
        return "BeyondDimensions ItemTag Provider";
    }

    @Override
    protected void addTags(HolderLookup.@NotNull Provider provider)
    {
    }
}
