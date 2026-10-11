package com.wintercogs.beyonddimensions.integration.module.jade;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wintercogs.beyonddimensions.api.storage.key.IStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.api.storage.key.impl.ItemStackKey;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec2;
import snownee.jade.api.ui.Element;
import snownee.jade.api.ui.IDisplayHelper;
import snownee.jade.api.ui.IElement;

/**
 * Jade 提示里的一种资源：图标与数量都由资源自己的渲染器画出，与存储界面的格子一致
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

    static String amountText(KeyAmount stack)
    {
        return stack.key().getRender().getCountText(stack.amount());
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
            key.getRender().renderAmount(gui, stack.amount(), 0, 0);
        pose.popPose();
    }

    @Override
    public String getMessage()
    {
        return "%s %s".formatted(stack.amount(), name(stack.key()).getString());
    }
}
