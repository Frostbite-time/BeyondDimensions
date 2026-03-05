package com.wintercogs.beyonddimensions.integration.Botania.Block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import org.jetbrains.annotations.Nullable;
import vazkii.botania.client.core.helper.RenderHelper;
import vazkii.botania.common.helper.VecHelper;

import java.util.Objects;

import static vazkii.botania.api.BotaniaAPI.botaniaRL;

public class ManaPoolPathwayBlockEntityRender implements BlockEntityRenderer<ManaPoolPathwayBlockEntity>
{
    private final TextureAtlasSprite waterSprite;

    public ManaPoolPathwayBlockEntityRender(BlockEntityRendererProvider.Context ctx)
    {
        this.waterSprite = Objects.requireNonNull(
                Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                        .apply(botaniaRL("block/mana_water"))
        );
    }

    @Override
    public void render(@Nullable ManaPoolPathwayBlockEntity pool, float f, PoseStack ms, MultiBufferSource buffers, int light, int overlay)
    {

        boolean diluted = false;

        int insideUVStart = diluted ? 1 : 2;
        int insideUVEnd = 16 - insideUVStart;
        float poolBottom = insideUVStart / 16F + 0.001F;
        float poolTop = 7 / 16F;


        float manaLevel = 1f;
        ms.pushPose();
        ms.translate(0, Mth.clampedMap(manaLevel, 0, 1, poolBottom, poolTop), 0);
        ms.mulPose(VecHelper.rotateX(90F));

        VertexConsumer buffer = buffers.getBuffer(RenderHelper.MANA_POOL_WATER);
        RenderHelper.renderIconCropped(
                ms, buffer,
                insideUVStart, insideUVStart, insideUVEnd, insideUVEnd,
                this.waterSprite, 0xFFFFFF, 1, light);

        ms.popPose();

    }
}
