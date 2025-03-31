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
import com.cleanroommc.modularui.value.sync.SyncHandler;
import com.cleanroommc.modularui.widget.Widget;
import com.wintercogs.beyonddimensions.DataBase.Handler.IStackTypedHandler;
import com.wintercogs.beyonddimensions.DataBase.Stack.IStackType;
import com.wintercogs.beyonddimensions.DataBase.Stack.ItemStackType;
import com.wintercogs.beyonddimensions.Gui.Sync.ClickActionSync;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.util.ITooltipFlag;
import org.lwjgl.input.Keyboard;

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

    // 用于设置同步器
    public StackTypedSlot syncHandler(SyncHandler syncHandler)
    {
        this.setSyncHandler(syncHandler);
        return this;
    }

    public ClickActionSync getClickActionSync()
    {
        if(getSyncHandler() instanceof ClickActionSync sync)
            return sync;
        return null;
    }

    @Override
    public void draw(GuiContext context, WidgetTheme widgetTheme)
    {

        // 渲染成分
        IStackType stackType = getTypedStackFromUnifiedStorage();
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
    public Result onMousePressed(int mouseButton)
    {
        Interactable.super.onMousePressed(mouseButton);
        if(getClickActionSync() != null)
        {
            ClickActionSync clickActionSync = getClickActionSync();
            clickActionSync.isSlotFake = isFake();
            clickActionSync.isShiftDown = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)||Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
            clickActionSync.clickStack = getTypedStackFromUnifiedStorage();
            clickActionSync.button = mouseButton;
            clickActionSync.slotIndex = getSlotIndex(); //获取在仓储中的索引
            clickActionSync.syncToServer(0,clickActionSync::write);
            return Result.ACCEPT;
        }
        return Result.ACCEPT;
    }

    // JEI幽灵槽位处理
    @Override
    public void setGhostIngredient(IStackType<?> iStackType)
    {

    }

    @Nullable
    @Override
    public IStackType<?> castGhostIngredientIfValid(Object o)
    {
        return null;
    }


    @Override
    public Object getIngredient()
    {
        IStackType stackType = getTypedStackFromUnifiedStorage();
        if(stackType != null&& !stackType.isEmpty())
            return stackType;
        else
            return null;
    }
}
