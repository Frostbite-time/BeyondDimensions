package com.wintercogs.beyonddimensions.integration.module.rs.init;

import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.integration.module.rs.block.entity.RSNetPathwayBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class RSModuleBlockEntities
{
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, BDConstants.MODID);

    public static Supplier<BlockEntityType<?>> RS_NET_PATHWAY_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(
            "rs_net_pathway_block_entity",
            () -> new BlockEntityType<>(
                    RSNetPathwayBlockEntity::new,
                    RSModuleBlocks.RS_NET_PATHWAY.get()
            )
    );


    public static void register(IEventBus eventBus)
    {
        BLOCK_ENTITY_TYPES.register(eventBus);
    }
}
