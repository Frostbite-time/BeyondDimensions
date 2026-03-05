package com.wintercogs.beyonddimensions.common.init;

import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.common.block.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public class BDBlocks
{
    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(BDConstants.MODID);

    public static final DeferredBlock<@NotNull Block> NET_CONTROL = registerBlock("net_control",
            NetControlBlock::new,
            BlockBehaviour.Properties.of().strength(4f));

    public static final DeferredBlock<@NotNull Block> NET_INTERFACE = registerBlock("net_interface",
            NetInterfaceBlock::new,
            BlockBehaviour.Properties.of().strength(2f));

    public static final DeferredBlock<@NotNull Block> NET_PATHWAY = registerBlock("net_pathway",
            NetPathwayBlock::new,
            BlockBehaviour.Properties.of().strength(2f));

    public static final DeferredBlock<@NotNull Block> NET_ENERGY_PATHWAY = registerBlock("net_energy_pathway",
            NetEnergyPathwayBlock::new,
            BlockBehaviour.Properties.of().strength(2f));

    public static final DeferredBlock<@NotNull Block> NET_TERMINAL_BLOCK = registerBlock("net_terminal_block",
            NetTerminalBlock::new,
            BlockBehaviour.Properties.of().strength(2f));

    public static final DeferredBlock<@NotNull Block> NET_PUMP_BLOCK = registerBlock("net_pump_block",
            NetPumpBlock::new,
            BlockBehaviour.Properties.of().strength(2f));

    public static final DeferredBlock<@NotNull Block> NET_HOPPER_BLOCK = registerBlock("net_hopper_block",
            NetHopperBlock::new,
            BlockBehaviour.Properties.of().strength(2f));

    public static final DeferredBlock<@NotNull Block> NET_FURNACE_BLOCK = registerBlock("net_furnace_block",
            NetFurnaceBlock::new,
            BlockBehaviour.Properties.of().strength(2f));

    // 合成材料-维度链接框架
    public static final DeferredBlock<@NotNull Block> DIMENSIONAL_CONNECT_BLOCK = registerBlock("dimensional_connect_block",
            Block::new,
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
