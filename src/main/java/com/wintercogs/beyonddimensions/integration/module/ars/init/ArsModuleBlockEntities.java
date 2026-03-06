package com.wintercogs.beyonddimensions.integration.module.ars.init;

import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.integration.module.ars.block.SourcePathwayBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ArsModuleBlockEntities
{
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, BDConstants.MODID);

    public static final RegistryObject<BlockEntityType<SourcePathwayBlockEntity>> ARS_SOURCE_PATHWAY_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(
            "ars_source_pathway_block_entity",
            () -> BlockEntityType.Builder.of(
                    SourcePathwayBlockEntity::new,
                    ArsModuleBlocks.ARS_SOURCE_PATHWAY.get()
            ).build(null)
    );

    public static void register(IEventBus eventBus)
    {
        BLOCK_ENTITY_TYPES.register(eventBus);
    }
}
