package com.wintercogs.beyonddimensions.integration.module.ifs.datagen;

import com.wintercogs.beyonddimensions.datagen.util.BDItemModelProvider;
import com.wintercogs.beyonddimensions.integration.module.ifs.init.IFSModuleItems;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.NotNull;

public class IFSModuleItemModelProvider extends BDItemModelProvider
{
    public IFSModuleItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper)
    {
        super(output, existingFileHelper);
    }

    @Override
    public @NotNull String getName()
    {
        return "BeyondDimensions IFSModule ItemModel Provider";
    }

    @Override
    protected void registerModels()
    {
        basicItem(IFSModuleItems.WARDEN_SOUL_TAG_ITEM.get());
    }
}
