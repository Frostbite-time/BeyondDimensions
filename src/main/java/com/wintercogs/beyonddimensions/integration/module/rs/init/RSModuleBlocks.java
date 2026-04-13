package com.wintercogs.beyonddimensions.integration.module.rs.init;

import com.wintercogs.beyonddimensions.api.ids.BDBlockIds;
import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.common.init.BDItems;
import com.wintercogs.beyonddimensions.integration.module.rs.block.RSNetPathwayBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Function;

public class RSModuleBlocks
{
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(BDConstants.MODID);


    public static final DeferredBlock<Block> RS_NET_PATHWAY = registerBlock(BDBlockIds.RS_NET_PATHWAY,
            RSNetPathwayBlock::new,
            BlockBehaviour.Properties.of().strength(2f));


    private static <T extends Block> DeferredBlock<T> registerBlock(String name, Function<BlockBehaviour.Properties, T> blockFactory, BlockBehaviour.Properties properties)
    {
        DeferredBlock<T> toReturn = BLOCKS.registerBlock(name, blockFactory, () -> properties);
        registerBlockItem(name, toReturn);
        return toReturn;
    }

    private static <T extends Block> void registerBlockItem(String name, DeferredBlock<T> block)
    {
        BDItems.ITEMS.registerSimpleBlockItem(name, block);
    }

    public static void register(IEventBus eventBus)
    {
        BLOCKS.register(eventBus);
    }
}
