package com.wintercogs.beyonddimensions.integration.module.ae2lt.datagen;

import com.wintercogs.beyonddimensions.datagen.util.BDBlockStateProvider;
import com.wintercogs.beyonddimensions.integration.module.ae2lt.init.AE2LTModuleBlocks;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.NotNull;

public class AE2LTModuleBlockStateProvider extends BDBlockStateProvider
{
    public AE2LTModuleBlockStateProvider(PackOutput output, ExistingFileHelper helper)
    {
        super(output, helper);
    }

    @Override
    public @NotNull String getName()
    {
        return "BeyondDimensions AE2LT Module BlockState Provider";
    }

    @Override
    protected void registerStatesAndModels()
    {
        blockWithItem(AE2LTModuleBlocks.LIGHTNING_PATHWAY);
    }
}
