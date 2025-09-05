package com.wintercogs.beyonddimensions.Api.DataBase.Stack;

import com.mojang.blaze3d.systems.RenderSystem;
import com.wintercogs.beyonddimensions.Render.IngredientRenderer;
import com.wintercogs.beyonddimensions.Unit.StringFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.client.ClientTooltipFlag;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class SourceStackKeyRender implements IStackRender<SourceStackKey> {
    public static final SourceStackKeyRender INSTANCE = new SourceStackKeyRender();

    private SourceStackKeyRender() {}

    @Override
    public void render(GuiGraphics gui, SourceStackKey key, int x, int y) {
        var pose = gui.pose();
        pose.pushPose();

        int tintColor = 0xFFFFFFFF;
        TextureAtlasSprite sprite = IngredientRenderer.ARS_SOURCE.sprite();
        IngredientRenderer.drawTiledSprite(gui, 16, 16, tintColor, 16, sprite, x, y);

        pose.popPose();
    }

    @Override
    public void renderAmount(GuiGraphics gui, long amount, int x, int y) {
        if (amount <= 0) return;
        String text = getCountText(amount);

        float scale = 0.666f;
        var pose = gui.pose();
        pose.pushPose();
        pose.translate(0, 0, 200);
        pose.scale(scale, scale, scale);
        RenderSystem.disableBlend();

        int w = Minecraft.getInstance().font.width(text);
        final int X = (int) ((x - 1 + 16.0f + 2.0f - w * 0.666f) / 0.666f);
        final int Y = (int) ((y - 1 + 16.0f - 5.0f * 0.666f) / 0.666f);
        gui.drawString(Minecraft.getInstance().font, text, X, Y, 0xFFFFFF);

        pose.popPose();
    }

    @Override
    public String getCountText(long count) {
        if (count <= 0) return "";
        return StringFormat.formatCount(count);
    }

    @Override
    public Component getDisplayName(SourceStackKey key) {
        return key.getRenderStack().getName();
    }

    @Override
    public List<Component> getTooltipLines(SourceStackKey key, long amount, Item.TooltipContext tooltipContext,
                                           @Nullable net.minecraft.world.entity.player.Player player,
                                           TooltipFlag tooltipFlag) {
        return List.of(
                getDisplayName(key),
                Component.translatable("istack.beyonddimensions.storage_num.long_type", amount)
        );
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(SourceStackKey key) {
        return Optional.empty();
    }

    @Override
    public void renderTooltip(GuiGraphics gui, Font font, SourceStackKey key, long amount, int mouseX, int mouseY) {
        var mc = Minecraft.getInstance();
        var ctx = mc.level != null ? Item.TooltipContext.of(mc.level) : Item.TooltipContext.EMPTY;
        gui.renderTooltip(
                mc.font,
                getTooltipLines(key, amount, ctx, mc.player,
                        ClientTooltipFlag.of(mc.options.advancedItemTooltips
                                ? TooltipFlag.Default.ADVANCED : TooltipFlag.Default.NORMAL)),
                getTooltipImage(key),
                ItemStack.EMPTY,
                mouseX, mouseY
        );
    }
}