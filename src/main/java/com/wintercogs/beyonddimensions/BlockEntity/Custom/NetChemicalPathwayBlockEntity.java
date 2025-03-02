package com.wintercogs.beyonddimensions.BlockEntity.Custom;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.BlockEntity.ModBlockEntities;
import com.wintercogs.beyonddimensions.DataBase.DimensionsNet;
import com.wintercogs.beyonddimensions.Integration.Mek.Capability.ChemicalCapabilityHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;


public class NetChemicalPathwayBlockEntity extends NetedBlockEntity
{


    public NetChemicalPathwayBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.NET_CHEMICAL_PATHWAY_BLOCK_ENTITY.get(), pos, blockState);
    }

    //--- 能力注册 (通过事件) ---
    public static void registerCapability(RegisterCapabilitiesEvent event) {
        if(!BeyondDimensions.MekLoaded || ChemicalCapabilityHelper.CHEMICAL == null)
            return;
        event.registerBlockEntity(
                ChemicalCapabilityHelper.CHEMICAL,
                ModBlockEntities.NET_CHEMICAL_PATHWAY_BLOCK_ENTITY.get(),
                (be, side) -> {
                        if (be.getNetId() < 0) return null;
                        DimensionsNet net = be.getNet();
                        return (net != null) ? net.getChemicalStorage() : null;
                    }
        );
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries)
    {
        super.loadAdditional(tag,registries);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries)
    {
        super.saveAdditional(tag, registries);
    }

}
