package com.wintercogs.beyonddimensions.common.init;

import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.common.block.entity.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.NotNull;

public class BDBlockEntities
{

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, BDConstants.MODID);

    public static final DeferredHolder<BlockEntityType<?>, @NotNull BlockEntityType<@NotNull NetInterfaceBlockEntity>> NET_INTERFACE_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register(
                    "net_interface_block_entity",
                    () -> new BlockEntityType<>(
                            NetInterfaceBlockEntity::new,
                            BDBlocks.NET_INTERFACE.get()
                    )
            );

    public static final DeferredHolder<BlockEntityType<?>, @NotNull BlockEntityType<@NotNull NetPathwayBlockEntity>> NET_PATHWAY_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register(
                    "net_pathway_block_entity",
                    () -> new BlockEntityType<>(
                            NetPathwayBlockEntity::new,
                            BDBlocks.NET_PATHWAY.get()
                    )
            );


    public static final DeferredHolder<BlockEntityType<?>, @NotNull BlockEntityType<@NotNull NetEnergyPathwayBlockEntity>> NET_ENERGY_PATHWAY_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register(
                    "net_energy_pathway_block_entity",
                    () -> new BlockEntityType<>(
                            NetEnergyPathwayBlockEntity::new,
                            BDBlocks.NET_ENERGY_PATHWAY.get()
                    )
            );

    public static final DeferredHolder<BlockEntityType<?>, @NotNull BlockEntityType<@NotNull NetTerminalBlockEntity>> NET_TERMINAL_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register(
                    "net_terminal_block_entity",
                    () -> new BlockEntityType<>(
                            NetTerminalBlockEntity::new,
                            BDBlocks.NET_TERMINAL_BLOCK.get()
                    )
            );

    public static final DeferredHolder<BlockEntityType<?>, @NotNull BlockEntityType<@NotNull NetPumpBlockEntity>> NET_PUMP_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register(
                    "net_pump_block_entity",
                    () -> new BlockEntityType<>(
                            NetPumpBlockEntity::new,
                            BDBlocks.NET_PUMP_BLOCK.get()
                    )
            );

    public static final DeferredHolder<BlockEntityType<?>, @NotNull BlockEntityType<@NotNull NetHopperBlockEntity>> NET_HOPPER_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register(
                    "net_hopper_block_entity",
                    () -> new BlockEntityType<>(
                            NetHopperBlockEntity::new,
                            BDBlocks.NET_HOPPER_BLOCK.get()
                    )
            );

    public static final DeferredHolder<BlockEntityType<?>, @NotNull BlockEntityType<@NotNull NetFurnaceBlockEntity>> NET_FURNACE_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register(
                    "net_furnace_block_entity",
                    () -> new BlockEntityType<>(
                            NetFurnaceBlockEntity::new,
                            BDBlocks.NET_FURNACE_BLOCK.get()
                    )
            );

    public static void register(IEventBus eventBus)
    {
        BLOCK_ENTITY_TYPES.register(eventBus);
    }
}
