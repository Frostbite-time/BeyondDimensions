package com.wintercogs.beyonddimensions.integration.module.emi.exclusion;

import com.wintercogs.beyonddimensions.client.ui.InventoryScreenAccess;
import dev.compixel.forge.ComposeInventoryScreen;
import dev.emi.emi.api.EmiExclusionArea;
import dev.emi.emi.api.widget.Bounds;
import net.minecraft.client.gui.screens.Screen;

import java.util.function.Consumer;

// 让 EMI 的物品列表避开 Compose 界面实际占据的区域
public class BDExclusionZones implements EmiExclusionArea<Screen>
{
    @Override
    public void addExclusionArea(Screen screen, Consumer<Bounds> consumer)
    {
        if (!(screen instanceof ComposeInventoryScreen<?, ?, ?>))
            return;
        var area = InventoryScreenAccess.area(screen);
        if (area != null)
            consumer.accept(new Bounds(area.getX(), area.getY(), area.getWidth(), area.getHeight()));
    }
}
