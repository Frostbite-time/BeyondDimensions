package com.wintercogs.beyonddimensions.integration.module.polymorph;

import com.illusivesoulworks.polymorph.api.client.PolymorphWidgets;
import com.wintercogs.beyonddimensions.client.ui.storage.CraftScreen;
import com.wintercogs.beyonddimensions.integration.BDIntegrationClientModule;
import com.wintercogs.beyonddimensions.integration.IIntegrationClientModule;
import com.wintercogs.beyonddimensions.integration.OtherModIds;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

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
        PolymorphWidgets.getInstance().registerWidget(screen -> {
            // 界面按 Compose 布局同步槽位坐标，Polymorph 的按钮仍然出现在产物槽旁
            if (screen instanceof CraftScreen<?> gui)
                return new RecipeWidget(gui, gui.getMenu().getSlot(gui.getMenu().resultSlotIndex));

            return null;
        });
    }
}
