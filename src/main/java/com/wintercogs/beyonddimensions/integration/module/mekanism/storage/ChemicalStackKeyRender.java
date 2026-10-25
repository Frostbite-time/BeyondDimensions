package com.wintercogs.beyonddimensions.integration.module.mekanism.storage;

import com.wintercogs.beyonddimensions.api.storage.key.IStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.IStackRender;
import com.wintercogs.beyonddimensions.util.IngredientRenderer;
import com.wintercogs.beyonddimensions.util.StringFormat;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 气体、灌注、颜料、浆液四种化学品共用的渲染器，资源的渲染对象是对应的 ChemicalStack
 */
public class ChemicalStackKeyRender implements IStackRender
{
    public static final ChemicalStackKeyRender INSTANCE = new ChemicalStackKeyRender();

    @Override
    public void render(GuiGraphics gui, IStackKey<?> key, int x, int y)
    {
        if (key.getRenderStack() instanceof ChemicalStack<?> stack)
        {
            var pose = gui.pose();
            pose.pushPose();

            Chemical<?> chem = stack.getType();
            if (!stack.isEmpty())
            {
                ResourceLocation icon = chem.getIcon();
                TextureAtlasSprite sprite = icon == null ? null :
                        Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(icon);
                if (sprite != null && sprite.atlasLocation() != MissingTextureAtlasSprite.getLocation())
                {
                    int tint = chem.getTint();
                    IngredientRenderer
                            .drawTiledSprite(gui, 16, 16, tint, 16, sprite, x, y);
                }
            }

            pose.popPose();
        }
    }

    @Override
    public String getCountText(long count)
    {
        return count < 0 ? "" : StringFormat.formatBucket(count);
    }

    @Override
    public Component getDisplayName(IStackKey<?> key)
    {
        if (key.getRenderStack() instanceof ChemicalStack<?> s)
        {
            return s.isEmpty() ? Component.empty() : s.getTextComponent();
        }
        return Component.empty();
    }

    @Override
    public List<Component> getTooltipLines(IStackKey<?> key, long amount,
                                           @Nullable net.minecraft.world.entity.player.Player player,
                                           TooltipFlag tooltipFlag)
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
