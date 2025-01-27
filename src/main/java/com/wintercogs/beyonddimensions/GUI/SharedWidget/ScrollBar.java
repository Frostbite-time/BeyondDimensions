package com.wintercogs.beyonddimensions.GUI.SharedWidget;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import com.wintercogs.beyonddimensions.Menu.DimensionsNetMenu;
import com.wintercogs.beyonddimensions.Packet.ScrollGuiPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import org.slf4j.Logger;

// 这是一个自定义的滑动条类，要实例化它，你需要传入以下数据
// 滑动条的起始渲染点x、y、滑动条的宽度、高度、它能向下滑行的最大像素长度、用于计算滑动条当前位置的数据、用于设置滑动条最大位置的数据
// 这个类用于定义滑动条的基本结构和易用方法，对于实际应用，请使用子类继承
@OnlyIn(Dist.CLIENT)
public class ScrollBar extends AbstractWidget
{
    private final Logger LOGGER = LogUtils.getLogger();
    protected ResourceLocation SPRITE;
    protected int maxScrollLength = 0;
    public int currentPosition = 0;
    public int maxPosition = 0;
    private double dragHold = 0; // 用于计数累计拖动量
    private boolean isDragging = false; // 用于确定当前是否处于被拖拽状态
    private int startY;

    public ScrollBar(int x, int y, int width, int height,ResourceLocation sprite, int maxScrollLength,int currentPosition,int maxPosition,Component message)
    {
        super(x, y, width, height, message);
        this.SPRITE = sprite;
        this.maxScrollLength = maxScrollLength;
        this.currentPosition = currentPosition;
        this.maxPosition = maxPosition;
        startY = this.getY();
    }


    public void updateScrollPosition(int currentPosition,int maxPosition)
    {
        this.currentPosition = currentPosition;
        this.maxPosition = maxPosition;
    }

    public int customDragAction(double mouseX, double mouseY,int button ,double dragX, double dragY)
    {
        if(button != 0)
        {
            return 0;
        }
        if(maxPosition !=0 && isDragging)
        {
            dragHold += dragY;
            double scrollhold = ((double) maxScrollLength / maxPosition)/1.5;
            if(dragHold>scrollhold||dragHold<-scrollhold)
            {
                double drag = dragHold;
                dragHold = 0;
                if(drag >0)
                {
                    return -1; //返回一个与drag同号的数，以便使用相同逻辑处理
                }
                else
                {
                    return 1;
                }
            }
            else
            {
                return 0;
            }
        }
        return 0;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.active && this.visible) {
            if (this.isValidClickButton(button)) { //被左键点击
                boolean flag = this.clicked(mouseX, mouseY);
                if (flag) {
                    this.playDownSound(Minecraft.getInstance().getSoundManager());
                    this.onClick(mouseX, mouseY, button);
                    //此处为自己的逻辑
                    LOGGER.info("点击事件捕获");
                    isDragging = true;
                    return true;
                }
            }

            return false;
        } else {
            return false;
        }
    }

    @Override
    public void onRelease(double mouseX, double mouseY) {
        LOGGER.info("释放事件捕获");
        isDragging = false;
    }

    @Override
    protected void onDrag(double mouseX, double mouseY, double dragX, double dragY)
    {

    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int i, int i1, float v)
    {
        //由于render无法重写，也未找到其他可重写的每tick自动执行的命令，故用此方法更新XY
        int scrollerOffset;
        if(maxPosition != 0)
            scrollerOffset = (int) (maxScrollLength * ((float)currentPosition/ (float)maxPosition));
        else
            scrollerOffset = 0;
        this.setY(this.startY+scrollerOffset);
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, this.alpha);
        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
        guiGraphics.blitSprite(SPRITE, this.getX(), this.getY(), this.getWidth(), this.getHeight());
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput)
    {
        narrationElementOutput.add(NarratedElementType.TITLE, this.createNarrationMessage());
        if (this.active) {
            if (this.isFocused()) {
                narrationElementOutput.add(NarratedElementType.USAGE, Component.translatable("beyonddimensions.scrollbar.usage.focused"));
            } else {
                narrationElementOutput.add(NarratedElementType.USAGE, Component.translatable("beyonddimensions.scrollbar.usage.hovered"));
            }
        }
    }
}
