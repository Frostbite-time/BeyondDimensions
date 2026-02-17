package com.wintercogs.beyonddimensions.Api.DataBase.Stack;

import com.mojang.blaze3d.systems.RenderSystem;
import com.wintercogs.beyonddimensions.Unit.StringFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ItemStackKeyRender implements IStackRender
{
    public static final ItemStackKeyRender INSTANCE = new ItemStackKeyRender();

    @Override
    public void render(GuiGraphics gui, IStackKey<?> key, int x, int y)
    {
        if (key instanceof ItemStackKey itemKey)
        {
            // 渲染物品图标
            var poseStack = gui.pose(); // 获取渲染的变换矩阵
            poseStack.pushPose(); // 保存矩阵状态
            ItemStack renderStack = itemKey.getRenderStack();
            gui.renderFakeItem(renderStack, x, y);
            gui.renderItemDecorations(Minecraft.getInstance().font, renderStack, x, y, "");
            poseStack.popPose(); // 恢复矩阵状态，结束渲染
        }
    }

    @Override
    public void renderAmount(GuiGraphics gui, long amount, int x, int y)
    {
        // 渲染数量文本
        String countText = getCountText(amount);
        if (countText.isEmpty()) return;

        float scale = 0.666f; // 文本缩放因数
        var poseStackText = gui.pose();
        poseStackText.pushPose();
        poseStackText.translate(0, 0, 200); // 确保文本在顶层
        poseStackText.scale(scale, scale, scale); // 文本整体缩放，便于查看
        RenderSystem.disableBlend(); // 禁用混合渲染模式
        final int X = (int) (
                (x + -1 + 16.0f + 2.0f - Minecraft.getInstance().font.width(countText) * 0.666f)
                        * 1.0f / 0.666f
        );
        final int Y = (int) (
                (y + -1 + 16.0f - 5.0f * 0.666f)
                        * 1.0f / 0.666f
        );
        gui.drawString(Minecraft.getInstance().font, countText, X, Y, 0xFFFFFF);
        poseStackText.popPose();
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
    public List<Component> getTooltipLines(IStackKey<?> key, long amount, @Nullable Player player, TooltipFlag tooltipFlag)
    {
        if (key instanceof ItemStackKey itemKey)
        {
            ItemStack renderStack = itemKey.getRenderStack();
            List<Component> tooltips = renderStack.getTooltipLines(player, tooltipFlag);
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
    public void renderTooltip(GuiGraphics gui, Font font, IStackKey<?> key, long amount, int mouseX, int mouseY)
    {
        var minecraft = Minecraft.getInstance();
        gui.renderTooltip(minecraft.font, this.getTooltipLines(key, amount, minecraft.player, minecraft.options.advancedItemTooltips ? TooltipFlag.Default.ADVANCED : TooltipFlag.Default.NORMAL)
                , getTooltipImage(key), ItemStack.EMPTY, mouseX, mouseY);
    }
}