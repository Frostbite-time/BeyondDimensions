package com.wintercogs.beyonddimensions.Api.DataBase.Stack;

import com.wintercogs.beyonddimensions.Util.StringFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.ClientTooltipFlag;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FluidStackKeyRender implements IStackRender
{
    public static final FluidStackKeyRender INSTANCE = new FluidStackKeyRender();

    @Override
    public void render(GuiGraphics gui, IStackKey<?> key, int x, int y)
    {
        if (key instanceof FluidStackKey fluidKey)
        {
            FluidStack stack = fluidKey.getRenderStack();
            if (!stack.isEmpty())
            {
                int tint = IClientFluidTypeExtensions.of(stack.getFluid()).getTintColor(stack);
                int argb = tint | 0xFF000000;
                gui.fill(x, y, x + 16, y + 16, argb);
            }
        }

    }

    @Override
    public void renderAmount(GuiGraphics gui, long amount, int x, int y)
    {
        // 渲染数量文本（右下角）
        String text = getCountText(amount);
        if (text.isEmpty()) return;

        float scale = 0.666f;
        var pose = gui.pose();
        pose.pushMatrix();
        pose.scale(scale, scale);

        int w = Minecraft.getInstance().font.width(text);
        final int X = (int) ((x - 1 + 16.0f + 2.0f - w * 0.666f) / 0.666f);
        final int Y = (int) ((y - 1 + 16.0f - 5.0f * 0.666f) / 0.666f);

        gui.drawString(Minecraft.getInstance().font, text, X, Y, 0xFFFFFF);
        pose.popMatrix();
    }

    @Override
    public String getCountText(long count)
    {
        if (count < 0) return "";
        return StringFormat.formatBucket(count);
    }

    @Override
    public Component getDisplayName(IStackKey<?> key)
    {
        if (key instanceof FluidStackKey fluidKey)
        {
            FluidStack stack = fluidKey.getRenderStack();
            return stack.isEmpty() ? Component.empty() : stack.getHoverName();
        }
        return Component.empty();
    }

    @Override
    public List<Component> getTooltipLines(IStackKey<?> key, long amount, Item.TooltipContext tooltipContext,
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
        // 流体默认无额外 TooltipComponent
        return Optional.empty();
    }

    @Override
    public void renderTooltip(GuiGraphics gui, Font font, IStackKey<?> key, long amount, int mouseX, int mouseY)
    {
        var mc = Minecraft.getInstance();
        var ctx = mc.level != null ? Item.TooltipContext.of(mc.level) : Item.TooltipContext.EMPTY;
        var tooltips = getTooltipLines(key, amount, ctx, mc.player, ClientTooltipFlag.of(mc.options.advancedItemTooltips
                ? TooltipFlag.Default.ADVANCED : TooltipFlag.Default.NORMAL));
        var visualTooltipComponent = getTooltipImage(key);

        List<ClientTooltipComponent> clientTooltips =
                ClientHooks.gatherTooltipComponents(
                        ItemStack.EMPTY, tooltips, visualTooltipComponent, mouseX, gui.guiWidth(), gui.guiHeight(), font);

        gui.renderTooltip(
                mc.font,
                clientTooltips,
                mouseX, mouseY,
                DefaultTooltipPositioner.INSTANCE,
                null
        );
    }
}
