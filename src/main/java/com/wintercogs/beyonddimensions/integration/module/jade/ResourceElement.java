package com.wintercogs.beyonddimensions.integration.module.jade;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wintercogs.beyonddimensions.api.storage.key.IStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.api.storage.key.impl.ItemStackKey;
import com.wintercogs.beyonddimensions.client.ui.kit.BdFormatKt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec2;
import snownee.jade.api.theme.IThemeHelper;
import snownee.jade.api.ui.Element;
import snownee.jade.api.ui.IDisplayHelper;
import snownee.jade.api.ui.IElement;

/**
 * Jade 提示里的一种资源：BD 的资源图标，数量的字号与位置同 Jade 的物品格子
 */
final class ResourceElement extends Element
{
    private static final Vec2 SIZE = new Vec2(18, 18);
    private static final Vec2 SMALL_SIZE = new Vec2(10, 9);

    private final KeyAmount stack;
    private final boolean small;

    private ResourceElement(KeyAmount stack, boolean small)
    {
        this.stack = stack;
        this.small = small;
    }

    static IElement of(KeyAmount stack)
    {
        return new ResourceElement(stack, false);
    }

    /**
     * 与名称同行的小图标，不带数量
     */
    static IElement small(KeyAmount stack)
    {
        return new ResourceElement(stack, true).translate(new Vec2(0, -1)).message(null);
    }

    /**
     * 与存储界面格子里的数量写法相同
     */
    static String amountText(long amount)
    {
        return BdFormatKt.formatCompact(amount);
    }

    /**
     * 资源名称；物品的显示名带方括号，这里用不带括号的名称
     */
    static Component name(IStackKey<?> key)
    {
        if (key instanceof ItemStackKey itemKey)
            return itemKey.getReadOnlyStack().getHoverName();
        return key.getRender().getDisplayName(key);
    }

    @Override
    public Vec2 getSize()
    {
        return small ? SMALL_SIZE : SIZE;
    }

    @Override
    public void render(GuiGraphics gui, float x, float y, float maxX, float maxY)
    {
        // 与 Jade 的物品一样，提示框淡出到一半以下时不画
        if (IDisplayHelper.get().opacity() < 0.5F)
            return;
        IStackKey<?> key = stack.key();
        PoseStack pose = gui.pose();
        pose.pushPose();
        pose.translate(x + 1, y + 1, 0);
        if (small)
            pose.scale(0.5F, 0.5F, 0.5F);
        key.getRender().render(gui, key, 0, 0);
        if (!small && stack.amount() != 1)
            drawAmount(gui, amountText(stack.amount()));
        pose.popPose();
    }

    // 三个字符以内用 0.75 倍字号，更长时用 0.5 倍，靠右下角
    private static void drawAmount(GuiGraphics gui, String text)
    {
        Font font = Minecraft.getInstance().font;
        boolean longer = text.length() > 3;
        float scale = longer ? 0.5F : 0.75F;
        PoseStack pose = gui.pose();
        pose.pushPose();
        pose.translate(0, 0, 200);
        pose.scale(scale, scale, 1);
        gui.drawString(font, text, (longer ? 32 : 22) - font.width(text), longer ? 23 : 13,
                IThemeHelper.get().theme().text.itemAmountColor(), true);
        pose.popPose();
    }

    @Override
    public String getMessage()
    {
        return "%s %s".formatted(stack.amount(), name(stack.key()).getString());
    }
}
