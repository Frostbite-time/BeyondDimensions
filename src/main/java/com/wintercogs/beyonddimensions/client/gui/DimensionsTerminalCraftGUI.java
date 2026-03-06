package com.wintercogs.beyonddimensions.client.gui;

import com.wintercogs.beyonddimensions.common.menu.DimensionsCraftMenuTerminal;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class DimensionsTerminalCraftGUI extends DimensionsCraftGUI<DimensionsCraftMenuTerminal>
{

    public DimensionsTerminalCraftGUI(DimensionsCraftMenuTerminal container, Inventory playerInventory, Component title)
    {
        super(container, playerInventory, title);
    }

    @Override
    protected void init()
    {
        super.init();
    }

    @Override
    protected void addCraftButton()
    {
        // 清空
    }
}
