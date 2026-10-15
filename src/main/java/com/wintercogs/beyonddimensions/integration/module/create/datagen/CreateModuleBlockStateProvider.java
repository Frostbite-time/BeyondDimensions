package com.wintercogs.beyonddimensions.integration.module.create.datagen;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.datagen.util.BDBlockStateProvider;
import com.wintercogs.beyonddimensions.integration.module.create.init.CreateModuleBlocks;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.NotNull;

public class CreateModuleBlockStateProvider extends BDBlockStateProvider
{

    public CreateModuleBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper)
    {
        super(output, exFileHelper);
    }

    @Override
    public @NotNull String getName()
    {
        return "BeyondDimensions CreateModule BlockState Provider";
    }

    @Override
    protected void registerStatesAndModels()
    {
        // 手写的模型，正面（图纸面）朝北，方块状态按朝向转动
        ModelFile model = models().getExistingFile(BeyondDimensions.makeId("schematicannon_pathway"));
        orientedBlock(CreateModuleBlocks.SCHEMATICANNON_PATHWAY.get(), state -> model);
        simpleBlockItem(CreateModuleBlocks.SCHEMATICANNON_PATHWAY.get(), model);
    }
}
