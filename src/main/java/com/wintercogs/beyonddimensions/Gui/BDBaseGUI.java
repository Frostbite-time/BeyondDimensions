package com.wintercogs.beyonddimensions.Gui;

import com.cleanroommc.modularui.api.IGuiHolder;
import com.cleanroommc.modularui.factory.GuiData;
import com.cleanroommc.modularui.factory.SimpleGuiFactory;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.value.sync.GuiSyncManager;
import com.cleanroommc.modularui.widgets.FluidSlot;
import com.wintercogs.beyonddimensions.DataBase.DimensionsNet;

public class BDBaseGUI implements IGuiHolder<GuiData>
{

    public static SimpleGuiFactory factory =  new SimpleGuiFactory("test",() ->{
        return new BDBaseGUI();
    });

    @Override
    public ModularPanel buildUI(GuiData guiData, GuiSyncManager guiSyncManager)
    {
        DimensionsNet dimensionsNet = DimensionsNet.getNetFromPlayer(guiData.getPlayer());
        ModularPanel panel = ModularPanel.defaultPanel("test")
                .bindPlayerInventory();
        return panel;
    }
}
