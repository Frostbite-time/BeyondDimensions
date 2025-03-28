//package com.wintercogs.beyonddimensions.Datagen;
//
//import com.wintercogs.beyonddimensions.Block.ModBlocks;
//import net.minecraft.data.loot.BlockLootSubProvider;
//import net.minecraft.world.flag.FeatureFlags;
//import net.minecraft.world.level.block.Block;
//import net.minecraftforge.registries.RegistryObject;
//
//import java.util.Set;
//
//public class ModBlockLootTableProvider extends BlockLootSubProvider
//{
//
//
//    protected ModBlockLootTableProvider()
//    {
//        super(Set.of(), FeatureFlags.REGISTRY.allFlags());
//    }
//
//    @Override
//    protected void generate()
//    {
//        dropSelf(ModBlocks.NET_CONTROL.get());
//        dropSelf(ModBlocks.NET_INTERFACE.get());
//        dropSelf(ModBlocks.NET_PATHWAY.get());
//        dropSelf(ModBlocks.NET_ENERGY_PATHWAY.get());
//    }
//
//    @Override
//    protected Iterable<Block> getKnownBlocks()
//    {
//        return ModBlocks.BLOCKS.getEntries().stream().flatMap(RegistryObject::stream)::iterator;
//    }
//}
