package com.wintercogs.beyonddimensions.integration.module.botania.datagen;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.datagen.util.BDBlockStateProvider;
import com.wintercogs.beyonddimensions.integration.module.botania.init.BotaniaModuleBlocks;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.ExistingFileHelper;

public class BotaniaModuleBlockStateProvider extends BDBlockStateProvider
{
    public BotaniaModuleBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper)
    {
        super(output, exFileHelper);
    }

    @Override
    public String getName()
    {
        return "BeyondDimensions BotaniaModule BlockState Provider";
    }

    @Override
    protected void registerStatesAndModels()
    {
        simpleBlockItem(BotaniaModuleBlocks.MANA_POOL_PATHWAY.get(), models().getExistingFile(BeyondDimensions.makeId("mana_pool_pathway")));
    }
}
