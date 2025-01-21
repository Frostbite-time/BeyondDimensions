package com.wintercogs.beyonddimensions.GUI;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import com.wintercogs.beyonddimensions.DataBase.ButtonState;
import com.wintercogs.beyonddimensions.GUI.Widget.Button.ReverseButton;
import com.wintercogs.beyonddimensions.GUI.Widget.Button.SortMethodButton;
import com.wintercogs.beyonddimensions.GUI.Widget.Scroller.BigScroller;
import com.wintercogs.beyonddimensions.Packet.SearchAndButtonGuiPacket;
import net.minecraft.client.gui.Font;
import com.wintercogs.beyonddimensions.Menu.DimensionsNetMenu;
import com.wintercogs.beyonddimensions.Packet.ScrollGuiPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import org.slf4j.Logger;
import java.util.HashMap;



public class DimensionsNetGUI extends AbstractContainerScreen<DimensionsNetMenu>
{

    private final Logger LOGGER = LogUtils.getLogger();
    private static final ResourceLocation GUI_TEXTURE = ResourceLocation.parse("beyonddimensions:textures/gui/dimensions_net.png");
    private EditBox searchField;
    private HashMap<String, ButtonState> buttonStateMap = new HashMap<>();
    private ReverseButton reverseButton;
    private SortMethodButton sortButton;
    private BigScroller scroller;
//    private double scrollCount = 0;

    public DimensionsNetGUI(DimensionsNetMenu container, Inventory playerInventory, Component title)
    {
        super(container, playerInventory, title);
        // 去除空白的真实部分，用于计算图片显示的最佳位置
        this.imageWidth = 198;
        this.imageHeight = 235;
    }



    @Override
    protected void init() {
        // 原父类方法--由于太少，显式写出来
        // 如果以后图片大小有变，显示中心所期望的大小仍然是x:176,y:235用于计算
        this.leftPos = (this.width - 176)/2;
        this.topPos = (this.height - 235)/2;

        // 初始化按钮组件
        sortButton = new SortMethodButton(this.leftPos+72+18*5-5,20,button ->
        {
            sortButton.toggleState();
            buttonStateMap.put(sortButton.getName(),sortButton.currentState);
        });
        addRenderableWidget(sortButton);

        reverseButton = new ReverseButton(this.leftPos+72+18*4-5,20,button ->
        {
            reverseButton.toggleState();
            buttonStateMap.put(reverseButton.getName(),reverseButton.currentState);
        });
        addRenderableWidget(reverseButton);

        // 初始化搜索方案
        buttonStateMap.put(sortButton.getName(),sortButton.currentState);
        buttonStateMap.put(reverseButton.getName(),reverseButton.currentState);

        // 重写部分
        this.searchField = new EditBox(getFont(), this.leftPos+20+26, this.topPos+4, 89, this.getFont().lineHeight+2, Component.translatable("wintercogs.BeyondDimensions.DimensionsGuiSearch"));
        this.searchField.setMaxLength(100);
        this.searchField.setBordered(true);
        this.searchField.setVisible(true);
        this.searchField.setTextColor(16777215);
        addRenderableWidget(searchField);

        // 初始化滚动按钮
        this.scroller = new BigScroller(this.leftPos+175,this.topPos+23,99,0,menu.maxLineData);
        addRenderableWidget(scroller);
    }

    @Override
    protected void containerTick() {
        //父类无操作
        //每tick自动更新搜索方案
        PacketDistributor.sendToServer(new SearchAndButtonGuiPacket(searchField.getValue(),buttonStateMap));
        scroller.updateScrollPosition(menu.lineData,menu.maxLineData);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY)
    {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, GUI_TEXTURE);
        guiGraphics.blit(GUI_TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks)
    {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTicks);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
        searchField.render(guiGraphics,mouseX,mouseY,partialTicks);
        reverseButton.render(guiGraphics,mouseX,mouseY,partialTicks);
        scroller.render(guiGraphics,mouseX,mouseY,partialTicks);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY)
    {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY+56+11, 4210752);
    }


    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY)
    {
        super.mouseScrolled(mouseX,mouseY,scrollX,scrollY);
        PacketDistributor.sendToServer(new ScrollGuiPacket(scrollY));
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        super.mouseDragged(mouseX,mouseY,button,dragX,dragY);
        // 父类的覆写方法没有显式调用其被拖拽的子元素的拖拽方法，所以需要手动调用
        scroller.mouseDragged(mouseX,mouseY,button,dragX,dragY);
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        super.mouseClicked(mouseX,mouseY,button);

        // 检查当前点击是否命中搜索框
        boolean flag =  searchField.active && searchField.visible && mouseX >= (double)searchField.getX() && mouseY >= (double)searchField.getY() && mouseX < (double)(searchField.getX() + searchField.getWidth()) && mouseY < (double)(searchField.getY() + searchField.getHeight());
        if(!flag)
        {
            if(this.getFocused() != null)
            {
                if(this.getFocused() == searchField)
                {   // 在未命中搜索框情况下 焦点不为空 且焦点为搜索框，则取消搜索框的焦点身份
                    searchField.setFocused(false);
                    this.setFocused(null);
                }
            }
        }
        return true;
    }

    public Font getFont() {
        return font;
    }


}