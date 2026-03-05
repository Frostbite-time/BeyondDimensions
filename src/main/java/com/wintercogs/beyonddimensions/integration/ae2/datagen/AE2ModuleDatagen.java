package com.wintercogs.beyonddimensions.integration.ae2.datagen;

import com.wintercogs.beyonddimensions.Datagen.DataGenerators;
import net.neoforged.neoforge.data.event.GatherDataEvent;

public class AE2ModuleDatagen
{
    public static void register()
    {
        DataGenerators.addAdditionalProvider(GatherDataEvent::includeServer, gatherDataEvent ->
                new AE2ModuleRecipeProvider(gatherDataEvent.getGenerator().getPackOutput(), gatherDataEvent.getLookupProvider())
        );
        DataGenerators.addAdditionalProvider(GatherDataEvent::includeServer, gatherDataEvent ->
                new AE2ModuleItemModelProvider(gatherDataEvent.getGenerator().getPackOutput(), gatherDataEvent.getExistingFileHelper())
        );
    }
}
