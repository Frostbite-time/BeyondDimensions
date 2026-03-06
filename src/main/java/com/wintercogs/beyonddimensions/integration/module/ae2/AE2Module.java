package com.wintercogs.beyonddimensions.integration.module.ae2;

import appeng.api.storage.StorageCells;
import com.wintercogs.beyonddimensions.integration.BDIntegrationModule;
import com.wintercogs.beyonddimensions.integration.IIntegrationModule;
import com.wintercogs.beyonddimensions.integration.OtherModIds;
import com.wintercogs.beyonddimensions.integration.module.ae2.datagen.AE2ModuleItemModelProvider;
import com.wintercogs.beyonddimensions.integration.module.ae2.datagen.AE2ModuleRecipeProvider;
import com.wintercogs.beyonddimensions.integration.module.ae2.init.AE2ModuleItems;
import com.wintercogs.beyonddimensions.integration.module.ae2.me.CellHandler;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.concurrent.CompletableFuture;

@BDIntegrationModule(modId = OtherModIds.AE2)
public class AE2Module implements IIntegrationModule
{
    @Override
    public String modId()
    {
        return OtherModIds.AE2;
    }

    @Override
    public void onBootstrap(IEventBus modBus, IEventBus gameBus)
    {
        AE2ModuleItems.register(modBus);
    }

    @Override
    public void onCommonSetup(FMLCommonSetupEvent event)
    {
        StorageCells.addCellHandler(CellHandler.INSTANCE);
    }

    @Override
    public void onItemCreativeTabCollect(CreativeModeTab.ItemDisplayParameters displayParameters, CreativeModeTab.Output output)
    {
        output.accept(AE2ModuleItems.NET_AE_STORAGE_CELL);
    }

    @Override
    public void onDatagen(GatherDataEvent event)
    {
        DataGenerator generator = event.getGenerator();
        PackOutput packOutput = generator.getPackOutput();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();

        generator.addProvider(event.includeClient(), new AE2ModuleItemModelProvider(packOutput, existingFileHelper));
        generator.addProvider(event.includeServer(), new AE2ModuleRecipeProvider(packOutput, lookupProvider));
    }
}
