package com.wintercogs.beyonddimensions.integration.module.ae2lt.block.entity;

import com.moakiee.ae2lt.api.AE2LTCapabilities;
import com.wintercogs.beyonddimensions.common.block.entity.NetedBlockEntity;
import com.wintercogs.beyonddimensions.integration.module.ae2lt.init.AE2LTModuleBlockEntities;
import com.wintercogs.beyonddimensions.integration.module.ae2lt.storage.LightningUnifiedStorageHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

public class LightningPathwayBlockEntity extends NetedBlockEntity
{
    public LightningPathwayBlockEntity(BlockPos pos, BlockState state)
    {
        super(AE2LTModuleBlockEntities.LIGHTNING_PATHWAY_BLOCK_ENTITY.get(), pos, state);
    }

    public static void registerCapability(RegisterCapabilitiesEvent event)
    {
        event.registerBlockEntity(
                AE2LTCapabilities.LIGHTNING_ENERGY_BLOCK,
                AE2LTModuleBlockEntities.LIGHTNING_PATHWAY_BLOCK_ENTITY.get(),
                (be, side) -> be.getNet() == null
                        ? null
                        : new LightningUnifiedStorageHandler(be.getNet().getUnifiedStorage())
        );
    }
}
