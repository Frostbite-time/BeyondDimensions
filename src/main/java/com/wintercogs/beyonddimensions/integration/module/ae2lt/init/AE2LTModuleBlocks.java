package com.wintercogs.beyonddimensions.integration.module.ae2lt.init;

import com.wintercogs.beyonddimensions.api.ids.BDBlockIds;
import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.common.init.BDItems;
import com.wintercogs.beyonddimensions.integration.module.ae2lt.block.LightningPathwayBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class AE2LTModuleBlocks
{
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(BDConstants.MODID);

    public static final DeferredBlock<Block> LIGHTNING_PATHWAY = registerBlock(
            BDBlockIds.LIGHTNING_PATHWAY,
            () -> new LightningPathwayBlock(BlockBehaviour.Properties.of().strength(2f))
    );

    private static <T extends Block> DeferredBlock<T> registerBlock(String name, Supplier<T> block)
    {
        DeferredBlock<T> result = BLOCKS.register(name, block);
        BDItems.ITEMS.register(name, () -> new BlockItem(result.get(), new Item.Properties()));
        return result;
    }

    public static void register(IEventBus eventBus)
    {
        BLOCKS.register(eventBus);
    }
}
