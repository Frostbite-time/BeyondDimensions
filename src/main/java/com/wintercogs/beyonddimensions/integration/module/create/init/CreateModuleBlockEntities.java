package com.wintercogs.beyonddimensions.integration.module.create.init;

import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.integration.module.create.block.entity.SchematicannonPathWayBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class CreateModuleBlockEntities
{
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, BDConstants.MODID);

    public static Supplier<BlockEntityType<SchematicannonPathWayBlockEntity>> SCHEMATICANNON_PATHWAY_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(
            "schematicannon_pathway_block_entity",
            () -> BlockEntityType.Builder.of(
                    SchematicannonPathWayBlockEntity::new,
                    CreateModuleBlocks.SCHEMATICANNON_PATHWAY.get()
            ).build(null)
    );

    public static void register(IEventBus eventBus)
    {
        BLOCK_ENTITY_TYPES.register(eventBus);
    }
}
