package com.wintercogs.beyonddimensions.integration.module.botania.storage;

import com.wintercogs.beyonddimensions.api.storage.key.IStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.IStackRender;
import com.wintercogs.beyonddimensions.util.IngredientRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class ManaStackKeyRender implements IStackRender
{
    public static final ManaStackKeyRender INSTANCE = new ManaStackKeyRender();

    private ManaStackKeyRender()
    {
    }

    @Override
    public void render(GuiGraphics gui, IStackKey<?> key, int x, int y)
    {
        var pose = gui.pose();
        pose.pushPose();

        // 使用工程里的 BOTANIA_MANA 图集贴图
        int tintColor = 0xFFFFFFFF;
        TextureAtlasSprite sprite = IngredientRenderer.BOTANIA_MANA.sprite();
        IngredientRenderer.drawTiledSprite(gui, 16, 16, tintColor, 16, sprite, x, y);

        pose.popPose();
    }

    @Override
    public Component getDisplayName(IStackKey<?> key)
    {
        return ManaStackKey.INSTANCE.getRenderStack().getName();
    }

    @Override
    public List<Component> getTooltipLines(IStackKey<?> key, long amount,
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
        gui.renderTooltip(
                mc.font,
                getTooltipLines(key, amount, mc.player,
                        mc.options.advancedItemTooltips
                                ? TooltipFlag.Default.ADVANCED : TooltipFlag.Default.NORMAL),
                getTooltipImage(key),
                ItemStack.EMPTY,
                mouseX, mouseY
        );
    }
}