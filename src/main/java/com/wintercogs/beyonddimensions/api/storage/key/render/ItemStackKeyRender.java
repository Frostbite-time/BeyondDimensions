package com.wintercogs.beyonddimensions.api.storage.key.render;

import com.wintercogs.beyonddimensions.api.storage.key.IStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.IStackRender;
import com.wintercogs.beyonddimensions.api.storage.key.impl.ItemStackKey;
import com.wintercogs.beyonddimensions.util.StringFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.ClientTooltipFlag;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ItemStackKeyRender implements IStackRender
{
    public static final ItemStackKeyRender INSTANCE = new ItemStackKeyRender();

    @Override
    public void render(GuiGraphicsExtractor gui, IStackKey<?> key, int x, int y)
    {
        if (key instanceof ItemStackKey itemKey)
        {
            ItemStack renderStack = itemKey.getRenderStack();
            gui.fakeItem(renderStack, x, y);
            gui.itemDecorations(Minecraft.getInstance().font, renderStack, x, y, "");
        }
    }

    @Override
    public void renderAmount(GuiGraphicsExtractor gui, long amount, int x, int y)
    {
        // 渲染数量文本
        String countText = getCountText(amount);
        if (countText.isEmpty()) return;

        float scale = 0.666f;
        var pose = gui.pose();
        pose.pushMatrix();
        pose.scale(scale, scale);

        int w = Minecraft.getInstance().font.width(countText);
        final int X = (int) ((x - 1 + 16.0f + 2.0f - w * 0.666f) / 0.666f);
        final int Y = (int) ((y - 1 + 16.0f - 5.0f * 0.666f) / 0.666f);
        gui.text(Minecraft.getInstance().font, countText, X, Y, 0xFFFFFFFF);
        pose.popMatrix();
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
        if (key instanceof ItemStackKey itemKey)
        {
            ItemStack renderStack = itemKey.getRenderStack();
            return renderStack.getDisplayName();
        }
        return Component.empty();
    }

    @Override
    public List<Component> getTooltipLines(IStackKey<?> key, long amount, Item.TooltipContext tooltipContext, @Nullable Player player, TooltipFlag tooltipFlag)
    {
        if (key instanceof ItemStackKey itemKey)
        {
            ItemStack renderStack = itemKey.getRenderStack();
            List<Component> tooltips = renderStack.getTooltipLines(tooltipContext, player, tooltipFlag);
            tooltips.add(Component.translatable("istack.beyonddimensions.storage_num.item", amount));
            return tooltips;
        }
        return new ArrayList<>();
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(IStackKey<?> key)
    {
        if (key instanceof ItemStackKey itemKey)
        {
            ItemStack renderStack = itemKey.getRenderStack();
            return renderStack.getTooltipImage();
        }
        return Optional.empty();
    }

    @Override
    public void renderTooltip(GuiGraphicsExtractor gui, Font font, IStackKey<?> key, long amount, int mouseX, int mouseY)
    {
        var mc = Minecraft.getInstance();
        var ctx = mc.level != null ? Item.TooltipContext.of(mc.level) : Item.TooltipContext.EMPTY;
        var tooltips = getTooltipLines(key, amount, ctx, mc.player, ClientTooltipFlag.of(mc.options.advancedItemTooltips
                ? TooltipFlag.Default.ADVANCED : TooltipFlag.Default.NORMAL));
        var visualTooltipComponent = getTooltipImage(key);

        List<ClientTooltipComponent> clientTooltips =
                ClientHooks.gatherTooltipComponents(
                        ItemStack.EMPTY, tooltips, visualTooltipComponent, mouseX, gui.guiWidth(), gui.guiHeight(), font);

        gui.tooltip(
                mc.font,
                clientTooltips,
                mouseX, mouseY,
                DefaultTooltipPositioner.INSTANCE,
                null
        );
    }
}
