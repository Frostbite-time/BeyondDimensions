package com.wintercogs.beyonddimensions.integration.module.polymorph;

import com.illusivesoulworks.polymorph.api.PolymorphApi;
import com.wintercogs.beyonddimensions.client.gui.DimensionsCraftGUI;
import com.wintercogs.beyonddimensions.integration.BDIntegrationClientModule;
import com.wintercogs.beyonddimensions.integration.IIntegrationClientModule;
import com.wintercogs.beyonddimensions.integration.OtherModIds;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@BDIntegrationClientModule(modId = OtherModIds.POLYMORPH)
public class PolymorphClientModule implements IIntegrationClientModule
{
    @Override
    public String modId()
    {
        return OtherModIds.POLYMORPH;
    }

    @Override
    public void onBootstrapClient(IEventBus modBus, IEventBus gameBus)
    {

    }

    @Override
    public void onClientSetup(FMLClientSetupEvent event)
    {
        PolymorphApi.client().registerWidget(screen -> {
            if (screen instanceof DimensionsCraftGUI<?> gui)
                return new RecipeWidget(gui, gui.getMenu().getSlot(gui.getMenu().resultSlotIndex));

            return null;
        });
    }
}
