package com.wintercogs.beyonddimensions.integration.module.ae2.datagen;

import com.wintercogs.beyonddimensions.datagen.util.BDModelProvider;
import com.wintercogs.beyonddimensions.integration.module.ae2.init.AE2ModuleItems;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.core.Holder;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Stream;

public class AE2ModuleModelProvider extends BDModelProvider
{
    public AE2ModuleModelProvider(PackOutput output)
    {
        super(output);
    }

    @Override
    public @NotNull String getName()
    {
        return "BeyondDimensions AE2Module Model Provider";
    }

    @Override
    protected void registerModels(@NotNull BlockModelGenerators blockModels, @NotNull ItemModelGenerators itemModels)
    {
        itemModels.generateFlatItem(AE2ModuleItems.NET_AE_STORAGE_CELL.get(), ModelTemplates.FLAT_ITEM);
    }

    @Override
    protected @NotNull Stream<? extends Holder<Block>> getKnownBlocks()
    {
        return Stream.empty();
    }

    @Override
    protected @NotNull Stream<? extends Holder<Item>> getKnownItems()
    {
        return AE2ModuleItems.ITEMS.getEntries().stream();
    }
}
