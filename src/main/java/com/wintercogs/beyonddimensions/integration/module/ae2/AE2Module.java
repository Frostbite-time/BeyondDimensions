package com.wintercogs.beyonddimensions.integration.module.ae2;

import appeng.api.storage.StorageCells;
import com.wintercogs.beyonddimensions.integration.BDIntegrationModule;
import com.wintercogs.beyonddimensions.integration.IIntegrationModule;
import com.wintercogs.beyonddimensions.integration.OtherModIds;
import com.wintercogs.beyonddimensions.integration.module.ae2.datagen.AE2ModuleModelProvider;
import com.wintercogs.beyonddimensions.integration.module.ae2.datagen.AE2ModuleRecipeProvider;
import com.wintercogs.beyonddimensions.integration.module.ae2.init.AE2ModuleItems;
import com.wintercogs.beyonddimensions.integration.module.ae2.me.CellHandler;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.data.event.GatherDataEvent;

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
    public void onDatagen(GatherDataEvent.Client event)
    {
        event.createProvider(AE2ModuleModelProvider::new);
        event.createProvider(AE2ModuleRecipeProvider.Runner::new);
    }
}
