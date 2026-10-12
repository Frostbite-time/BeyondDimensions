package com.wintercogs.beyonddimensions.integration.module.jei.handler;

import com.wintercogs.beyonddimensions.client.ui.InventoryScreenAccess;
import dev.compixel.forge.ComposeInventoryScreen;
import mezz.jei.api.gui.handlers.IGuiProperties;
import mezz.jei.api.gui.handlers.IScreenHandler;
import net.minecraft.client.gui.screens.Screen;

/**
 * 向 JEI 报告 Compose 界面实际占据的区域，使 JEI 的物品列表避开界面
 */
public final class ComposeJeiScreenHandler<S extends ComposeInventoryScreen<?, ?, ?>> implements IScreenHandler<S>
{
    @Override
    public IGuiProperties apply(S screen)
    {
        var area = InventoryScreenAccess.area(screen);
        if (area == null)
            return null;
        return new Properties(screen.getClass(), area.getX(), area.getY(), area.getWidth(), area.getHeight(), screen.width, screen.height);
    }

    private record Properties(Class<? extends Screen> screenClass, int guiLeft, int guiTop, int guiXSize, int guiYSize,
                              int screenWidth, int screenHeight) implements IGuiProperties
    {
    }
}
