package com.wintercogs.beyonddimensions.integration.module.botania.init;

import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.integration.module.botania.block.entity.ManaPoolPathwayBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class BotaniaModuleBlockEntities
{
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, BDConstants.MODID);


    public static Supplier<BlockEntityType<ManaPoolPathwayBlockEntity>> MANA_POOL_PATHWAY_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register(
                    "mana_pool_pathway_block_entity",
                    () -> BlockEntityType.Builder.of(
                            ManaPoolPathwayBlockEntity::new,
                            BotaniaModuleBlocks.MANA_POOL_PATHWAY.get()
                    ).build(null)
            );


    public static void register(IEventBus eventBus)
    {
        BLOCK_ENTITY_TYPES.register(eventBus);
    }
}
