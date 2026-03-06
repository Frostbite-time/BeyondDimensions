package com.wintercogs.beyonddimensions.common.init;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.common.block.*;
import com.wintercogs.beyonddimensions.integration.module.botania.block.ManaPoolPathway;
import com.wintercogs.beyonddimensions.integration.module.create.block.SchematicannonPathWayBlock;
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

    public static final RegistryObject<Block> NET_CONTROL = registerBlock("net_control",
            () -> new NetControlBlock(BlockBehaviour.Properties.of()
                    .strength(4f)));

    public static final RegistryObject<Block> NET_INTERFACE = registerBlock("net_interface",
            () -> new NetInterfaceBlock(BlockBehaviour.Properties.of().strength(2f)));

    public static final RegistryObject<Block> NET_PATHWAY = registerBlock("net_pathway",
            () -> new NetPathwayBlock(BlockBehaviour.Properties.of().strength(2f)));

    public static final RegistryObject<Block> NET_ENERGY_PATHWAY = registerBlock("net_energy_pathway",
            () -> new NetEnergyPathwayBlock(BlockBehaviour.Properties.of().strength(2f)));

    public static final RegistryObject<Block> NET_TERMINAL_BLOCK = registerBlock("net_terminal_block",
            () -> new NetTerminalBlock(BlockBehaviour.Properties.of().strength(2f)));

    public static final RegistryObject<Block> NET_PUMP_BLOCK = registerBlock("net_pump_block",
            () -> new NetPumpBlock(BlockBehaviour.Properties.of().strength(2f)));

    public static final RegistryObject<Block> NET_HOPPER_BLOCK = registerBlock("net_hopper_block",
            () -> new NetHopperBlock(BlockBehaviour.Properties.of().strength(2f)));

    public static final RegistryObject<Block> NET_FURNACE_BLOCK = registerBlock("net_furnace_block",
            () -> new NetFurnaceBlock(BlockBehaviour.Properties.of().strength(2f)));

    // 合成材料-维度链接框架
    public static final RegistryObject<Block> DIMENSIONAL_CONNECT_BLOCK = registerBlock("dimensional_connect_block",
            () -> new Block(BlockBehaviour.Properties.of().strength(2f)));

    public static final RegistryObject<Block> MANA_POOL_PATHWAY = registerBlock("mana_pool_pathway",
            () -> {
                if (BeyondDimensions.Botania_Loaded)
                    return new ManaPoolPathway(BlockBehaviour.Properties.of().strength(2f));
                else
                    return new Block(BlockBehaviour.Properties.of().strength(2f));
            });

    public static final RegistryObject<Block> SCHEMATICANNON_PATHWAY = registerBlock("schematicannon_pathway",
            () -> {
                if (BeyondDimensions.Create_Loaded)
                    return new SchematicannonPathWayBlock(BlockBehaviour.Properties.of().strength(2f).noOcclusion());
                else
                    return new Block(BlockBehaviour.Properties.of().strength(2f).noOcclusion());
            });


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
