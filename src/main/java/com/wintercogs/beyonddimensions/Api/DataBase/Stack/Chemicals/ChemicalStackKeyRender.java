package com.wintercogs.beyonddimensions.Api.DataBase.Stack.Chemicals;

import com.mojang.blaze3d.systems.RenderSystem;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.IStackKey;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.IStackRender;
import com.wintercogs.beyonddimensions.Render.IngredientRenderer;
import com.wintercogs.beyonddimensions.Util.StringFormat;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 统一化学品渲染器（Gas/Infuse/Slurry/Pigment 等）
 * 适配旧版 IStackRender 接口：render(key,x,y) + renderTooltip(...)
 * <p>
 * 要求：key.getRenderStack() 返回的对象应当是 ChemicalStack。
 */
public class ChemicalStackKeyRender implements IStackRender
{
    public static final ChemicalStackKeyRender INSTANCE = new ChemicalStackKeyRender();

    @OnlyIn(Dist.CLIENT)
    @Override
    public void render(GuiGraphics gui, IStackKey<?> key, int x, int y)
    {
        if (!(key instanceof IStackKey<?>)) return;

        Object obj = key.getRenderStack();
        if (!(obj instanceof ChemicalStack<?> stack)) return;

        var pose = gui.pose();
        pose.pushPose();

        Chemical<?> chem = stack.getType();
        if (!stack.isEmpty())
        {
            ResourceLocation icon = chem.getIcon();
            TextureAtlasSprite sprite = Minecraft.getInstance()
                    .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                    .apply(icon);

            if (sprite != null && sprite.atlasLocation() != MissingTextureAtlasSprite.getLocation())
            {
                int tint = chem.getTint();
                IngredientRenderer.drawTiledSprite(gui, 16, 16, tint, 16, sprite, x, y);
            }
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
        pose.translate(0, 0, 200); // 顶层
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
        if (count <= 0) return "";
        return StringFormat.formatBucket(count);
    }

    @Override
    public Component getDisplayName(IStackKey<?> key)
    {
        Object obj = key.getRenderStack();
        if (obj instanceof ChemicalStack<?> stack)
        {
            return stack.isEmpty() ? Component.empty() : stack.getTextComponent();
        }
        return Component.empty();
    }

    @Override
    public List<Component> getTooltipLines(IStackKey<?> key, long amount,
                                           @Nullable Player player, TooltipFlag tooltipFlag)
    {
        List<Component> lines = new ArrayList<>();
        lines.add(getDisplayName(key));
        lines.add(Component.translatable("istack.beyonddimensions.storage_num.fluid", amount));
        return lines;
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(IStackKey<?> key)
    {
        return Optional.empty();
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void renderTooltip(GuiGraphics gui, Font font, IStackKey<?> key, long amount, int mouseX, int mouseY)
    {
        var mc = Minecraft.getInstance();
        gui.renderTooltip(
                mc.font,
                getTooltipLines(key, amount, mc.player,
                        mc.options.advancedItemTooltips ? TooltipFlag.Default.ADVANCED : TooltipFlag.Default.NORMAL),
                getTooltipImage(key),
                ItemStack.EMPTY,
                mouseX, mouseY
        );
    }
}