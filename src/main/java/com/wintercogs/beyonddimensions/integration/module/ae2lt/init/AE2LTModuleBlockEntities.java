package com.wintercogs.beyonddimensions.integration.module.ae2lt.init;

import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.integration.module.ae2lt.block.entity.LightningPathwayBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class AE2LTModuleBlockEntities
{
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, BDConstants.MODID);

    public static final Supplier<BlockEntityType<LightningPathwayBlockEntity>> LIGHTNING_PATHWAY_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("lightning_pathway_block_entity", () ->
                    BlockEntityType.Builder.of(
                            LightningPathwayBlockEntity::new,
                            AE2LTModuleBlocks.LIGHTNING_PATHWAY.get()
                    ).build(null));

    public static void register(IEventBus eventBus)
    {
        BLOCK_ENTITY_TYPES.register(eventBus);
    }
}
