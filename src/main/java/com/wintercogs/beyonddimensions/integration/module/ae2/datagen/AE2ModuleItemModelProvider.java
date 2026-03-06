package com.wintercogs.beyonddimensions.integration.module.ae2.datagen;

import com.wintercogs.beyonddimensions.datagen.util.BDItemModelProvider;
import com.wintercogs.beyonddimensions.integration.module.ae2.init.AE2ModuleItems;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.NotNull;

public class AE2ModuleItemModelProvider extends BDItemModelProvider
{
    public AE2ModuleItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper)
    {
        super(output, existingFileHelper);
    }

    @Override
    public @NotNull String getName()
    {
        return "BeyondDimensions AE2Module ItemModel Provider";
    }

    @Override
    protected void registerModels()
    {
        basicItem(AE2ModuleItems.NET_AE_STORAGE_CELL.get());
    }
}
