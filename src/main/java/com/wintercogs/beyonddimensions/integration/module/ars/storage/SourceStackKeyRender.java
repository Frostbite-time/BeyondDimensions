package com.wintercogs.beyonddimensions.integration.module.ars.storage;

import com.wintercogs.beyonddimensions.api.storage.key.IStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.IStackRender;
import com.wintercogs.beyonddimensions.util.IngredientRenderer;
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

public class SourceStackKeyRender implements IStackRender
{
    public static final SourceStackKeyRender INSTANCE = new SourceStackKeyRender();

    private SourceStackKeyRender()
    {
    }

    @Override
    public void render(GuiGraphics gui, IStackKey<?> key, int x, int y)
    {
        var pose = gui.pose();
        pose.pushPose();

        int tintColor = 0xFFFFFFFF;
        TextureAtlasSprite sprite = IngredientRenderer.ARS_SOURCE.sprite();
        IngredientRenderer.drawTiledSprite(gui, 16, 16, tintColor, 16, sprite, x, y);

        pose.popPose();
    }

    @Override
    public Component getDisplayName(IStackKey<?> key)
    {
        return SourceStackKey.INSTANCE.getRenderStack().getName();
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