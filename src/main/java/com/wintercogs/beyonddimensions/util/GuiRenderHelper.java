package com.wintercogs.beyonddimensions.util;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class GuiRenderHelper
{
    /**
     * 绘制具有边框的九宫格纹理，并自动处理拉伸（统一使用 9 参 blit）
     *
     * @param guiGraphics  GUI 渲染上下文
     * @param texture      纹理资源位置
     * @param x            目标位置 X
     * @param y            目标位置 Y
     * @param width        目标总宽度
     * @param height       目标总高度
     * @param borderTop    上边框大小 (像素)
     * @param borderBottom 下边框大小 (像素)
     * @param borderLeft   左边框大小 (像素)
     * @param borderRight  右边框大小 (像素)
     * @param origWidth    原始纹理宽度
     * @param origHeight   原始纹理高度
     */
    public static void renderBorderedPanel(
            GuiGraphics guiGraphics,
            ResourceLocation texture,
            int x, int y,
            int width, int height,
            int borderTop, int borderBottom,
            int borderLeft, int borderRight,
            int origWidth, int origHeight)
    {

        // === 四个角（不拉伸） ===
        // 左上
        guiGraphics.blit(texture,
                x, y,
                borderLeft, borderTop,
                0, 0,
                borderLeft, borderTop,
                origWidth, origHeight);

        // 右上
        guiGraphics.blit(texture,
                x + width - borderRight, y,
                borderRight, borderTop,
                origWidth - borderRight, 0,
                borderRight, borderTop,
                origWidth, origHeight);

        // 左下
        guiGraphics.blit(texture,
                x, y + height - borderBottom,
                borderLeft, borderBottom,
                0, origHeight - borderBottom,
                borderLeft, borderBottom,
                origWidth, origHeight);

        // 右下
        guiGraphics.blit(texture,
                x + width - borderRight, y + height - borderBottom,
                borderRight, borderBottom,
                origWidth - borderRight, origHeight - borderBottom,
                borderRight, borderBottom,
                origWidth, origHeight);

        // === 四条边（单向拉伸） ===
        int dstEdgeW = width - borderLeft - borderRight;
        int dstEdgeH = height - borderTop - borderBottom;
        int srcEdgeW = origWidth - borderLeft - borderRight;
        int srcEdgeH = origHeight - borderTop - borderBottom;

        // 上边
        if (borderTop > 0)
        {
            guiGraphics.blit(texture,
                    x + borderLeft, y,
                    dstEdgeW, borderTop,
                    borderLeft, 0,
                    srcEdgeW, borderTop,
                    origWidth, origHeight);
        }

        // 下边
        if (borderBottom > 0)
        {
            guiGraphics.blit(texture,
                    x + borderLeft, y + height - borderBottom,
                    dstEdgeW, borderBottom,
                    borderLeft, origHeight - borderBottom,
                    srcEdgeW, borderBottom,
                    origWidth, origHeight);
        }

        // 左边
        if (borderLeft > 0)
        {
            guiGraphics.blit(texture,
                    x, y + borderTop,
                    borderLeft, dstEdgeH,
                    0, borderTop,
                    borderLeft, srcEdgeH,
                    origWidth, origHeight);
        }

        // 右边
        if (borderRight > 0)
        {
            guiGraphics.blit(texture,
                    x + width - borderRight, y + borderTop,
                    borderRight, dstEdgeH,
                    origWidth - borderRight, borderTop,
                    borderRight, srcEdgeH,
                    origWidth, origHeight);
        }

        // === 中心（双向拉伸） ===
        guiGraphics.blit(texture,
                x + borderLeft, y + borderTop,
                dstEdgeW, dstEdgeH,
                borderLeft, borderTop,
                srcEdgeW, srcEdgeH,
                origWidth, origHeight);
    }

    /**
     * 绘制整张纹理并缩放到指定宽高
     *
     * @param guiGraphics 渲染上下文
     * @param texture     纹理资源路径（不需要是在图集里的）
     * @param x           目标左上角 X
     * @param y           目标左上角 Y
     * @param width       希望绘制出的宽度
     * @param height      希望绘制出的高度
     */
    public static void renderFullTexture(
            GuiGraphics guiGraphics,
            ResourceLocation texture,
            int x, int y,
            int width, int height,
            int originalWidth, int originalHeight)
    {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, texture);

        guiGraphics.blit(texture,
                x, y,
                width, height,
                0, 0,
                originalWidth, originalHeight,
                originalWidth, originalHeight);
    }

    /**
     * @param guiGraphics 渲染上下文
     * @param font        字体
     * @param text        绘制文本
     * @param xRight      右对齐情况下的x坐标
     * @param y           y坐标
     * @param color       字体颜色
     * @param dropShadow  是否绘制字体阴影
     */
    public static void drawRightAnchoredText(GuiGraphics guiGraphics,
                                             Font font,
                                             Component text,
                                             int xRight,
                                             int y,
                                             int color,
                                             boolean dropShadow)
    {
        int width = font.width(text);
        int xStart = xRight - width;
        guiGraphics.drawString(font, text, xStart, y, color, dropShadow);
    }
}
