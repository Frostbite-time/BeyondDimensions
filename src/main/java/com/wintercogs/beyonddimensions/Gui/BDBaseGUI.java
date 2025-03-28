package com.wintercogs.beyonddimensions.Gui;

import com.cleanroommc.modularui.api.IGuiHolder;
import com.cleanroommc.modularui.factory.GuiData;
import com.cleanroommc.modularui.factory.SimpleGuiFactory;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.value.sync.GuiSyncManager;

public class BDBaseGUI implements IGuiHolder<GuiData>
{

    public static SimpleGuiFactory factory =  new SimpleGuiFactory("test",() ->{
        return new BDBaseGUI();
    });

    @Override
    public ModularPanel buildUI(GuiData guiData, GuiSyncManager guiSyncManager)
    {
        ModularPanel panel = ModularPanel.defaultPanel("test")
                .bindPlayerInventory();
        return panel;
    }
}
