package com.wintercogs.beyonddimensions.integration.JEI.ContainerHandler;

import com.wintercogs.beyonddimensions.client.gui.BDBaseGUI;
import com.wintercogs.beyonddimensions.client.gui.widget.shared.GuiElementAccess;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.renderer.Rect2i;

import java.util.ArrayList;
import java.util.List;

// 定义UI在JEI中的显示方式与额外信息
public class JeiContainerHandler implements IGuiContainerHandler<BDBaseGUI<?>>
{
    @Override
    public List<Rect2i> getGuiExtraAreas(BDBaseGUI<?> containerScreen)
    {
        List<Rect2i> areas = new ArrayList<>();
        for (Renderable renderable : containerScreen.renderables)
        {
            if (renderable instanceof GuiElementAccess access)
            {
                areas.add(access.getElementArea());
            }
        }
        return areas;
    }
}
