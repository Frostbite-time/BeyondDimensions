package com.wintercogs.beyonddimensions.GUI;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.wintercogs.beyonddimensions.Api.DataBase.ButtonState;
import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.Config;
import com.wintercogs.beyonddimensions.GUI.SharedWidget.IconButton;
import com.wintercogs.beyonddimensions.GUI.Widget.Button.ReverseButton;
import com.wintercogs.beyonddimensions.GUI.Widget.Button.SearchToggleButton;
import com.wintercogs.beyonddimensions.GUI.Widget.Button.SortMethodButton;
import com.wintercogs.beyonddimensions.GUI.Widget.Scroller.BigScroller;
import com.wintercogs.beyonddimensions.Menu.DimensionsCraftMenu;
import com.wintercogs.beyonddimensions.Menu.DimensionsNetMenu;
import com.wintercogs.beyonddimensions.Packet.OpenNetGuiPacket;
import com.wintercogs.beyonddimensions.ShortCutKey.DimensionsShortKeys;
import com.wintercogs.beyonddimensions.Unit.UIDataHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.phys.Vec2;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Objects;


public class DimensionsNetGUI<T extends DimensionsNetMenu> extends BDBaseGUI<T>
{

    protected static final ResourceLocation GUI_TEXTURE_TOP_BASE = ResourceLocation.parse("beyonddimensions:textures/gui/top_base.png");
    protected static final int TOP_BASE_WIDTH = 194;
    protected static final int TOP_BASE_HEIGHT = 24;
    protected static final ResourceLocation GUI_TEXTURE_TOP_SLOTS = ResourceLocation.parse("beyonddimensions:textures/gui/top_slots.png");
    protected static final int TOP_SLOTS_WIDTH = 194;
    protected static final int TOP_SLOTS_HEIGHT = 18;
    protected static final ResourceLocation GUI_TEXTURE_MID_SLOTS = ResourceLocation.parse("beyonddimensions:textures/gui/mid_slots.png");
    protected static final int MID_SLOTS_WIDTH = 194;
    protected static final int MID_SLOTS_HEIGHT = 18;
    protected static final ResourceLocation GUI_TEXTURE_BOTTOM_SLOTS = ResourceLocation.parse("beyonddimensions:textures/gui/bottom_slots.png");
    protected static final int BOTTOM_SLOTS_WIDTH = 194;
    protected static final int BOTTOM_SLOTS_HEIGHT = 26;
    protected static final ResourceLocation GUI_TEXTURE_PLAYER_INV = ResourceLocation.parse("beyonddimensions:textures/gui/player_inv.png");
    protected static final int PLAYER_INV_WIDTH = 176;
    protected static final int PLAYER_INV_HEIGHT = 89;

    protected EditBox searchField;
    protected String lastSearchText = "";
    protected ReverseButton reverseButton;
    protected SortMethodButton sortButton;
    protected SortMethodButton secondSortButton;
    protected SearchToggleButton searchToggleButton;
    protected IconButton addPageButton;
    protected IconButton removePageButton;
    protected IconButton craftButton;
    protected BigScroller scroller;

    private boolean isTransferMode = false;

    public DimensionsNetGUI(T container, Inventory playerInventory, Component title)
    {
        super(container, playerInventory, title);
    }



    @Override
    protected void init() {

        clearWidgets();

        if(UIDataHelper.isTransfer)
        {
            menu.lineData = UIDataHelper.currentPage;
            if(UIDataHelper.lastMousePos != null)
            {
                Window window = Minecraft.getInstance().getWindow();
                GLFW.glfwSetCursorPos(
                        window.getWindow(),
                        UIDataHelper.lastMousePos.x,
                        UIDataHelper.lastMousePos.y
                );
            }

            UIDataHelper.isTransfer = false;
        }



        // 计算最大行数
        int maxLines = calMaxLines();
        if(maxLines < menu.getLines())
        {
            // 自动计算不主动持久化参数
            if(maxLines<2)
                maxLines = 2;
            menu.setLines(maxLines);
            menu.rebuildSlots();
        }

        // 去除空白的真实部分，用于计算图片显示的最佳位置
        this.imageWidth = 194;
        // 计算真实高度
        this.imageHeight = rebuildImageHeight();

        // 用于计算期望的起点位置
        // 宽按176 高按235可以得到一个较好的效果
        this.leftPos = (this.width - 176)/2;
        this.topPos = (this.height - imageHeight)/2;

        // Label的渲染函数使用drawString，默认以topPos为起点
        rebuildLabelHeight();


        // 初始化按钮组件
        //排序按钮
        sortButton = new SortMethodButton(this.leftPos-18,this.topPos+6,button ->
        {
            sortButton.toggleState();
            Config.uiSortButton = (ButtonState) sortButton.currentState;
            Config.UI_SORT_BUTTON.set((ButtonState) sortButton.currentState);
            Config.UI_SORT_BUTTON.save();
            menu.buildIndexList(new ArrayList<>(menu.viewerStorage.getStorage()),true);
        });
        addRenderableWidget(sortButton);
        // 第二搜索策略按钮
        secondSortButton = new SortMethodButton(this.leftPos-18,this.topPos+6+18,button ->
        {
            secondSortButton.toggleState();
            Config.uiSecondSortButton = (ButtonState) secondSortButton.currentState;
            Config.UI_SECOND_SORT_BUTTON.set((ButtonState) secondSortButton.currentState);
            Config.UI_SECOND_SORT_BUTTON.save();
            menu.buildIndexList(new ArrayList<>(menu.viewerStorage.getStorage()),true);
        })
        {
            @Override
            protected void initButton()
            {
                iconMap.put(ButtonState.SORT_QUANTITY,ResourceLocation.tryBuild(BeyondDimensions.MODID,"widget/sort_quantity"));
                iconMap.put(ButtonState.SORT_NAME,ResourceLocation.tryBuild(BeyondDimensions.MODID,"widget/sort_name"));
                iconMap.put(ButtonState.SORT_MODID, ResourceLocation.tryBuild(BeyondDimensions.MODID,"widget/sort_modid"));
                iconMap.put(ButtonState.SORT_INSERTED_TIME, ResourceLocation.tryBuild(BeyondDimensions.MODID,"widget/sort_inserted_time"));
                iconMap.put(ButtonState.SORT_MODIFIED_TIME, ResourceLocation.tryBuild(BeyondDimensions.MODID,"widget/sort_modified_time"));

                tooltipMap.put(ButtonState.SORT_QUANTITY, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.sort_quantity_second")));
                tooltipMap.put(ButtonState.SORT_NAME, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.sort_name_second")));
                tooltipMap.put(ButtonState.SORT_MODID, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.sort_modid_second")));
                tooltipMap.put(ButtonState.SORT_INSERTED_TIME, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.sort_inserted_time_second")));
                tooltipMap.put(ButtonState.SORT_MODIFIED_TIME, Tooltip.create(Component.translatable(("tooltip.button.beyonddimensions.sort_modified_time_second"))));

                for(Enum<?> state : iconMap.keySet())
                {
                    this.states.add(state);
                }
                setState(Config.uiSecondSortButton);
            }
        };
        addRenderableWidget(secondSortButton);
        // 倒序切换按钮
        reverseButton = new ReverseButton(this.leftPos-18,this.topPos+6+18*2,button ->
        {
            reverseButton.toggleState();
            Config.uiReverseButton = (ButtonState) reverseButton.currentState;
            Config.UI_REVERSE_BUTTON.set((ButtonState) reverseButton.currentState);
            Config.UI_REVERSE_BUTTON.save();
            menu.buildIndexList(new ArrayList<>(menu.viewerStorage.getStorage()),true);
        });
        addRenderableWidget(reverseButton);
        // 搜索切换按钮
        searchToggleButton = new SearchToggleButton(this.leftPos-18,this.topPos+6+18*3,button ->{
            searchToggleButton.toggleState();
            Config.uiSearchButton = (ButtonState) searchToggleButton.currentState;
            Config.UI_SEARCH_BUTTON.set((ButtonState) searchToggleButton.currentState);
            Config.UI_SEARCH_BUTTON.save();
        });
        addRenderableWidget(searchToggleButton);

        //页面增减按钮
        addPageButton = new IconButton(this.leftPos-18,this.topPos+6+18*4,16,16,ResourceLocation.tryBuild(BeyondDimensions.MODID,"widget/up_arrow"), button ->
        {
            if(this.height - 36 <= (rebuildImageHeight()+MID_SLOTS_HEIGHT)
                || menu.getLines()>=99)
            {
                return;
            }
            menu.addLines();
            Config.uiPageNum = menu.getLines();
            Config.UI_PAGE_NUM.set(menu.getLines());
            Config.UI_PAGE_NUM.save();
            Config.uiSearch = searchField.getValue();
            this.imageHeight = rebuildImageHeight();
            menu.rebuildSlots();
            menu.buildIndexList(new ArrayList<>(menu.viewerStorage.getStorage()),true);
            init();
        });
        addPageButton.setTooltip(Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.add_page")));
        addRenderableWidget(addPageButton);

        removePageButton = new IconButton(this.leftPos-18,this.topPos+6+18*5,16,16,ResourceLocation.tryBuild(BeyondDimensions.MODID,"widget/down_arrow"), button ->
        {
            if(menu.getLines()<=2)
                return;
            menu.reduceLines();
            Config.uiPageNum = menu.getLines();
            Config.UI_PAGE_NUM.set(menu.getLines());
            Config.UI_PAGE_NUM.save();
            Config.uiSearch = searchField.getValue();
            this.imageHeight = rebuildImageHeight();
            menu.rebuildSlots();
            menu.buildIndexList(new ArrayList<>(menu.viewerStorage.getStorage()),true);
            init();
        });
        removePageButton.setTooltip(Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.remove_page")));
        addRenderableWidget(removePageButton);

        addCraftButton();



        // 初始化搜索方案
        this.searchField = new EditBox(getFont(), this.leftPos+60, this.topPos+7, 120, this.getFont().lineHeight+5, Component.translatable("wintercogs.beyonddimensions.dimensionsguisearch"));
        this.searchField.setMaxLength(200);
        this.searchField.setBordered(true);
        this.searchField.setVisible(true);
        this.searchField.setTextColor(16777215);
        this.searchField.setValue(Config.uiSearch);
        this.searchField.setTooltip(Tooltip.create(Component.translatable("tooltip.editbox.beyonddimensions.search")));
        if(!this.searchField.getValue().equals(""))
        {
            this.searchField.setSuggestion(null);
        }
        else
        {
            searchField.setSuggestion(Component.translatable("wintercogs.beyonddimensions.dimensionsguisearch").getString());
        }
        addRenderableWidget(searchField);

        // 初始化滚动按钮
        this.scroller = new BigScroller(this.leftPos+174,this.topPos+TOP_BASE_HEIGHT+1,18*menu.getLines() - 15 -2,0,menu.maxLineData);
        addRenderableWidget(scroller);

        lastSearchText = searchField.getValue();

    }

    @Override
    protected void containerTick()
    {
        super.containerTick();
        //父类无操作
        //每tick自动更新搜索方案
        if(!Objects.equals(lastSearchText, searchField.getValue()))
        {

            if(!searchField.getValue().equals(""))
                searchField.setSuggestion(null);
            else
                searchField.setSuggestion(Component.translatable("wintercogs.beyonddimensions.dimensionsguisearch").getString());

            menu.loadSearchText(searchField.getValue());
            Config.uiSearch = searchField.getValue();
            menu.buildIndexList(new ArrayList<>(menu.viewerStorage.getStorage()),true);
            lastSearchText = searchField.getValue();
        }
        scroller.updateScrollPosition(menu.lineData,menu.maxLineData);// 读取翻页数据并应用
    }

    // 用于让子类重写工艺槽位按钮的函数
    protected void addCraftButton()
    {
        craftButton = new IconButton(this.leftPos-18,this.topPos+6+18*6,16,16,ResourceLocation.tryBuild(BeyondDimensions.MODID,"widget/craft_button"), button ->
        {
            UIDataHelper.currentPage = menu.lineData;

            double xpos[] = new double[1];
            double ypos[] = new double[1];
            GLFW.glfwGetCursorPos(Minecraft.getInstance().getWindow().getWindow(), xpos, ypos);
            UIDataHelper.lastMousePos = new Vec2(
                    (float) xpos[0],
                    (float) ypos[0]
            );

            UIDataHelper.isTransfer = true;

            if(menu instanceof DimensionsCraftMenu)
            {
                Config.uiCraftButton = ButtonState.DISABLED;
                Config.UI_CRAFT_BUTTON.set(ButtonState.DISABLED);
                Config.UI_CRAFT_BUTTON.save();
                PacketDistributor.sendToServer(new OpenNetGuiPacket(menu.player.getStringUUID(),NetMenuType.NET_MENU));
            }
            else
            {
                Config.uiCraftButton = ButtonState.ENABLED;
                Config.UI_CRAFT_BUTTON.set(ButtonState.ENABLED);
                Config.UI_CRAFT_BUTTON.save();
                PacketDistributor.sendToServer(new OpenNetGuiPacket(menu.player.getStringUUID(),NetMenuType.NET_CRAFT_MENU));
            }
        });
        craftButton.setTooltip(Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.craft_toggle")));
        addRenderableWidget(craftButton);
    }

    protected int rebuildImageHeight()
    {
        return TOP_BASE_HEIGHT + TOP_SLOTS_HEIGHT + (menu.getLines()-2) * MID_SLOTS_HEIGHT + BOTTOM_SLOTS_HEIGHT + PLAYER_INV_HEIGHT;
    }

    protected void rebuildLabelHeight()
    {
        this.titleLabelY = 8;
        this.inventoryLabelY = TOP_BASE_HEIGHT + menu.getLines()*18+5;
    }

    protected int calMaxLines()
    {
        return (int)((this.height -36 - (TOP_BASE_HEIGHT+TOP_SLOTS_HEIGHT+BOTTOM_SLOTS_HEIGHT+PLAYER_INV_HEIGHT))/(float)MID_SLOTS_HEIGHT +2);
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
        menu.buildIndexList(new ArrayList<>(menu.viewerStorage.getStorage()),false);
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        super.mouseDragged(mouseX,mouseY,button,dragX,dragY);
        // 父类的覆写方法没有显式调用其被拖拽的子元素的拖拽方法，所以需要手动调用
        int scrollY =  scroller.customDragAction(mouseX,mouseY,button,dragX,dragY);
        int lastLine = menu.lineData;
        if (scrollY > 0)
        {
            menu.lineData--;
        } else if(scrollY < 0)
        {
            menu.lineData++;
        }
        //ScrollTo会处理lineData小于0的情况 并通知客户端翻页
        if(lastLine != menu.lineData) // 只有确实改变页面的情况下再重构索引
            menu.buildIndexList(new ArrayList<>(menu.viewerStorage.getStorage()),false);
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

        if(keyCode == GLFW.GLFW_KEY_LEFT_SHIFT || keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT)
            menu.hasShiftDown = true;


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
        if(this.minecraft.options.keyInventory.isActiveAndMatches(mouseKey) ||
                DimensionsShortKeys.OPEN_GUI_KEY.getKey() == mouseKey)
        {
            onClose();
            return true;
        }
        else
        {
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers)
    {
        boolean result = super.keyReleased(keyCode, scanCode, modifiers);

        if (keyCode == GLFW.GLFW_KEY_LEFT_SHIFT || keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT)
        {
            menu.updateViewerStorage();
            menu.hasShiftDown = false;
        }

        return result;
    }

    @Override
    public void removed()
    {
        super.removed();

        if(searchField != null)
        {
            if(searchField.getValue().length() > 0 && Config.uiSearchButton == ButtonState.ENABLED)
            {
                Config.uiSearch = searchField.getValue();
                Config.UI_SEARCH.set(searchField.getValue());
                Config.UI_SEARCH.save();
            }
            else
            {
                Config.uiSearch = "";
                Config.UI_SEARCH.set("");
                Config.UI_SEARCH.save();
            }
        }

    }

    public Font getFont()
    {
        return font;
    }

}