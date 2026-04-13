package com.wintercogs.beyonddimensions.integration.module.rs.datagen;

import com.wintercogs.beyonddimensions.datagen.util.BDModelProvider;
import com.wintercogs.beyonddimensions.integration.module.rs.init.RSModuleBlocks;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.data.PackOutput;
import org.jetbrains.annotations.NotNull;

public class RSModuleModelProvider extends BDModelProvider
{

    public RSModuleModelProvider(PackOutput output)
    {
        super(output);
    }

    @Override
    public @NotNull String getName()
    {
        return "BeyondDimensions RSModule BlockState Provider";
    }

    @Override
    protected void registerModels(@NotNull BlockModelGenerators blockModels, @NotNull ItemModelGenerators itemModels)
    {
        blockWithItem(blockModels, RSModuleBlocks.RS_NET_PATHWAY);
    }
}
