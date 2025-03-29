package com.wintercogs.beyonddimensions.Gui.Slots;

import com.cleanroommc.modularui.api.ITheme;
import com.cleanroommc.modularui.api.widget.Interactable;
import com.cleanroommc.modularui.drawable.GuiDraw;
import com.cleanroommc.modularui.integration.jei.JeiGhostIngredientSlot;
import com.cleanroommc.modularui.integration.jei.JeiIngredientProvider;
import com.cleanroommc.modularui.screen.Tooltip;
import com.cleanroommc.modularui.screen.viewport.GuiContext;
import com.cleanroommc.modularui.theme.WidgetSlotTheme;
import com.cleanroommc.modularui.theme.WidgetTheme;
import com.cleanroommc.modularui.widget.Widget;
import com.wintercogs.beyonddimensions.DataBase.Handler.IStackTypedHandler;
import com.wintercogs.beyonddimensions.DataBase.Stack.FluidStackType;
import com.wintercogs.beyonddimensions.DataBase.Stack.IStackType;
import com.wintercogs.beyonddimensions.DataBase.Stack.ItemStackType;
import com.wintercogs.beyonddimensions.Gui.Sync.UnorderdStackTypedHandlerSync;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.util.ITooltipFlag;

import javax.annotation.Nullable;
import java.util.List;

public class StackTypedSlot extends Widget<StackTypedSlot> implements Interactable, JeiGhostIngredientSlot<IStackType<?>>, JeiIngredientProvider
{
    // slot指向的是StackTypedHandler的第几个槽位？
    private int slotIndex;
    // slot对应的存储器，请赋值直接引用(备注，此处为显示存储器，真实存储器存在对应的同步器种)
    private IStackTypedHandler stackTypedHandler;
    // 指示此槽位是否为虚假槽位
    private boolean fake;


    public StackTypedSlot(int slotIndex, IStackTypedHandler stackTypedHandler)
    {
        super();
        this.slotIndex = slotIndex;
        this.stackTypedHandler = stackTypedHandler;
    }

    public StackTypedSlot(int slotIndex, IStackTypedHandler stackTypedHandler, boolean fake)
    {
        super();
        this.slotIndex = slotIndex;
        this.stackTypedHandler = stackTypedHandler;
        this.fake = fake;
    }

    public IStackType getTypedStackFromUnifiedStorage()
    {
        IStackType stackType = stackTypedHandler.getStackBySlot(getSlotIndex());
        if(stackType != null)
            return stackType.copy();
        else
            return new ItemStackType();
    }

    public int getSlotIndex()
    {
        return slotIndex;
    }

    public void setSlotIndex(int slotIndex)
    {
        this.slotIndex = slotIndex;
    }

    public boolean isFake()
    {
        return fake;
    }

    public void setFake(boolean fake)
    {
        this.fake = fake;
    }

    public StackTypedSlot syncHandler(IStackTypedHandler stackTypedHandler)
    {
        this.setSyncHandler(new UnorderdStackTypedHandlerSync(stackTypedHandler));
        return this;
    }

    @Override
    public void draw(GuiContext context, WidgetTheme widgetTheme) {
//        IFluidTank fluidTank = this.getFluidTank();
//        FluidStack content = this.syncHandler.getValue();
//        if (content != null) {
//            int y = this.contentOffsetY;
//            float height = (float)(this.getArea().height - y * 2);
//            if (!this.alwaysShowFull) {
//                float newHeight = height * (float)content.amount * 1.0F / (float)fluidTank.getCapacity();
//                y += (int)(height - newHeight);
//                height = newHeight;
//            }
//
//            GuiDraw.drawFluidTexture(content, (float)this.contentOffsetX, (float)y, (float)(this.getArea().width - this.contentOffsetX * 2), height, 0.0F);
//        }
//
//        if (content != null && this.syncHandler.controlsAmount()) {
//            String s = NumberFormat.formatWithMaxDigits(this.getBaseUnitAmount((double)content.amount)) + this.getBaseUnit();
//            this.textRenderer.setAlignment(Alignment.CenterRight, (float)(this.getArea().width - this.contentOffsetX) - 1.0F);
//            this.textRenderer.setPos((int)((float)this.contentOffsetX + 0.5F), (int)((float)this.getArea().height - 5.5F));
//            this.textRenderer.draw(s);
//        }


        // 渲染成分
        IStackType stackType = stackTypedHandler.getStackBySlot(getSlotIndex());
        if(stackType != null && !stackType.isEmpty())
        {
            // 图片以及数量
            stackType.render(1, 1);
        }

        // 渲染覆盖层
        if (this.isHovering()) {
            GlStateManager.colorMask(true, true, true, false);
            GuiDraw.drawRect(1.0F, 1.0F, (float)(this.getArea().w() - 2), (float)(this.getArea().h() - 2), ((WidgetSlotTheme)this.getWidgetTheme(context.getTheme())).getSlotHoverColor());
            GlStateManager.colorMask(true, true, true, true);
        }

    }

    @Override
    public Tooltip getTooltip()
    {
        IStackType stackType = stackTypedHandler.getStackBySlot(getSlotIndex());
        Tooltip tooltip = (new Tooltip()).excludeArea(this.getArea());
        if (stackType != null && !stackType.isEmpty())
        {

            for (String line : (List<String>) stackType.getTooltipLines(Minecraft.getMinecraft().player, Minecraft.getMinecraft().gameSettings.advancedItemTooltips ? ITooltipFlag.TooltipFlags.ADVANCED : ITooltipFlag.TooltipFlags.NORMAL))
            {
                tooltip.addLine(line);
            }
        }
        return tooltip;
    }

    @Override
    public WidgetTheme getWidgetTheme(ITheme theme)
    {
        return theme.getFluidSlotTheme();
    }

    @Override
    public void setGhostIngredient(IStackType<?> iStackType)
    {

    }

    @Nullable
    @Override
    public IStackType<?> castGhostIngredientIfValid( Object o)
    {
        return null;
    }


    @Override
    public Object getIngredient()
    {
        return null;
    }
}
