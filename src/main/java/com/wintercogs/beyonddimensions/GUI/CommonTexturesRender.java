package com.wintercogs.beyonddimensions.GUI;

import net.minecraft.client.gui.GuiGraphics;

public class CommonTexturesRender
{
    // TOP_BASE_COMMON 渲染方法
    public static void renderTopBaseCommon(GuiGraphics guiGraphics, int leftPos, int[] yPosRef) {
        renderTopBaseCommon(guiGraphics, leftPos, yPosRef,
                CommonTextures.TOP_BASE_COMMON_WIDTH,
                CommonTextures.TOP_BASE_COMMON_HEIGHT);
    }

    public static void renderTopBaseCommon(GuiGraphics guiGraphics, int leftPos, int[] yPosRef,
                                           int width, int height) {
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
    public static void renderCommonConnection(GuiGraphics guiGraphics, int leftPos, int[] yPosRef) {
        renderCommonConnection(guiGraphics, leftPos, yPosRef,
                CommonTextures.COMMON_CONNECTION_WIDTH,
                CommonTextures.COMMON_CONNECTION_HEIGHT);
    }

    public static void renderCommonConnection(GuiGraphics guiGraphics, int leftPos, int[] yPosRef,
                                              int width, int height) {
        guiGraphics.blit(CommonTextures.COMMON_CONNECTION, leftPos, yPosRef[0],
                width, height,
                0, 0,
                CommonTextures.COMMON_CONNECTION_WIDTH,
                CommonTextures.COMMON_CONNECTION_HEIGHT,
                CommonTextures.COMMON_CONNECTION_WIDTH,
                CommonTextures.COMMON_CONNECTION_HEIGHT);
        yPosRef[0] += height;
    }
    // COMMON_SLOTS 渲染方法
    public static void renderCommonSlots(GuiGraphics guiGraphics, int leftPos, int[] yPosRef) {
        renderCommonSlots(guiGraphics, leftPos, yPosRef,
                CommonTextures.COMMON_SLOTS_WIDTH,
                CommonTextures.COMMON_SLOTS_HEIGHT);
    }

    public static void renderCommonSlots(GuiGraphics guiGraphics, int leftPos, int[] yPosRef,
                                         int width, int height) {
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
    public static void renderFilterSlots(GuiGraphics guiGraphics, int leftPos, int[] yPosRef) {
        renderFilterSlots(guiGraphics, leftPos, yPosRef,
                CommonTextures.FILTER_SLOTS_WIDTH,
                CommonTextures.FILTER_SLOTS_HEIGHT);
    }

    public static void renderFilterSlots(GuiGraphics guiGraphics, int leftPos, int[] yPosRef,
                                         int width, int height) {
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
    public static void renderPlayerInv(GuiGraphics guiGraphics, int leftPos, int[] yPosRef) {
        renderPlayerInv(guiGraphics, leftPos, yPosRef,
                CommonTextures.PLAYER_INV_WIDTH,
                CommonTextures.PLAYER_INV_HEIGHT);
    }

    public static void renderPlayerInv(GuiGraphics guiGraphics, int leftPos, int[] yPosRef,
                                       int width, int height) {
        guiGraphics.blit(CommonTextures.GUI_TEXTURE_PLAYER_INV, leftPos, yPosRef[0],
                width, height,
                0, 0,
                CommonTextures.PLAYER_INV_WIDTH,
                CommonTextures.PLAYER_INV_HEIGHT,
                CommonTextures.PLAYER_INV_WIDTH,
                CommonTextures.PLAYER_INV_HEIGHT);
        yPosRef[0] += height;
    }
}
