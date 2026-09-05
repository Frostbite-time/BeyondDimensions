package com.wintercogs.beyonddimensions.client.gui.widget;

import com.wintercogs.beyonddimensions.client.gui.widget.shared.GuiElementAccess;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.renderer.Rect2i;
import org.jetbrains.annotations.NotNull;

/**
 * 无背景的左侧按钮栏，按添加顺序纵向排列，尺寸随按钮自动计算。
 */
public class LeftButtonSidebar extends LinearLayout implements Renderable, GuiElementAccess
{
    public LeftButtonSidebar(int x, int y)
    {
        super(x, y, Orientation.VERTICAL);
        spacing(2);
    }

    /**
     * 添加并排列按钮；返回原实例，供界面通过 addRenderableWidget 注册渲染和输入。
     */
    public <T extends AbstractButton> T addButton(T button)
    {
        addChild(button);
        arrangeElements();
        return button;
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick)
    {
        // 按钮由界面渲染；侧边栏仅注册到 renderables，供 JEI/EMI 获取整体避让区域。
    }

    @Override
    public Rect2i getElementArea()
    {
        return new Rect2i(getX(), getY(), getWidth(), getHeight());
    }
}
