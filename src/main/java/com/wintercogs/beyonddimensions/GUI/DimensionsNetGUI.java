package com.wintercogs.beyonddimensions.GUI;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.Config;
import com.wintercogs.beyonddimensions.DataBase.ButtonName;
import com.wintercogs.beyonddimensions.DataBase.ButtonState;
import com.wintercogs.beyonddimensions.GUI.SharedWidget.IconButton;
import com.wintercogs.beyonddimensions.GUI.Widget.Button.ReverseButton;
import com.wintercogs.beyonddimensions.GUI.Widget.Button.SortMethodButton;
import com.wintercogs.beyonddimensions.GUI.Widget.Scroller.BigScroller;
import com.wintercogs.beyonddimensions.Menu.DimensionsNetMenu;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;


public class DimensionsNetGUI extends BDBaseGUI<DimensionsNetMenu>
{

    private static final ResourceLocation GUI_TEXTURE_TOP_BASE = ResourceLocation.parse("beyonddimensions:textures/gui/top_base.png");
    private static final int TOP_BASE_WIDTH = 194;
    private static final int TOP_BASE_HEIGHT = 24;
    private static final ResourceLocation GUI_TEXTURE_TOP_SLOTS = ResourceLocation.parse("beyonddimensions:textures/gui/top_slots.png");
    private static final int TOP_SLOTS_WIDTH = 194;
    private static final int TOP_SLOTS_HEIGHT = 18;
    private static final ResourceLocation GUI_TEXTURE_MID_SLOTS = ResourceLocation.parse("beyonddimensions:textures/gui/mid_slots.png");
    private static final int MID_SLOTS_WIDTH = 194;
    private static final int MID_SLOTS_HEIGHT = 18;
    private static final ResourceLocation GUI_TEXTURE_BOTTOM_SLOTS = ResourceLocation.parse("beyonddimensions:textures/gui/bottom_slots.png");
    private static final int BOTTOM_SLOTS_WIDTH = 194;
    private static final int BOTTOM_SLOTS_HEIGHT = 26;
    private static final ResourceLocation GUI_TEXTURE_PLAYER_INV = ResourceLocation.parse("beyonddimensions:textures/gui/player_inv.png");
    private static final int PLAYER_INV_WIDTH = 176;
    private static final int PLAYER_INV_HEIGHT = 89;

    private EditBox searchField;
    private HashMap<ButtonName, ButtonState> buttonStateMap = new HashMap<>();
    private HashMap<ButtonName,ButtonState> lastButtonStateMap = new HashMap<>();
    private String lastSearchText = "";
    private ReverseButton reverseButton;
    private SortMethodButton sortButton;
    private IconButton addPageButton;
    private IconButton removePageButton;
    private BigScroller scroller;

    public DimensionsNetGUI(DimensionsNetMenu container, Inventory playerInventory, Component title)
    {
        super(container, playerInventory, title);
        // 去除空白的真实部分，用于计算图片显示的最佳位置
        this.imageWidth = 194;

        // 计算真实高度
        this.imageHeight = TOP_BASE_HEIGHT + TOP_SLOTS_HEIGHT + (container.getLines()-2) * MID_SLOTS_HEIGHT + BOTTOM_SLOTS_HEIGHT + PLAYER_INV_HEIGHT;
    }



    @Override
    protected void init() {

        clearWidgets();
        // 用于计算期望的起点位置
        // 宽按176 高按235可以得到一个较好的效果
        this.leftPos = (this.width - 176)/2;
        this.topPos = (this.height - imageHeight)/2;

        // Label的渲染函数使用drawString，默认以topPos为起点
        this.titleLabelY = 8;
        this.inventoryLabelY = TOP_BASE_HEIGHT + menu.getLines()*18+5;

        // 初始化按钮组件
        //排序按钮
        sortButton = new SortMethodButton(this.leftPos-18,this.topPos+6,button ->
        {
            sortButton.toggleState();
            buttonStateMap.put(sortButton.getName(),sortButton.currentState);
            Config.uiSortButton = sortButton.currentState;
            Config.UI_SORT_BUTTON.set(sortButton.currentState);
            Config.UI_SORT_BUTTON.save();
        });
        addRenderableWidget(sortButton);
        // 倒序切换按钮
        reverseButton = new ReverseButton(this.leftPos-18,this.topPos+6+18,button ->
        {
            reverseButton.toggleState();
            buttonStateMap.put(reverseButton.getName(),reverseButton.currentState);
            Config.uiReverseButton = reverseButton.currentState;
            Config.UI_REVERSE_BUTTON.set(reverseButton.currentState);
            Config.UI_REVERSE_BUTTON.save();
        });
        addRenderableWidget(reverseButton);

        buttonStateMap.put(sortButton.getName(),sortButton.currentState);
        buttonStateMap.put(reverseButton.getName(),reverseButton.currentState);

        //页面增减按钮
        addPageButton = new IconButton(this.leftPos-18,this.topPos+6+18*2,16,16,ResourceLocation.tryBuild(BeyondDimensions.MODID,"widget/sort_asc"),ButtonName.AddPageButton , button ->
        {
            if(this.height - 36 <= (TOP_BASE_HEIGHT + TOP_SLOTS_HEIGHT + (menu.getLines()-1) * MID_SLOTS_HEIGHT + BOTTOM_SLOTS_HEIGHT + PLAYER_INV_HEIGHT)
                || menu.getLines()>=99)
            {
                return;
            }
            menu.addLines();
            Config.uiPageNum = menu.getLines();
            Config.UI_PAGE_NUM.set(menu.getLines());
            Config.UI_PAGE_NUM.save();
            this.imageHeight = TOP_BASE_HEIGHT + TOP_SLOTS_HEIGHT + (menu.getLines()-2) * MID_SLOTS_HEIGHT + BOTTOM_SLOTS_HEIGHT + PLAYER_INV_HEIGHT;
            menu.rebuildSlots();
            menu.buildIndexList(new ArrayList<>(menu.viewerStorage.getStorage()));
            init();
        });
        addRenderableWidget(addPageButton);

        removePageButton = new IconButton(this.leftPos-18,this.topPos+6+18*3,16,16,ResourceLocation.tryBuild(BeyondDimensions.MODID,"widget/sort_desc"),ButtonName.RemovePageButton , button ->
        {
            if(menu.getLines()<=2)
                return;
            menu.reduceLines();
            Config.uiPageNum = menu.getLines();
            Config.UI_PAGE_NUM.set(menu.getLines());
            Config.UI_PAGE_NUM.save();
            this.imageHeight = TOP_BASE_HEIGHT + TOP_SLOTS_HEIGHT + (menu.getLines()-2) * MID_SLOTS_HEIGHT + BOTTOM_SLOTS_HEIGHT + PLAYER_INV_HEIGHT;
            menu.rebuildSlots();
            menu.buildIndexList(new ArrayList<>(menu.viewerStorage.getStorage()));
            init();
        });
        addRenderableWidget(removePageButton);


        // 初始化搜索方案
        this.searchField = new EditBox(getFont(), this.leftPos+60, this.topPos+7, 120, this.getFont().lineHeight+5, Component.translatable("wintercogs.beyonddimensions.dimensionsguisearch"));
        this.searchField.setSuggestion(Component.translatable("wintercogs.beyonddimensions.dimensionsguisearch").getString());
        this.searchField.setMaxLength(100);
        this.searchField.setBordered(true);
        this.searchField.setVisible(true);
        this.searchField.setTextColor(16777215);
        addRenderableWidget(searchField);

        // 初始化滚动按钮
        this.scroller = new BigScroller(this.leftPos+174,this.topPos+TOP_BASE_HEIGHT+1,18*menu.getLines() - 15 -2,0,menu.maxLineData);
        addRenderableWidget(scroller);

        lastButtonStateMap = new HashMap<>(buttonStateMap);
        lastSearchText = searchField.getValue();

    }

    @Override
    protected void containerTick() {
        //父类无操作
        //每tick自动更新搜索方案
        if(!lastButtonStateMap.equals(buttonStateMap) || !Objects.equals(lastSearchText, searchField.getValue()))
        {

            if(!searchField.getValue().equals(""))
                searchField.setSuggestion(null);
            else
                searchField.setSuggestion(Component.translatable("wintercogs.beyonddimensions.dimensionsguisearch").getString());

            menu.loadSearchText(searchField.getValue());
            menu.loadButtonState(buttonStateMap);
            menu.buildIndexList(new ArrayList<>(menu.viewerStorage.getStorage()));
            lastButtonStateMap = new HashMap<>(buttonStateMap);
            lastSearchText = searchField.getValue();
        }
        scroller.updateScrollPosition(menu.lineData,menu.maxLineData);// 读取翻页数据并应用
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY)
    {
        int drawY = this.topPos; // 用于动态控制绘制
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        RenderSystem.setShaderTexture(0, GUI_TEXTURE_TOP_BASE);
        guiGraphics.blit(GUI_TEXTURE_TOP_BASE, this.leftPos, drawY, 0, 0, TOP_BASE_WIDTH, TOP_BASE_HEIGHT, TOP_BASE_WIDTH, TOP_BASE_HEIGHT);
        drawY += TOP_BASE_HEIGHT;

        RenderSystem.setShaderTexture(0, GUI_TEXTURE_TOP_SLOTS);
        guiGraphics.blit(GUI_TEXTURE_TOP_SLOTS, this.leftPos, drawY, 0, 0, TOP_SLOTS_WIDTH, TOP_SLOTS_HEIGHT, TOP_SLOTS_WIDTH, TOP_SLOTS_HEIGHT);
        drawY += TOP_SLOTS_HEIGHT;

        RenderSystem.setShaderTexture(0, GUI_TEXTURE_MID_SLOTS);
        for(int i = 0;i<menu.getLines()-2;i++)
        {
            guiGraphics.blit(GUI_TEXTURE_MID_SLOTS, this.leftPos, drawY, 0, 0, MID_SLOTS_WIDTH, MID_SLOTS_HEIGHT, MID_SLOTS_WIDTH, MID_SLOTS_HEIGHT);
            drawY += MID_SLOTS_HEIGHT;
        }

        RenderSystem.setShaderTexture(0, GUI_TEXTURE_BOTTOM_SLOTS);
        guiGraphics.blit(GUI_TEXTURE_BOTTOM_SLOTS, this.leftPos, drawY, 0, 0, BOTTOM_SLOTS_WIDTH, BOTTOM_SLOTS_HEIGHT, BOTTOM_SLOTS_WIDTH, BOTTOM_SLOTS_HEIGHT);
        drawY += BOTTOM_SLOTS_HEIGHT;

        RenderSystem.setShaderTexture(0, GUI_TEXTURE_PLAYER_INV);
        guiGraphics.blit(GUI_TEXTURE_PLAYER_INV, this.leftPos, drawY, 0, 0, PLAYER_INV_WIDTH, PLAYER_INV_HEIGHT, PLAYER_INV_WIDTH, PLAYER_INV_HEIGHT);
        //drawY += PLAYER_INV_HEIGHT;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks)
    {
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY)
    {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752,false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752,false);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY)
    {
        super.mouseScrolled(mouseX,mouseY,scrollX,scrollY);
        if (scrollY > 0)
        {
            menu.lineData--;
        } else if(scrollY < 0)
        {
            menu.lineData++;
        }
        //ScrollTo会处理lineData小于0的情况 并通知客户端翻页
        menu.buildIndexList(new ArrayList<>(menu.viewerStorage.getStorage()));
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        super.mouseDragged(mouseX,mouseY,button,dragX,dragY);
        // 父类的覆写方法没有显式调用其被拖拽的子元素的拖拽方法，所以需要手动调用
        int scrollY =  scroller.customDragAction(mouseX,mouseY,button,dragX,dragY);
        if (scrollY > 0)
        {
            menu.lineData--;
        } else if(scrollY < 0)
        {
            menu.lineData++;
        }
        //ScrollTo会处理lineData小于0的情况 并通知客户端翻页
        menu.buildIndexList(new ArrayList<>(menu.viewerStorage.getStorage()));
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        super.mouseClicked(mouseX,mouseY,button);

        // 处理对搜索框的焦点取消
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
        else if(button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) // 右键点击搜索框则清空搜索框内容
        {
            searchField.setValue("");
        }

        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        InputConstants.Key mouseKey = InputConstants.getKey(keyCode, scanCode);
        if(this.searchField.isFocused())
        {
            if ((mouseKey.getValue()>=48 &&mouseKey.getValue()<=57) || // 属于数字键
                    (mouseKey.getValue()>=65 &&mouseKey.getValue()<=90) || // 属于字母键
                    (mouseKey.getValue()>=320 &&mouseKey.getValue()<=329) || // 属于小键盘数字
                    mouseKey.getValue() == 32 ) // 属于空格
            {
                // 当搜索框为焦点且属于常见输入时，禁止其他操作
                return true;
            }
        }
        if(this.minecraft.options.keyInventory.isActiveAndMatches(mouseKey))
        {
            return true;
        }
        else
        {
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
    }

    public Font getFont()
    {
        return font;
    }

}