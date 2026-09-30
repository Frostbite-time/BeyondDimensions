package com.wintercogs.beyonddimensions.integration.module.emi.exclusion;

import com.wintercogs.beyonddimensions.client.ui.InventoryScreenAccess;
import dev.emi.emi.api.EmiExclusionArea;
import dev.emi.emi.api.widget.Bounds;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;

import java.util.function.Consumer;

// 让 EMI 避开面板外的按钮（模式按钮、侧边栏）；面板本身的区域由界面坐标给出
public class BDExclusionZones implements EmiExclusionArea<Screen>
{
    @Override
    public void addExclusionArea(Screen screen, Consumer<Bounds> consumer)
    {
        for (Rect2i area : InventoryScreenAccess.extraAreas(screen))
            consumer.accept(new Bounds(area.getX(), area.getY(), area.getWidth(), area.getHeight()));
    }
}
