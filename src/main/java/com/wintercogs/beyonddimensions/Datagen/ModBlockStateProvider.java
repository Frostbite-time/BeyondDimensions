//package com.wintercogs.beyonddimensions.Datagen;
//
//import com.wintercogs.beyonddimensions.BeyondDimensions;
//import com.wintercogs.beyonddimensions.Block.ModBlocks;
//import net.minecraft.data.PackOutput;
//import net.minecraft.world.level.block.Block;
//import net.minecraftforge.client.model.generators.BlockStateProvider;
//import net.minecraftforge.common.data.ExistingFileHelper;
//import net.minecraftforge.registries.RegistryObject;
//
//public class ModBlockStateProvider extends BlockStateProvider
//{
//
//    public ModBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper)
//    {
//        super(output, BeyondDimensions.MODID, exFileHelper);
//    }
//
//    @Override
//    protected void registerStatesAndModels()
//    {
//        blockWithItem(ModBlocks.NET_CONTROL);
//        blockWithItem(ModBlocks.NET_INTERFACE);
//        blockWithItem(ModBlocks.NET_PATHWAY);
//        blockWithItem(ModBlocks.NET_ENERGY_PATHWAY);
//
//    }
//
//    private void blockWithItem(RegistryObject<Block> deferredBlock)
//    {
//        simpleBlockWithItem(deferredBlock.get(), cubeAll(deferredBlock.get()));
//    }
//}
