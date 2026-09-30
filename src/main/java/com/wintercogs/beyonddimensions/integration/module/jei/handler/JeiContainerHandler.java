package com.wintercogs.beyonddimensions.integration.module.jei.handler;

import com.wintercogs.beyonddimensions.client.ui.InventoryScreenAccess;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;

import java.util.List;

// 让 JEI 避开面板外的按钮（模式按钮、侧边栏）；面板本身的区域由界面坐标给出
public class JeiContainerHandler implements IGuiContainerHandler<AbstractContainerScreen<?>>
{
    @Override
    public List<Rect2i> getGuiExtraAreas(AbstractContainerScreen<?> containerScreen)
    {
        return InventoryScreenAccess.extraAreas(containerScreen);
    }
}
