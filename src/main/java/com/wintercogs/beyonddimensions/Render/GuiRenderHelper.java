package com.wintercogs.beyonddimensions.Render;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

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
            Identifier texture,
            int x, int y,
            int width, int height,
            int borderTop, int borderBottom,
            int borderLeft, int borderRight,
            int origWidth, int origHeight)
    {

        // === 1. 四个角（不拉伸） ===
        // 左上
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, texture,
                x, y,
                0F, 0F,
                borderLeft, borderTop,
                borderLeft, borderTop,
                origWidth, origHeight);

        // 右上
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, texture,
                x + width - borderRight, y,
                origWidth - borderRight, 0F,
                borderRight, borderTop,
                borderRight, borderTop,
                origWidth, origHeight);

        // 左下
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, texture,
                x, y + height - borderBottom,
                0F, origHeight - borderBottom,
                borderLeft, borderBottom,
                borderLeft, borderBottom,
                origWidth, origHeight);

        // 右下
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, texture,
                x + width - borderRight, y + height - borderBottom,
                origWidth - borderRight, origHeight - borderBottom,
                borderRight, borderBottom,
                borderRight, borderBottom,
                origWidth, origHeight);

        // === 2. 四条边（单向拉伸） ===
        int dstEdgeW = width - borderLeft - borderRight;
        int dstEdgeH = height - borderTop - borderBottom;
        int srcEdgeW = origWidth - borderLeft - borderRight;
        int srcEdgeH = origHeight - borderTop - borderBottom;

        // 上边
        if (borderTop > 0)
        {
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, texture,
                    x + borderLeft, y,
                    borderLeft, 0F,
                    dstEdgeW, borderTop,
                    srcEdgeW, borderTop,
                    origWidth, origHeight);
        }

        // 下边
        if (borderBottom > 0)
        {
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, texture,
                    x + borderLeft, y + height - borderBottom,
                    borderLeft, origHeight - borderBottom,
                    dstEdgeW, borderBottom,
                    srcEdgeW, borderBottom,
                    origWidth, origHeight);
        }

        // 左边
        if (borderLeft > 0)
        {
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, texture,
                    x, y + borderTop,
                    0F, borderTop,
                    borderLeft, dstEdgeH,
                    borderLeft, srcEdgeH,
                    origWidth, origHeight);
        }

        // 右边
        if (borderRight > 0)
        {
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, texture,
                    x + width - borderRight, y + borderTop,
                    origWidth - borderRight, borderTop,
                    borderRight, dstEdgeH,
                    borderRight, srcEdgeH,
                    origWidth, origHeight);
        }

        // === 3. 中心（双向拉伸） ===
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, texture,
                x + borderLeft, y + borderTop,
                borderLeft, borderTop,
                dstEdgeW, dstEdgeH,
                srcEdgeW, srcEdgeH,
                origWidth, origHeight);
    }

    /**
     * 绘制整张纹理并缩放到指定宽高
     * <p>已帮你绑定 shader & 纹理；调用方不用再 setShaderTexture。</p>
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
            Identifier texture,
            int x, int y,
            int width, int height,
            int originalWidth, int originalHeight)
    {
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, texture,
                x, y,                     // 目标起点
                0F, 0F,                   // 纹理起点 (u,v)
                width, height,            // 目标尺寸
                originalWidth, originalHeight,               // 采样整张纹理
                originalWidth, originalHeight);              // 纹理原尺寸（用于 UV 归一化）
    }

    public static void drawRightAnchoredText(GuiGraphics guiGraphics,
                                             Font font,
                                             Component text,      // 要绘的文字
                                             int xRight,          // 想让文字右边对齐到的 x 坐标
                                             int y,               // y 坐标
                                             int color,
                                             boolean dropShadow)
    {         // 颜色 0xAARRGGBB
        // 1. 计算文字宽度
        int width = font.width(text);

        // 2. 计算左上角起点
        int xStart = xRight - width;

        // 3. 绘制
        guiGraphics.drawString(font, text, xStart, y, color, dropShadow);
    }


}
