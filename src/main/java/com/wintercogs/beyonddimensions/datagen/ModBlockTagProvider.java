package com.wintercogs.beyonddimensions.datagen;

import com.simibubi.create.AllTags;
import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.common.init.BDBlocks;
import com.wintercogs.beyonddimensions.integration.IIntegrationModule;
import com.wintercogs.beyonddimensions.integration.IntegrationManager;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.data.BlockTagsProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.concurrent.CompletableFuture;

public class ModBlockTagProvider extends BlockTagsProvider
{

    public ModBlockTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, @Nullable ExistingFileHelper existingFileHelper)
    {
        super(output, lookupProvider, BDConstants.MODID, existingFileHelper);
    }

    @Override
    public String getName()
    {
        return "BeyondDimensions BlockTag Provider";
    }

    @Override
    protected void addTags(HolderLookup.Provider provider)
    {
        // 标记以下方块使用镐子挖掘更快
        tag(BlockTags.MINEABLE_WITH_PICKAXE)
                .add(BDBlocks.NET_CONTROL.get())
                .add(BDBlocks.NET_INTERFACE.get())
                .add(BDBlocks.NET_PATHWAY.get())
                .add(BDBlocks.NET_ENERGY_PATHWAY.get())
                .add(BDBlocks.NET_TERMINAL_BLOCK.get())
                .add(BDBlocks.NET_PUMP_BLOCK.get())
                .add(BDBlocks.NET_HOPPER_BLOCK.get())
                .add(BDBlocks.NET_FURNACE_BLOCK.get())
                .add(BDBlocks.DIMENSIONAL_CONNECT_BLOCK.get());

        // 防止被机械动力识别为可用于移动式存储的方块
        // （仅1.20.1用，应对机械动力本体的bug）
        tag(AllTags.AllBlockTags.NON_MOVABLE.tag)
                .add(BDBlocks.NET_CONTROL.get())
                .add(BDBlocks.NET_INTERFACE.get())
                .add(BDBlocks.NET_PATHWAY.get())
                .add(BDBlocks.NET_ENERGY_PATHWAY.get())
                .add(BDBlocks.NET_TERMINAL_BLOCK.get())
                .add(BDBlocks.NET_PUMP_BLOCK.get())
                .add(BDBlocks.NET_HOPPER_BLOCK.get())
                .add(BDBlocks.NET_FURNACE_BLOCK.get())
                .add(BDBlocks.DIMENSIONAL_CONNECT_BLOCK.get());

        IntegrationManager.onBlockTagDatagen(provider, new IIntegrationModule.BlockTagAppender()
        {
            @Override
            public void add(TagKey<Block> tag, Block... blocks)
            {
                ModBlockTagProvider.this.tag(tag).add(blocks);
            }

            @Override
            public void addTag(TagKey<Block> tag, TagKey<Block> nestedTag)
            {
                ModBlockTagProvider.this.tag(tag).addTag(nestedTag);
            }

            @Override
            public void addOptional(TagKey<Block> tag, ResourceLocation blockId)
            {
                ModBlockTagProvider.this.tag(tag).addOptional(blockId);
            }

            @Override
            public void addOptionalTag(TagKey<Block> tag, ResourceLocation nestedTagId)
            {
                ModBlockTagProvider.this.tag(tag).addOptionalTag(nestedTagId);
            }
        });
    }
}
