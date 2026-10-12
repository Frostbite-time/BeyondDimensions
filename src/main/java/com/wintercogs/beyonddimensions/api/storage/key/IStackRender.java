package com.wintercogs.beyonddimensions.api.storage.key;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wintercogs.beyonddimensions.util.StringFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

/**
 * 用与StackKey的渲染器，实现一般可以使用单例模式
 */
public interface IStackRender
{
    /**
     * UI渲染，即绘制当前资源的图标
     * <p>
     * 必须以注解标注为仅客户端
     */
    @OnlyIn(Dist.CLIENT)
    void render(GuiGraphics gui, IStackKey<?> key, int x, int y);

    /**
     * 在 (x, y) 处 16×16 图标的右下角画出数量，写法见 {@link #getCountText}。
     * 字号为原版的 2/3，文字连同投影收在图标内，与存储界面格子里的数量一致
     */
    @OnlyIn(Dist.CLIENT)
    default void renderAmount(GuiGraphics gui, long amount, int x, int y)
    {
        String text = getCountText(amount);
        if (text.isEmpty())
            return;
        Font font = Minecraft.getInstance().font;
        float scale = 2f / 3f;
        PoseStack pose = gui.pose();
        pose.pushPose();
        // 原版数字连同投影高 8 像素、宽为 font.width
        pose.translate(x + 16 - font.width(text) * scale, y + 16 - 8 * scale, 200);
        pose.scale(scale, scale, 1);
        gui.drawString(font, text, 0, 0, 0xFFFFFF, true);
        pose.popPose();
    }

    /**
     * 格子里的数量写法，默认与存储界面相同，见 {@link StringFormat#formatCompact}
     */
    default String getCountText(long count)
    {
        return count < 0 ? "" : StringFormat.formatCompact(count);
    }

    /**
     * 获取资源名称
     */
    Component getDisplayName(IStackKey<?> key);

    /**
     * 获取资源的工具提示
     */
    List<Component> getTooltipLines(IStackKey<?> key, long amount, Item.TooltipContext tooltipContext, @Nullable Player player, TooltipFlag tooltipFlag);

    /**
     *
     */
    Optional<TooltipComponent> getTooltipImage(IStackKey<?> key);

    /**
     * 绘制工具提示，必须要标记为仅客户端
     */
    @OnlyIn(Dist.CLIENT)
    void renderTooltip(GuiGraphics gui, Font font, IStackKey<?> key, long amount, int mouseX, int mouseY);
}
