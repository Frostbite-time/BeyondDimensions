package com.wintercogs.beyonddimensions.datagen.util;

import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredBlock;
import org.jetbrains.annotations.NotNull;

public abstract class BDBlockStateProvider extends BlockStateProvider
{
    public BDBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper)
    {
        super(output, BDConstants.MODID, exFileHelper);
    }

    @Override
    public abstract @NotNull String getName();

    protected void blockWithItem(DeferredBlock<?> deferredBlock)
    {
        simpleBlockWithItem(deferredBlock.get(), cubeAll(deferredBlock.get()));
    }
}
