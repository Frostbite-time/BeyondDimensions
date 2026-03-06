package com.wintercogs.beyonddimensions.client.init;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.common.init.ModBlockEntities;
import com.wintercogs.beyonddimensions.integration.Botania.Block.ManaPoolPathwayBlockEntity;
import com.wintercogs.beyonddimensions.integration.Botania.Block.ManaPoolPathwayBlockEntityRender;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.client.event.EntityRenderersEvent;

public class ModBlockRenders
{
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event)
    {
        if (BeyondDimensions.Botania_Loaded)
        {
            event.registerBlockEntityRenderer(
                    (BlockEntityType<ManaPoolPathwayBlockEntity>) ModBlockEntities.MANA_POOL_PATHWAY_BLOCK_ENTITY.get(),
                    ManaPoolPathwayBlockEntityRender::new
            );
        }
    }
}
