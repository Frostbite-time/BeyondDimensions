package com.wintercogs.beyonddimensions.client.init;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.common.init.BDBlockEntities;
import com.wintercogs.beyonddimensions.integration.module.botania.Block.ManaPoolPathwayBlockEntity;
import com.wintercogs.beyonddimensions.integration.module.botania.Block.ManaPoolPathwayBlockEntityRender;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

public class BDBlockRenders
{
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event)
    {
        if (BeyondDimensions.Botania_Loaded)
        {
            event.registerBlockEntityRenderer(
                    (BlockEntityType<ManaPoolPathwayBlockEntity>) BDBlockEntities.MANA_POOL_PATHWAY_BLOCK_ENTITY.get(),
                    ManaPoolPathwayBlockEntityRender::new
            );
        }
    }
}
