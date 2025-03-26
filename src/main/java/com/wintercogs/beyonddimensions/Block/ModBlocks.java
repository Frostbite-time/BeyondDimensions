package com.wintercogs.beyonddimensions.Block;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.Block.Custom.NetControlBlock;
import com.wintercogs.beyonddimensions.Block.Custom.NetEnergyPathwayBlock;
import com.wintercogs.beyonddimensions.Block.Custom.NetInterfaceBlock;
import com.wintercogs.beyonddimensions.Block.Custom.NetPathwayBlock;
import com.wintercogs.beyonddimensions.Item.ModItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.eventhandler.EventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public class ModBlocks
{
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS,BeyondDimensions.MODID);
    
    public static final RegistryObject<Block> NET_CONTROL = registerBlock("net_control",
            ()-> new NetControlBlock(BlockBehaviour.Properties.of()
                    .strength(4f)));

    public static final RegistryObject<Block> NET_INTERFACE = registerBlock("net_interface",
            ()-> new NetInterfaceBlock(BlockBehaviour.Properties.of().strength(2f)));

    public static final RegistryObject<Block> NET_PATHWAY = registerBlock("net_pathway",
            ()-> new NetPathwayBlock(BlockBehaviour.Properties.of().strength(2f)));

    public static final RegistryObject<Block> NET_ENERGY_PATHWAY = registerBlock("net_energy_pathway",
            ()-> new NetEnergyPathwayBlock(BlockBehaviour.Properties.of().strength(2f)));


    private static <T extends Block> RegistryObject<T> registerBlock(String name, Supplier<T> block)
    {
        RegistryObject<T> toReturn = BLOCKS.register(name,block);
        registerBlockItem(name,toReturn);
        return toReturn;
    }

    private static <T extends Block> void registerBlockItem(String name, RegistryObject<T> block)
    {
        ModItems.ITEMS.register(name,() -> new BlockItem(block.get(), new Item.Properties()));
    }

    public static void register(EventBus eventBus)
    {
        BLOCKS.register(eventBus);
    }
}
