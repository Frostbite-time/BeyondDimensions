package com.wintercogs.beyonddimensions.datagen;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.common.init.BDBlocks;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class ModBlockTagProvider extends BlockTagsProvider
{

    public ModBlockTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, @Nullable ExistingFileHelper existingFileHelper)
    {
        super(output, lookupProvider, BeyondDimensions.MODID, existingFileHelper);
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
                .add(BDBlocks.DIMENSIONAL_CONNECT_BLOCK.get())
                .add(BDBlocks.RS_NET_PATHWAY.get())
                .add(BDBlocks.ARS_SOURCE_PATHWAY.get())
                .add(BDBlocks.MANA_POOL_PATHWAY.get())
                .add(BDBlocks.SCHEMATICANNON_PATHWAY.get());

    }
}
