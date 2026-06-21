package com.wintercogs.beyonddimensions.datagen;

import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.common.init.BDBlocks;
import com.wintercogs.beyonddimensions.integration.IIntegrationModule;
import com.wintercogs.beyonddimensions.integration.IntegrationManager;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class ModBlockTagProvider extends BlockTagsProvider
{

    public ModBlockTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider)
    {
        super(output, lookupProvider, BDConstants.MODID);
    }

    @Override
    public @NotNull String getName()
    {
        return "BeyondDimensions BlockTag Provider";
    }

    @Override
    protected void addTags(HolderLookup.@NotNull Provider provider)
    {
        // 标记以下方块使用镐子挖掘更快
        tag(BlockTags.MINEABLE_WITH_PICKAXE)
                .add(BDBlocks.NET_CONTROL.getKey())
                .add(BDBlocks.NET_INTERFACE.getKey())
                .add(BDBlocks.NET_PATHWAY.getKey())
                .add(BDBlocks.NET_ENERGY_PATHWAY.getKey())
                .add(BDBlocks.NET_TERMINAL_BLOCK.getKey())
                .add(BDBlocks.NET_PUMP_BLOCK.getKey())
                .add(BDBlocks.NET_HOPPER_BLOCK.getKey())
                .add(BDBlocks.NET_FURNACE_BLOCK.getKey())
                .add(BDBlocks.NET_BLAST_FURNACE_BLOCK.getKey())
                .add(BDBlocks.NET_SMOKER_BLOCK.getKey())
                .add(BDBlocks.DIMENSIONAL_CONNECT_BLOCK.getKey());

        IntegrationManager.onBlockTagDatagen(provider, new IIntegrationModule.BlockTagAppender()
        {
            @Override
            public void add(TagKey<Block> tag, ResourceKey<Block>... blocks)
            {
                ModBlockTagProvider.this.tag(tag).add(blocks);
            }

            @Override
            public void addTag(TagKey<Block> tag, TagKey<Block> nestedTag)
            {
                ModBlockTagProvider.this.tag(tag).addTag(nestedTag);
            }

            @Override
            public void addOptional(TagKey<Block> tag, ResourceKey<Block> block)
            {
                ModBlockTagProvider.this.tag(tag).addOptional(block);
            }

            @Override
            public void addOptionalTag(TagKey<Block> tag, TagKey<Block> nestedTag)
            {
                ModBlockTagProvider.this.tag(tag).addOptionalTag(nestedTag);
            }
        });
    }
}
