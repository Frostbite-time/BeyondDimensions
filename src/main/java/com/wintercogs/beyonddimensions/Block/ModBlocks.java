package com.wintercogs.beyonddimensions.Block;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.Block.Custom.*;
import com.wintercogs.beyonddimensions.Item.ModItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModBlocks
{
    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(BeyondDimensions.MODID);
    
    public static final  DeferredBlock<Block> NET_CONTROL = registerBlock("net_control",
            ()-> new NetControlBlock(BlockBehaviour.Properties.of()
                    .strength(4f)));

    public static final DeferredBlock<Block> NET_INTERFACE = registerBlock("net_interface",
            ()-> new NetInterfaceBlock(BlockBehaviour.Properties.of().strength(2f)));

    public static final DeferredBlock<Block> NET_PATHWAY = registerBlock("net_pathway",
            ()-> new NetPathwayBlock(BlockBehaviour.Properties.of().strength(2f)));

    public static final DeferredBlock<Block> NET_ENERGY_PATHWAY = registerBlock("net_energy_pathway",
            ()-> new NetEnergyPathwayBlock(BlockBehaviour.Properties.of().strength(2f)));


    private static <T extends Block> DeferredBlock<T> registerBlock(String name, Supplier<T> block)
    {
        DeferredBlock<T> toReturn = BLOCKS.register(name,block);
        registerBlockItem(name,toReturn);
        return toReturn;
    }

    private static <T extends Block> void registerBlockItem(String name, DeferredBlock<T> block)
    {
        ModItems.ITEMS.register(name,() -> new BlockItem(block.get(), new Item.Properties()));
    }

    public static void register(IEventBus eventBus)
    {
        BLOCKS.register(eventBus);
    }
}
