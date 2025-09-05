package com.wintercogs.beyonddimensions.Api.DataBase.Stack;

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

public interface IStackRender<T>
{
    /**
     * UI渲染，即绘制当前资源的图标。
     * <p>
     * 必须以注解标注为仅客户端
     */
    @OnlyIn(Dist.CLIENT)
    void render(GuiGraphics gui, T key, int x, int y);

    void renderAmount(GuiGraphics gui, long amount, int x, int y);

    /**
     * 对当前存储数量进行格式化
     */
    String getCountText(long count);

    /**
     * 获取资源名称
     */
    Component getDisplayName(T key);

    /**
     * 获取资源的工具提示
     */
    List<Component> getTooltipLines(T key, long amount, Item.TooltipContext tooltipContext, @Nullable Player player, TooltipFlag tooltipFlag);

    /**
     *
     */
    Optional<TooltipComponent> getTooltipImage(T key);

    /**
     * 绘制工具提示，必须要标记为仅客户端
     */
    @OnlyIn(Dist.CLIENT)
    void renderTooltip(GuiGraphics gui, Font font, T key, long amount, int mouseX, int mouseY);
}
