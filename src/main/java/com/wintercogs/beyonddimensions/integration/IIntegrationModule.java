package com.wintercogs.beyonddimensions.integration;

import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.data.event.GatherDataEvent;

public interface IIntegrationModule
{
    String modId();

    void onBootstrap(IEventBus modBus, IEventBus gameBus);

    void onCommonSetup(FMLCommonSetupEvent event);

    default void onItemCreativeTabCollect(CreativeModeTab.ItemDisplayParameters displayParameters, CreativeModeTab.Output output)
    {

    }

    default void onBlockCreativeTabCollect(CreativeModeTab.ItemDisplayParameters displayParameters, CreativeModeTab.Output output)
    {

    }

    default void onDatagen(GatherDataEvent.Client event)
    {

    }

    default void onBlockTagDatagen(HolderLookup.Provider provider, BlockTagAppender appender)
    {

    }

    default void onItemTagDatagen(HolderLookup.Provider provider, ItemTagAppender appender)
    {

    }

    default void onFluidTagDatagen(HolderLookup.Provider provider, FluidTagAppender appender)
    {

    }

    interface BlockTagAppender
    {
        void add(TagKey<Block> tag, ResourceKey<Block>... blocks);

        void addTag(TagKey<Block> tag, TagKey<Block> nestedTag);

        void addOptional(TagKey<Block> tag, ResourceKey<Block> block);

        void addOptionalTag(TagKey<Block> tag, TagKey<Block> nestedTag);
    }

    interface ItemTagAppender
    {
        void add(TagKey<Item> tag, ResourceKey<Item>... items);

        void addTag(TagKey<Item> tag, TagKey<Item> nestedTag);

        void addOptional(TagKey<Item> tag, ResourceKey<Item> item);

        void addOptionalTag(TagKey<Item> tag, TagKey<Item> nestedTag);
    }

    interface FluidTagAppender
    {
        void add(TagKey<Fluid> tag, ResourceKey<Fluid>... fluids);

        void addTag(TagKey<Fluid> tag, TagKey<Fluid> nestedTag);

        void addOptional(TagKey<Fluid> tag, ResourceKey<Fluid> fluid);

        void addOptionalTag(TagKey<Fluid> tag, TagKey<Fluid> nestedTag);
    }
}

