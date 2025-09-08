package com.wintercogs.beyonddimensions.Api.DataBase.Stack;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 什么都不做的keyRender
 */
public class EmptyStackKeyRender implements IStackRender
{
    public static final EmptyStackKeyRender INSTANCE = new EmptyStackKeyRender();

    private EmptyStackKeyRender() {}

    @Override
    public void render(GuiGraphics gui, IStackKey<?> key, int x, int y)
    {

    }

    @Override
    public void renderAmount(GuiGraphics gui, long amount, int x, int y)
    {

    }

    @Override
    public String getCountText(long count)
    {
        return "";
    }

    @Override
    public Component getDisplayName(IStackKey<?> key)
    {
        return Component.empty();
    }

    @Override
    public List<Component> getTooltipLines(IStackKey<?> key, long amount, Item.TooltipContext tooltipContext, @Nullable Player player, TooltipFlag tooltipFlag)
    {
        return new ArrayList<>();
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(IStackKey<?> key)
    {
        return Optional.empty();
    }

    @Override
    public void renderTooltip(GuiGraphics gui, Font font, IStackKey<?> key, long amount, int mouseX, int mouseY)
    {

    }
}
