package com.wintercogs.beyonddimensions.BlockRender;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.BlockEntity.ModBlockEntities;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

public class ModBlockRenders
{
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event)
    {
        if (BeyondDimensions.Botania_Loaded)
        {
//            event.registerBlockEntityRenderer(
//                    (BlockEntityType<ManaPoolPathwayBlockEntity>) ModBlockEntities.MANA_POOL_PATHWAY_BLOCK_ENTITY.get(),
//                    ManaPoolPathwayBlockEntityRender::new
//            );
        }
    }
}
