package com.wintercogs.beyonddimensions.datagen;

import com.wintercogs.beyonddimensions.datagen.util.BDItemTagsProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class ModItemTagProvider extends BDItemTagsProvider
{
    public ModItemTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, CompletableFuture<TagLookup<Block>> blockTags, @Nullable ExistingFileHelper existingFileHelper)
    {
        super(output, lookupProvider, blockTags, existingFileHelper);
    }

    @Override
    public @NotNull String getName()
    {
        return "BeyondDimensions ItemTag Provider";
    }

    @Override
    protected void addTags(HolderLookup.Provider provider)
    {
    }
}
