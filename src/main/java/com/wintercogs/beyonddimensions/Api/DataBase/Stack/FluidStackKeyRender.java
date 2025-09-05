package com.wintercogs.beyonddimensions.Api.DataBase.Stack;

import com.mojang.blaze3d.systems.RenderSystem;
import com.wintercogs.beyonddimensions.Unit.StringFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.client.ClientTooltipFlag;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FluidStackKeyRender implements IStackRender<FluidStackKey> {
    public static final FluidStackKeyRender INSTANCE = new FluidStackKeyRender();

    @Override
    public void render(GuiGraphics gui, FluidStackKey key, int x, int y) {
        // 渲染流体图标（16×16）
        var pose = gui.pose();
        pose.pushPose();

        FluidStack stack = key.getRenderStack();
        if (!stack.isEmpty()) {
            var fluid = stack.getFluid();
            IClientFluidTypeExtensions props = IClientFluidTypeExtensions.of(fluid);
            ResourceLocation still = props.getStillTexture(stack);
            TextureAtlasSprite sprite = still == null ? null :
                    Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(still);

            if (sprite != null && sprite.atlasLocation() != MissingTextureAtlasSprite.getLocation()) {
                int tint = IClientFluidTypeExtensions.of(fluid).getTintColor();
                // 复用项目现有的绘制工具
                com.wintercogs.beyonddimensions.Render.IngredientRenderer
                        .drawTiledSprite(gui, 16, 16, tint, 16, sprite, x, y);
            }
        }

        pose.popPose();
    }

    @Override
    public void renderAmount(GuiGraphics gui, long amount, int x, int y) {
        // 渲染数量文本（右下角）
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

        if (amount >= 0) {
            gui.drawString(Minecraft.getInstance().font, text, X, Y, 0xFFFFFF);
        }
        pose.popPose();
    }

    @Override
    public String getCountText(long count) {
        if (count < 0) return "";
        return StringFormat.formatBucket(count);
    }

    @Override
    public Component getDisplayName(FluidStackKey key) {
        FluidStack stack = key.getRenderStack();
        return stack.isEmpty() ? Component.empty() : stack.getHoverName();
    }

    @Override
    public List<Component> getTooltipLines(FluidStackKey key, long amount, Item.TooltipContext tooltipContext,
                                           @Nullable net.minecraft.world.entity.player.Player player,
                                           TooltipFlag tooltipFlag) {
        List<Component> lines = new ArrayList<>();
        lines.add(getDisplayName(key));
        lines.add(Component.translatable("istack.beyonddimensions.storage_num.fluid", amount));
        return lines;
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(FluidStackKey key) {
        // 流体默认无额外 TooltipComponent
        return Optional.empty();
    }

    @Override
    public void renderTooltip(GuiGraphics gui, Font font, FluidStackKey key, long amount, int mouseX, int mouseY) {
        var mc = Minecraft.getInstance();
        var ctx = mc.level != null ? Item.TooltipContext.of(mc.level) : Item.TooltipContext.EMPTY;
        gui.renderTooltip(
                mc.font,
                getTooltipLines(key, amount, ctx, mc.player,
                        ClientTooltipFlag.of(mc.options.advancedItemTooltips ? TooltipFlag.Default.ADVANCED : TooltipFlag.Default.NORMAL)),
                getTooltipImage(key),
                ItemStack.EMPTY,
                mouseX, mouseY
        );
    }
}