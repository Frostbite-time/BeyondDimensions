package com.wintercogs.beyonddimensions.integration.module.botania.init;

import com.wintercogs.beyonddimensions.integration.module.botania.client.render.ManaPoolPathwayBlockEntityRender;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

public class BotaniaModuleBlockRenders
{
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event)
    {
        event.registerBlockEntityRenderer(
                BotaniaModuleBlockEntities.MANA_POOL_PATHWAY_BLOCK_ENTITY.get(),
                ManaPoolPathwayBlockEntityRender::new
        );
    }
}
