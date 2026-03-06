package com.wintercogs.beyonddimensions.api.storage.key;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.TooltipFlag;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

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
     * 将数量绘制到屏幕上
     */
    void renderAmount(GuiGraphics gui, long amount, int x, int y);

    /**
     * 对当前存储数量进行格式化
     */
    String getCountText(long count);

    /**
     * 获取资源名称
     */
    Component getDisplayName(IStackKey<?> key);

    /**
     * 获取资源的工具提示
     */
    List<Component> getTooltipLines(IStackKey<?> key, long amount, @Nullable Player player, TooltipFlag tooltipFlag);

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
