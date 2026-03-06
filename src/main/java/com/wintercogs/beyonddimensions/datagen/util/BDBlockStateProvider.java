package com.wintercogs.beyonddimensions.datagen.util;

import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.NotNull;

public abstract class BDBlockStateProvider extends BlockStateProvider
{
    public BDBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper)
    {
        super(output, BDConstants.MODID, exFileHelper);
    }

    @Override
    @NotNull
    public abstract String getName();

    protected void blockWithItem(RegistryObject<Block> deferredBlock)
    {
        simpleBlockWithItem(deferredBlock.get(), cubeAll(deferredBlock.get()));
    }
}
