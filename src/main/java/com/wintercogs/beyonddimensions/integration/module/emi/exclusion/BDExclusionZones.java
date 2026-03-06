package com.wintercogs.beyonddimensions.integration.module.emi.exclusion;

import com.wintercogs.beyonddimensions.client.gui.BDBaseGUI;
import com.wintercogs.beyonddimensions.client.gui.widget.shared.GuiElementAccess;
import dev.emi.emi.api.EmiExclusionArea;
import dev.emi.emi.api.widget.Bounds;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;

import java.util.function.Consumer;

public class BDExclusionZones implements EmiExclusionArea<Screen>
{

    @Override
    public void addExclusionArea(Screen screen, Consumer<Bounds> consumer)
    {
        if (screen instanceof BDBaseGUI<?>)
        {
            for (Renderable renderable : screen.renderables) // 仅为可渲染元素创造避让区域
            {
                if (renderable instanceof GuiElementAccess access)
                {
                    Rect2i area = access.getElementArea();
                    consumer.accept(new Bounds(area.getX(), area.getY(), area.getWidth(), area.getHeight()));
                }
            }
        }
    }
}
