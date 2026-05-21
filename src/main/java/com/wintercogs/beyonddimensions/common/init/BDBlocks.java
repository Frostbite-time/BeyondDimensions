package com.wintercogs.beyonddimensions.common.init;

import com.wintercogs.beyonddimensions.api.ids.BDBlockIds;
import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.common.block.*;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public class BDBlocks
{
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, BDConstants.MODID);

    public static final RegistryObject<Block> NET_CONTROL = registerBlock(BDBlockIds.NET_CONTROL,
            () -> new NetControlBlock(BlockBehaviour.Properties.of()
                    .strength(4f)));

    public static final RegistryObject<Block> NET_INTERFACE = registerBlock(BDBlockIds.NET_INTERFACE,
            () -> new NetInterfaceBlock(BlockBehaviour.Properties.of().strength(2f)));

    public static final RegistryObject<Block> NET_PATHWAY = registerBlock(BDBlockIds.NET_PATHWAY,
            () -> new NetPathwayBlock(BlockBehaviour.Properties.of().strength(2f)));

    public static final RegistryObject<Block> NET_ENERGY_PATHWAY = registerBlock(BDBlockIds.NET_ENERGY_PATHWAY,
            () -> new NetEnergyPathwayBlock(BlockBehaviour.Properties.of().strength(2f)));

    public static final RegistryObject<Block> NET_TERMINAL_BLOCK = registerBlock(BDBlockIds.NET_TERMINAL_BLOCK,
            () -> new NetTerminalBlock(BlockBehaviour.Properties.of().strength(2f)));

    public static final RegistryObject<Block> NET_PUMP_BLOCK = registerBlock(BDBlockIds.NET_PUMP_BLOCK,
            () -> new NetPumpBlock(BlockBehaviour.Properties.of().strength(2f)));

    public static final RegistryObject<Block> NET_HOPPER_BLOCK = registerBlock(BDBlockIds.NET_HOPPER_BLOCK,
            () -> new NetHopperBlock(BlockBehaviour.Properties.of().strength(2f)));

    public static final RegistryObject<Block> NET_FURNACE_BLOCK = registerBlock(BDBlockIds.NET_FURNACE_BLOCK,
            () -> new NetFurnaceBlock(BlockBehaviour.Properties.of().strength(2f)));

    public static final RegistryObject<Block> NET_BLAST_FURNACE_BLOCK = registerBlock(BDBlockIds.NET_BLAST_FURNACE_BLOCK,
            () -> new NetBlastFurnaceBlock(BlockBehaviour.Properties.of().strength(2f)));

    public static final RegistryObject<Block> NET_SMOKER_BLOCK = registerBlock(BDBlockIds.NET_SMOKER_BLOCK,
            () -> new NetSmokerBlock(BlockBehaviour.Properties.of().strength(2f)));

    // 合成材料-维度链接框架
    public static final RegistryObject<Block> DIMENSIONAL_CONNECT_BLOCK = registerBlock(BDBlockIds.DIMENSIONAL_CONNECT_BLOCK,
            () -> new Block(BlockBehaviour.Properties.of().strength(2f)));

    private static <T extends Block> RegistryObject<T> registerBlock(String name, Supplier<T> block)
    {
        RegistryObject<T> toReturn = BLOCKS.register(name, block);
        registerBlockItem(name, toReturn);
        return toReturn;
    }

    private static <T extends Block> void registerBlockItem(String name, RegistryObject<T> block)
    {
        BDItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    public static void register(IEventBus eventBus)
    {
        BLOCKS.register(eventBus);
    }
}
