package com.wintercogs.beyonddimensions.datagen;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.datagen.util.BDItemTagsProvider;
import com.wintercogs.beyonddimensions.integration.module.rs.Tags.RSTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
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
        // 添加RS富铁石英和机器框架的合成表支持
        tag(RSTags.RS_QUARTZ_ENRICHED_IRON)
                .addOptional(ResourceLocation.tryBuild(BeyondDimensions.RSModId, RSTags.QUARTZ_ENRICHED_IRON_NAME));
        tag(RSTags.RS_MACHINE_CASING)
                .addOptional(ResourceLocation.tryBuild(BeyondDimensions.RSModId, RSTags.MACHINE_CASING_NAME));
    }
}
