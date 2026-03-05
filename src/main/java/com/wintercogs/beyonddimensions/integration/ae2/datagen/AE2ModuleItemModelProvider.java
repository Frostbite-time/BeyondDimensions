package com.wintercogs.beyonddimensions.integration.ae2.datagen;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.Datagen.helpers.BaseItemModelProvider;
import com.wintercogs.beyonddimensions.integration.ae2.init.AE2ModuleItems;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.NotNull;

public class AE2ModuleItemModelProvider extends BaseItemModelProvider
{

    public AE2ModuleItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper)
    {
        super(output, BeyondDimensions.MODID, existingFileHelper);
    }

    @Override
    public @NotNull String getName()
    {
        return "AE2 Module ItemModels";
    }

    @Override
    protected void registerModels()
    {
        basicItem(AE2ModuleItems.NET_AE_STORAGE_CELL.get());
    }
}
