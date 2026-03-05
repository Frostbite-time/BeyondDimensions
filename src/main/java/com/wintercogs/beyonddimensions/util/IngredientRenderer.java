package com.wintercogs.beyonddimensions.util;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

// 参考自jei的IIngredientRenderer接口以及FluidTankRenderer类
public class IngredientRenderer
{
    private static final int TEXTURE_SIZE = 16;

    public static void drawTiledSprite(GuiGraphics guiGraphics, final int tiledWidth, final int tiledHeight, int color, long scaledAmount, TextureAtlasSprite sprite, int posX, int posY)
    {
        final int xTileCount = tiledWidth / TEXTURE_SIZE;
        final int xRemainder = tiledWidth - (xTileCount * TEXTURE_SIZE);
        final long yTileCount = scaledAmount / TEXTURE_SIZE;
        final long yRemainder = scaledAmount - (yTileCount * TEXTURE_SIZE);

        final int yStart = tiledHeight + posY;

        for (int xTile = 0; xTile <= xTileCount; xTile++)
        {
            for (int yTile = 0; yTile <= yTileCount; yTile++)
            {
                int width = (xTile == xTileCount) ? xRemainder : TEXTURE_SIZE;
                long height = (yTile == yTileCount) ? yRemainder : TEXTURE_SIZE;
                int x = posX + (xTile * TEXTURE_SIZE);
                int y = yStart - ((yTile + 1) * TEXTURE_SIZE);
                if (width > 0 && height > 0)
                {
                    guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, width, (int) height, color);
                }
            }
        }
    }
}
