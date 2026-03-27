package com.wintercogs.beyonddimensions.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

public class CommonTexturesRender
{
    // TOP_BASE_COMMON 渲染方法
    public static void renderTopBaseCommon(GuiGraphics guiGraphics, int leftPos, int[] yPosRef)
    {
        renderTopBaseCommon(guiGraphics, leftPos, yPosRef,
                CommonTextures.TOP_BASE_COMMON_WIDTH,
                CommonTextures.TOP_BASE_COMMON_HEIGHT);
    }

    public static void renderTopBaseCommon(GuiGraphics guiGraphics, int leftPos, int[] yPosRef,
                                           int width, int height)
    {
        guiGraphics.blit(CommonTextures.TOP_BASE_COMMON, leftPos, yPosRef[0],
                width, height,
                0, 0,
                CommonTextures.TOP_BASE_COMMON_WIDTH,
                CommonTextures.TOP_BASE_COMMON_HEIGHT,
                CommonTextures.TOP_BASE_COMMON_WIDTH,
                CommonTextures.TOP_BASE_COMMON_HEIGHT);
        yPosRef[0] += height;
    }

    // COMMON_CONNECTION 渲染方法
    public static void renderCommonConnection(GuiGraphics guiGraphics, int leftPos, int[] yPosRef)
    {
        renderCommonConnection(guiGraphics, leftPos, yPosRef,
                CommonTextures.COMMON_CONNECTION_WIDTH,
                CommonTextures.COMMON_CONNECTION_HEIGHT);
    }

    public static void renderCommonConnection(GuiGraphics guiGraphics, int leftPos, int[] yPosRef,
                                              int width, int height)
    {
        guiGraphics.blit(CommonTextures.COMMON_CONNECTION, leftPos, yPosRef[0],
                width, height,
                0, 0,
                CommonTextures.COMMON_CONNECTION_WIDTH,
                CommonTextures.COMMON_CONNECTION_HEIGHT,
                CommonTextures.COMMON_CONNECTION_WIDTH,
                CommonTextures.COMMON_CONNECTION_HEIGHT);
        yPosRef[0] += height;
    }

    public static void renderBottomBaseCommon(GuiGraphics guiGraphics, int leftPos, int[] yPosRef)
    {
        renderBottomBaseCommon(guiGraphics, leftPos, yPosRef,
                CommonTextures.BOTTOM_BASE_COMMON_WIDTH,
                CommonTextures.BOTTOM_BASE_COMMON_HEIGHT);
    }

    public static void renderBottomBaseCommon(GuiGraphics guiGraphics, int leftPos, int[] yPosRef,
                                              int width, int height)
    {
        guiGraphics.blit(CommonTextures.BOTTOM_BASE_COMMON, leftPos, yPosRef[0],
                width, height,
                0, 0,
                CommonTextures.BOTTOM_BASE_COMMON_WIDTH,
                CommonTextures.BOTTOM_BASE_COMMON_HEIGHT,
                CommonTextures.BOTTOM_BASE_COMMON_WIDTH,
                CommonTextures.BOTTOM_BASE_COMMON_HEIGHT);
        yPosRef[0] += height;
    }

    // COMMON_SLOTS 渲染方法
    public static void renderCommonSlots(GuiGraphics guiGraphics, int leftPos, int[] yPosRef)
    {
        renderCommonSlots(guiGraphics, leftPos, yPosRef,
                CommonTextures.COMMON_SLOTS_WIDTH,
                CommonTextures.COMMON_SLOTS_HEIGHT);
    }

    public static void renderCommonSlots(GuiGraphics guiGraphics, int leftPos, int[] yPosRef,
                                         int width, int height)
    {
        guiGraphics.blit(CommonTextures.COMMON_SLOTS, leftPos, yPosRef[0],
                width, height,
                0, 0,
                CommonTextures.COMMON_SLOTS_WIDTH,
                CommonTextures.COMMON_SLOTS_HEIGHT,
                CommonTextures.COMMON_SLOTS_WIDTH,
                CommonTextures.COMMON_SLOTS_HEIGHT);
        yPosRef[0] += height;
    }

    // FILTER_SLOTS 渲染方法
    public static void renderFilterSlots(GuiGraphics guiGraphics, int leftPos, int[] yPosRef)
    {
        renderFilterSlots(guiGraphics, leftPos, yPosRef,
                CommonTextures.FILTER_SLOTS_WIDTH,
                CommonTextures.FILTER_SLOTS_HEIGHT);
    }

    public static void renderFilterSlots(GuiGraphics guiGraphics, int leftPos, int[] yPosRef,
                                         int width, int height)
    {
        guiGraphics.blit(CommonTextures.FILTER_SLOTS, leftPos, yPosRef[0],
                width, height,
                0, 0,
                CommonTextures.FILTER_SLOTS_WIDTH,
                CommonTextures.FILTER_SLOTS_HEIGHT,
                CommonTextures.FILTER_SLOTS_WIDTH,
                CommonTextures.FILTER_SLOTS_HEIGHT);
        yPosRef[0] += height;
    }

    // PLAYER_INV 渲染方法
    public static void renderPlayerInv(GuiGraphics guiGraphics, int leftPos, int[] yPosRef)
    {
        renderPlayerInv(guiGraphics, leftPos, yPosRef,
                CommonTextures.PLAYER_INV_WIDTH,
                CommonTextures.PLAYER_INV_HEIGHT);
    }

    public static void renderPlayerInv(GuiGraphics guiGraphics, int leftPos, int[] yPosRef,
                                       int width, int height)
    {
        guiGraphics.blit(CommonTextures.GUI_TEXTURE_PLAYER_INV, leftPos, yPosRef[0],
                width, height,
                0, 0,
                CommonTextures.PLAYER_INV_WIDTH,
                CommonTextures.PLAYER_INV_HEIGHT,
                CommonTextures.PLAYER_INV_WIDTH,
                CommonTextures.PLAYER_INV_HEIGHT);
        yPosRef[0] += height;
    }

    // 右标签渲染
    public static void renderRightTab(GuiGraphics guiGraphics, int leftPos, int[] yPosRef)
    {
        renderRightTab(guiGraphics, leftPos, yPosRef,
                CommonTextures.RIGHT_TAB_WIDTH,
                CommonTextures.RIGHT_TAB_HEIGHT);
    }

    public static void renderRightTab(GuiGraphics guiGraphics, int leftPos, int[] yPosRef,
                                      int width, int height)
    {
        guiGraphics.blit(CommonTextures.RIGHT_TAB, leftPos, yPosRef[0],
                width, height,
                0, 0,
                CommonTextures.RIGHT_TAB_WIDTH,
                CommonTextures.RIGHT_TAB_HEIGHT,
                CommonTextures.RIGHT_TAB_WIDTH,
                CommonTextures.RIGHT_TAB_HEIGHT);
        yPosRef[0] += height;
    }


    public static void renderNetFurnaceBackground(GuiGraphics guiGraphics, int leftPos, int[] yPosRef)
    {
        renderNetFurnaceBackground(guiGraphics, leftPos, yPosRef,
                CommonTextures.NET_FURNACE_BACKGROUND_WIDTH,
                CommonTextures.NET_FURNACE_BACKGROUND_HEIGHT);
    }

    public static void renderNetFurnaceBackground(GuiGraphics guiGraphics, int leftPos, int[] yPosRef,
                                                  int width, int height)
    {
        guiGraphics.blit(CommonTextures.NET_FURNACE_BACKGROUND, leftPos, yPosRef[0],
                width, height,
                0, 0,
                CommonTextures.NET_FURNACE_BACKGROUND_WIDTH,
                CommonTextures.NET_FURNACE_BACKGROUND_HEIGHT,
                CommonTextures.NET_FURNACE_BACKGROUND_WIDTH,
                CommonTextures.NET_FURNACE_BACKGROUND_HEIGHT);
        yPosRef[0] += height;
    }

    public static void renderWorkDoneV(GuiGraphics guiGraphics, int leftPos, int[] yPosRef)
    {
        renderWorkDoneV(guiGraphics, leftPos, yPosRef,
                CommonTextures.WORK_DONE_V_WIDTH,
                CommonTextures.WORK_DONE_V_HEIGHT);
    }

    public static void renderWorkDoneV(GuiGraphics guiGraphics, int leftPos, int[] yPosRef,
                                       int width, int height)
    {
        guiGraphics.blit(CommonTextures.WORK_DONE_V, leftPos, yPosRef[0],
                width, height,
                0, 0,
                CommonTextures.WORK_DONE_V_WIDTH,
                CommonTextures.WORK_DONE_V_HEIGHT,
                CommonTextures.WORK_DONE_V_WIDTH,
                CommonTextures.WORK_DONE_V_HEIGHT);
        yPosRef[0] += height;
    }

    public static void renderWorkDoneV_AsProgress(GuiGraphics guiGraphics, int leftPos, int[] yPosRef,
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
        guiGraphics.blit(
                CommonTextures.WORK_DONE_V,           // atlasLocation
                leftPos, yPosRef[0],                  // 目标 X,Y
                width, (int) (height * progress),
                0, 0,
                CommonTextures.WORK_DONE_V_WIDTH, vHeight,
                CommonTextures.WORK_DONE_V_WIDTH,
                CommonTextures.WORK_DONE_V_HEIGHT);                              // VHeight = 截掉剩余 (1‑progress)
    }

    public static void renderFurnaceWorkV(GuiGraphics guiGraphics, int leftPos, int[] yPosRef)
    {
        renderWorkDoneV(guiGraphics, leftPos, yPosRef,
                CommonTextures.FURNACE_WORK_V_WIDTH,
                CommonTextures.FURNACE_WORK_V_HEIGHT);
    }

    public static void renderFurnaceWorkV(GuiGraphics guiGraphics, int leftPos, int[] yPosRef,
                                          int width, int height)
    {
        guiGraphics.blit(CommonTextures.FURNACE_WORK_V, leftPos, yPosRef[0],
                width, height,
                0, 0,
                CommonTextures.FURNACE_WORK_V_WIDTH,
                CommonTextures.FURNACE_WORK_V_HEIGHT,
                CommonTextures.FURNACE_WORK_V_WIDTH,
                CommonTextures.FURNACE_WORK_V_HEIGHT);
        yPosRef[0] += height;
    }

    public static void renderFurnaceWorkV_AsProgress(GuiGraphics guiGraphics, int leftPos, int[] yPosRef,
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
        guiGraphics.blit(
                CommonTextures.FURNACE_WORK_V,           // atlasLocation
                leftPos, drawY,                  // 目标 X,Y
                width, (int) (height * progress),
                0, vOffset,
                CommonTextures.FURNACE_WORK_V_WIDTH, vHeight,
                CommonTextures.FURNACE_WORK_V_WIDTH,
                CommonTextures.FURNACE_WORK_V_HEIGHT);                              // VHeight = 截掉剩余 (1‑progress)
    }
}
