package com.wintercogs.beyonddimensions.Gui;

import com.cleanroommc.modularui.factory.GuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.value.sync.GuiSyncManager;

public class BDBaseGUI
{
    public static ModularPanel createPanel(GuiData guiData, GuiSyncManager guiSyncManager)
    {
        ModularPanel panel = ModularPanel.defaultPanel("test");
        return panel;
    }
}
