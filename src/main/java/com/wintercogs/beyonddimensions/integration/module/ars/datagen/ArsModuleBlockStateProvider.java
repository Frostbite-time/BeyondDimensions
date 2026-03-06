package com.wintercogs.beyonddimensions.integration.module.ars.datagen;

import com.wintercogs.beyonddimensions.datagen.util.BDBlockStateProvider;
import com.wintercogs.beyonddimensions.integration.module.ars.init.ArsModuleBlocks;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.NotNull;

public class ArsModuleBlockStateProvider extends BDBlockStateProvider
{

    public ArsModuleBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper)
    {
        super(output, exFileHelper);
    }

    @Override
    public @NotNull String getName()
    {
        return "BeyondDimensions ArsModule BlockState Provider";
    }

    @Override
    protected void registerStatesAndModels()
    {
        blockWithItem(ArsModuleBlocks.ARS_SOURCE_PATHWAY);
    }
}
