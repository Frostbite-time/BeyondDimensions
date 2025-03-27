package com.wintercogs.beyonddimensions.Render;


import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;


// 参考自jei的IIngredientRenderer接口以及FluidTankRenderer类
@SideOnly(Side.CLIENT)
public class IngredientRenderer {
    private static final int TEXTURE_SIZE = 16;
    private static final int MIN_FLUID_HEIGHT = 1;

    public static void drawTiledSprite(int tiledWidth, int tiledHeight, int color, long scaledAmount, TextureAtlasSprite sprite, int posX, int posY) {
        GlStateManager.enableBlend();
        GlStateManager.color(1, 1, 1, 1);

        Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        setGLColorFromInt(color);

        final int xTileCount = tiledWidth / TEXTURE_SIZE;
        final int xRemainder = tiledWidth - (xTileCount * TEXTURE_SIZE);
        final long yTileCount = scaledAmount / TEXTURE_SIZE;
        final long yRemainder = scaledAmount - (yTileCount * TEXTURE_SIZE);

        final int yStart = tiledHeight + posY;

        for (int xTile = 0; xTile <= xTileCount; xTile++) {
            for (int yTile = 0; yTile <= yTileCount; yTile++) {
                int width = (xTile == xTileCount) ? xRemainder : TEXTURE_SIZE;
                long height = (yTile == yTileCount) ? yRemainder : TEXTURE_SIZE;
                int x = posX + (xTile * TEXTURE_SIZE);
                int y = yStart - ((yTile + 1) * TEXTURE_SIZE);
                if (width > 0 && height > 0) {
                    long maskTop = TEXTURE_SIZE - height;
                    int maskRight = TEXTURE_SIZE - width;

                    drawTextureWithMasking(x, y, sprite, maskTop, maskRight);
                }
            }
        }

        GlStateManager.color(1, 1, 1, 1);
        GlStateManager.disableBlend();
    }

    private static void setGLColorFromInt(int color) {
        float red = ((color >> 16) & 255) / 256f;
        float green = ((color >> 8) & 255) / 256f;
        float blue = (color & 255) / 256f;
        GlStateManager.color(red, green, blue, 1.0F);
    }

    private static void drawTextureWithMasking(float xCoord, float yCoord, TextureAtlasSprite textureSprite, long maskTop, long maskRight) {
        float uMin = textureSprite.getMinU();
        float uMax = textureSprite.getMaxU();
        float vMin = textureSprite.getMinV();
        float vMax = textureSprite.getMaxV();

        uMax = uMax - (maskRight / 16F * (uMax - uMin));
        vMax = vMax - (maskTop / 16F * (vMax - vMin));

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        buffer.pos(xCoord, yCoord + 16, 100).tex(uMin, vMax).endVertex();
        buffer.pos(xCoord + 16 - maskRight, yCoord + 16, 100).tex(uMax, vMax).endVertex();
        buffer.pos(xCoord + 16 - maskRight, yCoord + maskTop, 100).tex(uMax, vMin).endVertex();
        buffer.pos(xCoord, yCoord + maskTop, 100).tex(uMin, vMin).endVertex();
        tessellator.draw();
    }
}
