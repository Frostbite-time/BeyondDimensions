package com.wintercogs.beyonddimensions.GUI.SharedWidget;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.IntConsumer;

@OnlyIn(Dist.CLIENT)
public class ScrollBar extends AbstractWidget
{

    /**
     * 滑块贴图
     */
    protected final ResourceLocation SPRITE;

    /**
     * 轨道可滑动像素长度（滑块“顶部”从0到末端的位移量）
     */
    protected int maxScrollLength;

    /**
     * 当前位置（0..maxPosition）
     */
    protected int currentPosition;

    /**
     * 最大位置（总数据“起始行”或总索引；包含可见+不可见）
     */
    protected int maxPosition;

    /**
     * 步长（滚轮/量化的最小单位）
     */
    protected int step = 1;

    /**
     * 当前滑块像素偏移（相对组件y；只用于渲染/命中，不改变组件y）
     */
    protected int scrollerOffset = 0;

    /**
     * 是否按“滑块中心对齐鼠标”（true更自然；false为顶部对齐）
     */
    protected boolean alignCenterToMouse = true;

    /**
     * 拖拽状态
     */
    protected boolean isDragging = false;

    /**
     * 位置变化回调（在 setCurrentPosition() 时触发）
     */
    protected @Nullable IntConsumer onScroll;

    /**
     * @param x            组件左上角X（固定不动）
     * @param y            组件左上角Y（固定不动：轨道起点Y）
     * @param width        滑块宽度（不是轨道宽）
     * @param height       滑块高度（不是轨道高）
     * @param sprite       滑块贴图
     * @param maxScrollLen 轨道可滑动像素长度（滑块顶部0..此值）
     * @param currentPos   初始当前位置（0..maxPosition）
     * @param maxPos       最大位置
     * @param onScroll     位置变化回调，可为null
     * @param message      读屏文本
     */
    public ScrollBar(int x, int y, int width, int height,
                     ResourceLocation sprite,
                     int maxScrollLen,
                     int currentPos,
                     int maxPos,
                     @Nullable IntConsumer onScroll,
                     Component message)
    {
        super(x, y, width, height, message);
        this.SPRITE = sprite;
        this.maxScrollLength = Math.max(0, maxScrollLen);
        this.maxPosition = Math.max(0, maxPos);
        this.onScroll = onScroll;
        setCurrentPosition(currentPos); // 内部会量化+回调（如有变化）
    }

    /* ---------------------------- 外部API ---------------------------- */

    public void setOnScroll(@Nullable IntConsumer cb)
    {
        this.onScroll = cb;
    }

    public void setAlignCenterToMouse(boolean center)
    {
        this.alignCenterToMouse = center;
    }

    /**
     * 动态更新“当前位置/最大位置”（会触发量化+回调）
     */
    public void updateScrollPosition(int currentPosition, int maxPosition)
    {
        this.maxPosition = Math.max(0, maxPosition);
        setCurrentPosition(currentPosition);
    }

    /**
     * 动态更新轨道长度（像素）
     */
    public void setMaxScrollLength(int maxScrollLength)
    {
        this.maxScrollLength = Math.max(0, maxScrollLength);
        // 长度变化后，偏移重新按当前位置换算（renderWidget里会计算，这里可不做）
    }

    /**
     * 设置步长（>=1）
     */
    public void setStep(int step)
    {
        this.step = Math.max(1, step);
        setCurrentPosition(this.currentPosition); // 重新量化到步长网格
    }

    public int getStep()
    {
        return this.step;
    }

    /**
     * 相对滚动“步数”（>0 向下，<0 向上）
     */
    public void scrollBySteps(int steps)
    {
        if (maxPosition <= 0) return;
        int unit = Math.max(1, this.step);
        long target = (long) this.currentPosition + (long) steps * unit; // 防溢出
        setCurrentPosition((int) Mth.clamp(target, 0, this.maxPosition));
    }

    /**
     * 把当前位置锚到“鼠标在轨道上的比例”
     */
    public void scrollToMouse(double mouseY)
    {
        if (maxPosition <= 0 || maxScrollLength <= 0) return;

        double anchorOffset = alignCenterToMouse ? (this.getHeight() / 2.0) : 0.0;
        double relative = (mouseY - this.getY() - anchorOffset) / (double) this.maxScrollLength;
        double clamped = Mth.clamp(relative, 0.0, 1.0);
        int pos = (int) Math.round(clamped * this.maxPosition);
        setCurrentPosition(pos);
    }

    /**
     * 设置当前位置（统一出口：clamp + 步长量化 + 回调）
     */
    public void setCurrentPosition(int pos)
    {
        int clamped = Mth.clamp(pos, 0, Math.max(0, this.maxPosition));
        int quantized = quantizeToStep(clamped);
        if (quantized != this.currentPosition)
        {
            this.currentPosition = quantized;
            if (this.onScroll != null) this.onScroll.accept(this.currentPosition);
        }
    }

    /* ---------------------------- 内部工具 ---------------------------- */

    /**
     * 四舍五入到最近步长
     */
    protected int quantizeToStep(int value)
    {
        if (step <= 1) return value;
        int q = Math.round(value / (float) step) * step;
        return Mth.clamp(q, 0, Math.max(0, this.maxPosition));
    }

    /**
     * 根据 current/max → 计算像素偏移（滑块“顶部”）
     */
    protected int computeOffset()
    {
        if (maxPosition > 0 && maxScrollLength > 0)
        {
            return (int) Math.round(maxScrollLength * (this.currentPosition / (double) this.maxPosition));
        }
        return 0;
    }

    /* ---------------------------- 事件处理 ---------------------------- */

    /**
     * 整个轨道（y .. y + maxScrollLength + knobHeight）都算 hover，可点击/拖拽
     */
    @Override
    public boolean isMouseOver(double mouseX, double mouseY)
    {
        int left = this.getX();
        int right = left + this.getWidth();
        int top = this.getY();
        int bottom = top + this.maxScrollLength + this.getHeight(); // 包含滑块在底部的范围
        return mouseX >= left && mouseX < right && mouseY >= top && mouseY < bottom;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        if (!this.active || !this.visible) return false;
        if (!this.isValidClickButton(button)) return false;

        // 点击在整片轨道上都算：先将滑块跳到该位置，再进入拖拽
        if (this.isMouseOver(mouseX, mouseY))
        {
            this.playDownSound(Minecraft.getInstance().getSoundManager());
            this.onClick(mouseX, mouseY);
            this.isDragging = true;
            scrollToMouse(mouseY);
            return true;
        }
        return false;
    }

    @Override
    protected void onDrag(double mouseX, double mouseY, double dragX, double dragY)
    {
        if (!isDragging) return;
        scrollToMouse(mouseY);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY)
    {
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public void onRelease(double mouseX, double mouseY)
    {
        this.isDragging = false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY)
    {
        if (!this.active || !this.visible) return false;
        if (maxPosition <= 0) return false;

        int dir = (int) Math.signum(scrollY); // +1 上滚，-1 下滚
        if (dir != 0)
        {
            // 上滚 → 位置减小；下滚 → 位置增大
            scrollBySteps(-dir);
            return true;
        }
        return false;
    }

    /* ---------------------------- 渲染/无障碍 ---------------------------- */

    @Override
    protected void renderWidget(@NotNull GuiGraphics gg, int mouseX, int mouseY, float partialTick)
    {
        // 组件自身x/y不变，只根据当前位置计算“渲染偏移”
        this.scrollerOffset = computeOffset();

        gg.setColor(1.0F, 1.0F, 1.0F, this.alpha);
        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();

        // 在 (x, y + scrollerOffset) 处绘制“滑块”
        gg.blit(
                SPRITE,
                this.getX(),
                this.getY() + this.scrollerOffset,
                0, 0,
                this.getWidth(), this.getHeight(),
                this.getWidth(), this.getHeight()
        );

        gg.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput out)
    {
        out.add(NarratedElementType.TITLE, this.createNarrationMessage());
        if (this.active)
        {
            if (this.isFocused())
            {
                out.add(NarratedElementType.USAGE, Component.translatable("beyonddimensions.scrollbar.usage.focused"));
            }
            else
            {
                out.add(NarratedElementType.USAGE, Component.translatable("beyonddimensions.scrollbar.usage.hovered"));
            }
        }
    }
}