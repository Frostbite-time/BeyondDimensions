package com.wintercogs.beyonddimensions.integration.module.rs.datagen;

import com.wintercogs.beyonddimensions.datagen.util.BDBlockStateProvider;
import com.wintercogs.beyonddimensions.integration.module.rs.init.RSModuleBlocks;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.ExistingFileHelper;

public class RSModuleBlockStateProvider extends BDBlockStateProvider
{
    public RSModuleBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper)
    {
        super(output, exFileHelper);
    }

    @Override
    public String getName()
    {
        return "BeyondDimensions RSModule BlockState Provider";
    }

    @Override
    protected void registerStatesAndModels()
    {
        blockWithItem(RSModuleBlocks.RS_NET_PATHWAY);
    }
}
