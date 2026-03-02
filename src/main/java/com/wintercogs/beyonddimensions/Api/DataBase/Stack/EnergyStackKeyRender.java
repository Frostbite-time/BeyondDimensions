package com.wintercogs.beyonddimensions.Api.DataBase.Stack;

import com.mojang.blaze3d.systems.RenderSystem;
import com.wintercogs.beyonddimensions.Util.StringFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.client.ClientTooltipFlag;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class EnergyStackKeyRender implements IStackRender
{
    public static final EnergyStackKeyRender INSTANCE = new EnergyStackKeyRender();

    private EnergyStackKeyRender()
    {
    }

    @Override
    public void render(GuiGraphics gui, IStackKey<?> key, int x, int y)
    {
        var pose = gui.pose();
        pose.pushPose();

        // 占位图标：用水的静态贴图 + 绿色
        Identifier still = net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions
                .of(net.minecraft.world.level.material.Fluids.WATER)
                .getStillTexture();
        TextureAtlasSprite sprite = still == null ? null
                : Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(still);

        if (sprite != null && sprite.atlasLocation() != MissingTextureAtlasSprite.getLocation())
        {
            int tint = 0x50F18E; // 能量绿色
            com.wintercogs.beyonddimensions.Render.IngredientRenderer
                    .drawTiledSprite(gui, 16, 16, tint, 16, sprite, x, y);
        }

        pose.popPose();
    }

    @Override
    public void renderAmount(GuiGraphics gui, long amount, int x, int y)
    {
        String text = getCountText(amount);
        if (text.isEmpty()) return;

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
    public String getCountText(long count)
    {
        if (count < 0) return "";
        return StringFormat.formatCount(count);
    }

    @Override
    public Component getDisplayName(IStackKey<?> key)
    {
        // 使用最小非空渲染栈的名称
        return EnergyStackKey.INSTANCE.getRenderStack().getName();
    }

    @Override
    public List<Component> getTooltipLines(IStackKey<?> key, long amount, Item.TooltipContext tooltipContext,
                                           @Nullable net.minecraft.world.entity.player.Player player,
                                           TooltipFlag tooltipFlag)
    {
        return List.of(
                getDisplayName(key),
                Component.translatable("istack.beyonddimensions.storage_num.long_type", amount)
        );
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(IStackKey<?> key)
    {
        return Optional.empty();
    }

    @Override
    public void renderTooltip(GuiGraphics gui, Font font, IStackKey<?> key, long amount, int mouseX, int mouseY)
    {
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