package com.wintercogs.beyonddimensions.integration.module.ae2lt.storage;

import com.mojang.blaze3d.systems.RenderSystem;
import com.wintercogs.beyonddimensions.api.storage.key.IStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.IStackRender;
import com.wintercogs.beyonddimensions.util.IngredientRenderer;
import com.wintercogs.beyonddimensions.util.StringFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.client.ClientTooltipFlag;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public final class LightningStackKeyRender implements IStackRender
{
    public static final LightningStackKeyRender INSTANCE = new LightningStackKeyRender();

    private LightningStackKeyRender()
    {
    }

    @Override
    public void render(GuiGraphics gui, IStackKey<?> key, int x, int y)
    {
        var texture = IClientFluidTypeExtensions.of(Fluids.WATER).getStillTexture();
        var sprite = texture == null ? null
                : Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(texture);
        if (sprite != null && sprite.atlasLocation() != MissingTextureAtlasSprite.getLocation())
        {
            int tint = key == LightningStackKey.EXTREME_HIGH_VOLTAGE ? 0xB050FF : 0x58DFFF;
            IngredientRenderer.drawTiledSprite(gui, 16, 16, tint, 16, sprite, x, y);
        }
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
        int width = Minecraft.getInstance().font.width(text);
        int renderX = (int) ((x - 1 + 18.0f - width * scale) / scale);
        int renderY = (int) ((y - 1 + 16.0f - 5.0f * scale) / scale);
        gui.drawString(Minecraft.getInstance().font, text, renderX, renderY, 0xFFFFFF);
        pose.popPose();
    }

    @Override
    public String getCountText(long count)
    {
        return count < 0 ? "" : StringFormat.formatCount(count);
    }

    @Override
    public Component getDisplayName(IStackKey<?> key)
    {
        return key instanceof LightningStackKey lightning
                ? lightning.getRenderStack().getName()
                : LightningStackKey.HIGH_VOLTAGE.getRenderStack().getName();
    }

    @Override
    public List<Component> getTooltipLines(IStackKey<?> key, long amount, Item.TooltipContext context,
                                           @Nullable net.minecraft.world.entity.player.Player player,
                                           TooltipFlag flag)
    {
        return List.of(getDisplayName(key),
                Component.translatable("istack.beyonddimensions.storage_num.long_type", amount));
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(IStackKey<?> key)
    {
        return Optional.empty();
    }

    @Override
    public void renderTooltip(GuiGraphics gui, Font font, IStackKey<?> key, long amount, int mouseX, int mouseY)
    {
        var minecraft = Minecraft.getInstance();
        var context = minecraft.level == null ? Item.TooltipContext.EMPTY : Item.TooltipContext.of(minecraft.level);
        gui.renderTooltip(minecraft.font,
                getTooltipLines(key, amount, context, minecraft.player,
                        ClientTooltipFlag.of(minecraft.options.advancedItemTooltips
                                ? TooltipFlag.Default.ADVANCED : TooltipFlag.Default.NORMAL)),
                getTooltipImage(key), ItemStack.EMPTY, mouseX, mouseY);
    }
}
