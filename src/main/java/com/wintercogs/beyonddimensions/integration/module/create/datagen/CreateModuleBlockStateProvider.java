package com.wintercogs.beyonddimensions.integration.module.create.datagen;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.datagen.util.BDBlockStateProvider;
import com.wintercogs.beyonddimensions.integration.module.create.init.CreateModuleBlocks;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.ExistingFileHelper;

public class CreateModuleBlockStateProvider extends BDBlockStateProvider
{
    public CreateModuleBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper)
    {
        super(output, exFileHelper);
    }

    @Override
    public String getName()
    {
        return "BeyondDimensions CreateModule BlockState Provider";
    }

    @Override
    protected void registerStatesAndModels()
    {
        simpleBlockItem(CreateModuleBlocks.SCHEMATICANNON_PATHWAY.get(), models().getExistingFile(BeyondDimensions.makeId("schematicannon_pathway")));
    }
}
