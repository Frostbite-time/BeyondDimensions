package com.wintercogs.beyonddimensions.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.util.Mth;

// 提供便捷的，渲染CommonTextures中纹理的函数
// 与CommonTextures分开的目的是防止服务端出错（同时也便于服务端读取CommonTextures中的高度来确定slot位置）
public class CommonTexturesRender
{
    // TOP_BASE_COMMON 渲染方法
    public static void renderTopBaseCommon(GuiGraphicsExtractor guiGraphics, int leftPos, int[] yPosRef)
    {
        renderTopBaseCommon(guiGraphics, leftPos, yPosRef,
                CommonTextures.TOP_BASE_COMMON_WIDTH,
                CommonTextures.TOP_BASE_COMMON_HEIGHT);
    }

    public static void renderTopBaseCommon(GuiGraphicsExtractor guiGraphics, int leftPos, int[] yPosRef,
                                           int width, int height)
    {
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, CommonTextures.TOP_BASE_COMMON, leftPos, yPosRef[0],
                0F, 0F,
                width, height,
                CommonTextures.TOP_BASE_COMMON_WIDTH,
                CommonTextures.TOP_BASE_COMMON_HEIGHT);
        yPosRef[0] += height;
    }

    // COMMON_CONNECTION 渲染方法
    public static void renderCommonConnection(GuiGraphicsExtractor guiGraphics, int leftPos, int[] yPosRef)
    {
        renderCommonConnection(guiGraphics, leftPos, yPosRef,
                CommonTextures.COMMON_CONNECTION_WIDTH,
                CommonTextures.COMMON_CONNECTION_HEIGHT);
    }

    public static void renderCommonConnection(GuiGraphicsExtractor guiGraphics, int leftPos, int[] yPosRef,
                                              int width, int height)
    {
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, CommonTextures.COMMON_CONNECTION, leftPos, yPosRef[0],
                0F, 0F,
                width, height,
                CommonTextures.COMMON_CONNECTION_WIDTH,
                CommonTextures.COMMON_CONNECTION_HEIGHT);
        yPosRef[0] += height;
    }

    public static void renderBottomBaseCommon(GuiGraphicsExtractor guiGraphics, int leftPos, int[] yPosRef)
    {
        renderBottomBaseCommon(guiGraphics, leftPos, yPosRef,
                CommonTextures.BOTTOM_BASE_COMMON_WIDTH,
                CommonTextures.BOTTOM_BASE_COMMON_HEIGHT);
    }

    public static void renderBottomBaseCommon(GuiGraphicsExtractor guiGraphics, int leftPos, int[] yPosRef,
                                              int width, int height)
    {
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, CommonTextures.BOTTOM_BASE_COMMON, leftPos, yPosRef[0],
                0F, 0F,
                width, height,
                CommonTextures.BOTTOM_BASE_COMMON_WIDTH,
                CommonTextures.BOTTOM_BASE_COMMON_HEIGHT);
        yPosRef[0] += height;
    }

    // COMMON_SLOTS 渲染方法
    public static void renderCommonSlots(GuiGraphicsExtractor guiGraphics, int leftPos, int[] yPosRef)
    {
        renderCommonSlots(guiGraphics, leftPos, yPosRef,
                CommonTextures.COMMON_SLOTS_WIDTH,
                CommonTextures.COMMON_SLOTS_HEIGHT);
    }

    public static void renderCommonSlots(GuiGraphicsExtractor guiGraphics, int leftPos, int[] yPosRef,
                                         int width, int height)
    {
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, CommonTextures.COMMON_SLOTS, leftPos, yPosRef[0],
                0F, 0F,
                width, height,
                CommonTextures.COMMON_SLOTS_WIDTH,
                CommonTextures.COMMON_SLOTS_HEIGHT);
        yPosRef[0] += height;
    }

    // FILTER_SLOTS 渲染方法
    public static void renderFilterSlots(GuiGraphicsExtractor guiGraphics, int leftPos, int[] yPosRef)
    {
        renderFilterSlots(guiGraphics, leftPos, yPosRef,
                CommonTextures.FILTER_SLOTS_WIDTH,
                CommonTextures.FILTER_SLOTS_HEIGHT);
    }

    public static void renderFilterSlots(GuiGraphicsExtractor guiGraphics, int leftPos, int[] yPosRef,
                                         int width, int height)
    {
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, CommonTextures.FILTER_SLOTS, leftPos, yPosRef[0],
                0F, 0F,
                width, height,
                CommonTextures.FILTER_SLOTS_WIDTH,
                CommonTextures.FILTER_SLOTS_HEIGHT);
        yPosRef[0] += height;
    }

    // PLAYER_INV 渲染方法
    public static void renderPlayerInv(GuiGraphicsExtractor guiGraphics, int leftPos, int[] yPosRef)
    {
        renderPlayerInv(guiGraphics, leftPos, yPosRef,
                CommonTextures.PLAYER_INV_WIDTH,
                CommonTextures.PLAYER_INV_HEIGHT);
    }

    public static void renderPlayerInv(GuiGraphicsExtractor guiGraphics, int leftPos, int[] yPosRef,
                                       int width, int height)
    {
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, CommonTextures.GUI_TEXTURE_PLAYER_INV, leftPos, yPosRef[0],
                0F, 0F,
                width, height,
                CommonTextures.PLAYER_INV_WIDTH,
                CommonTextures.PLAYER_INV_HEIGHT);
        yPosRef[0] += height;
    }

    // 右标签渲染
    public static void renderRightTab(GuiGraphicsExtractor guiGraphics, int leftPos, int[] yPosRef)
    {
        renderRightTab(guiGraphics, leftPos, yPosRef,
                CommonTextures.RIGHT_TAB_WIDTH,
                CommonTextures.RIGHT_TAB_HEIGHT);
    }

    public static void renderRightTab(GuiGraphicsExtractor guiGraphics, int leftPos, int[] yPosRef,
                                      int width, int height)
    {
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, CommonTextures.RIGHT_TAB, leftPos, yPosRef[0], width, height);
        yPosRef[0] += height;
    }

    public static void renderNetFurnaceBackground(GuiGraphicsExtractor guiGraphics, int leftPos, int[] yPosRef)
    {
        renderNetFurnaceBackground(guiGraphics, leftPos, yPosRef,
                CommonTextures.NET_FURNACE_BACKGROUND_WIDTH,
                CommonTextures.NET_FURNACE_BACKGROUND_HEIGHT);
    }

    public static void renderNetFurnaceBackground(GuiGraphicsExtractor guiGraphics, int leftPos, int[] yPosRef,
                                                  int width, int height)
    {
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, CommonTextures.NET_FURNACE_BACKGROUND, leftPos, yPosRef[0],
                0F, 0F,
                width, height,
                CommonTextures.NET_FURNACE_BACKGROUND_WIDTH,
                CommonTextures.NET_FURNACE_BACKGROUND_HEIGHT);
        yPosRef[0] += height;
    }

    public static void renderWorkDoneV(GuiGraphicsExtractor guiGraphics, int leftPos, int[] yPosRef)
    {
        renderWorkDoneV(guiGraphics, leftPos, yPosRef,
                CommonTextures.WORK_DONE_V_WIDTH,
                CommonTextures.WORK_DONE_V_HEIGHT);
    }

    public static void renderWorkDoneV(GuiGraphicsExtractor guiGraphics, int leftPos, int[] yPosRef,
                                       int width, int height)
    {
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, CommonTextures.WORK_DONE_V, leftPos, yPosRef[0], width, height);
        yPosRef[0] += height;
    }

    public static void renderWorkDoneV_AsProgress(GuiGraphicsExtractor guiGraphics, int leftPos, int[] yPosRef,
                                                  int width, int height, float progress) //从上往下
    {
        // 0. 约束进度
        progress = Mth.clamp(progress, 0f, 1f);
        if (progress <= 0f) return;          // 0% 不绘制

        /* 2. 贴图需要的 V 高度 —— 按同样比例裁剪 */
        int vHeight = (int) (CommonTextures.WORK_DONE_V_HEIGHT * progress);

        /* 3. blit：
          ‑ 只画贴图的顶部 vHeight 像素；
          ‑ 目标区域从 (leftPos, yPos) 开始往下铺 drawH 像素 */
        guiGraphics.blitSprite(
                RenderPipelines.GUI_TEXTURED,
                CommonTextures.WORK_DONE_V,           // atlasLocation
                CommonTextures.WORK_DONE_V_WIDTH,
                CommonTextures.WORK_DONE_V_HEIGHT,
                0, 0,
                leftPos, yPosRef[0],                  // 目标 X,Y
                CommonTextures.WORK_DONE_V_WIDTH,     // UWidth  = 整张贴图宽
                vHeight);                              // VHeight = 截掉剩余 (1‑progress)
    }

    public static void renderFurnaceWorkV(GuiGraphicsExtractor guiGraphics, int leftPos, int[] yPosRef)
    {
        renderWorkDoneV(guiGraphics, leftPos, yPosRef,
                CommonTextures.FURNACE_WORK_V_WIDTH,
                CommonTextures.FURNACE_WORK_V_HEIGHT);
    }

    public static void renderFurnaceWorkV(GuiGraphicsExtractor guiGraphics, int leftPos, int[] yPosRef,
                                          int width, int height)
    {
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, CommonTextures.FURNACE_WORK_V, leftPos, yPosRef[0], width, height);
        yPosRef[0] += height;
    }

    public static void renderFurnaceWorkV_AsProgress(GuiGraphicsExtractor guiGraphics, int leftPos, int[] yPosRef,
                                                     int width, int height, float progress) // 从下往上
    {
        // 0. 约束进度
        progress = Mth.clamp(progress, 0f, 1f);
        if (progress <= 0f) return;          // 0% 不绘制

        /* 2. 贴图需要的 V 高度 —— 按同样比例裁剪 */
        int vHeight = (int) (CommonTextures.FURNACE_WORK_V_HEIGHT * progress);
        int vOffset = (int) (CommonTextures.FURNACE_WORK_V_HEIGHT - vHeight);

        int drawY = yPosRef[0] + vOffset;

        /* 3. blit：
          ‑ 只画贴图的顶部 vHeight 像素；
          ‑ 目标区域从 (leftPos, yPos) 开始往下铺 drawH 像素 */
        guiGraphics.blitSprite(
                RenderPipelines.GUI_TEXTURED,
                CommonTextures.FURNACE_WORK_V,           // atlasLocation
                CommonTextures.FURNACE_WORK_V_WIDTH,
                CommonTextures.FURNACE_WORK_V_HEIGHT,
                0, vOffset,
                leftPos, drawY,                  // 目标 X,Y
                CommonTextures.FURNACE_WORK_V_WIDTH,     // UWidth  = 整张贴图宽
                vHeight);                              // VHeight = 截掉剩余 (1‑progress)
    }
}
