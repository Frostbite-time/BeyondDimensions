package com.wintercogs.beyonddimensions.client.gui;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.wintercogs.beyonddimensions.Config;
import com.wintercogs.beyonddimensions.api.ButtonState;
import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.client.gui.widget.button.ReverseButton;
import com.wintercogs.beyonddimensions.client.gui.widget.button.SearchToggleButton;
import com.wintercogs.beyonddimensions.client.gui.widget.button.SortMethodButton;
import com.wintercogs.beyonddimensions.client.gui.widget.scroller.BigScroller;
import com.wintercogs.beyonddimensions.client.gui.widget.shared.IconButton;
import com.wintercogs.beyonddimensions.client.init.BDShortKeys;
import com.wintercogs.beyonddimensions.common.menu.DimensionsCraftMenu;
import com.wintercogs.beyonddimensions.common.menu.DimensionsNetMenu;
import com.wintercogs.beyonddimensions.config.CommonConfigRuntime;
import com.wintercogs.beyonddimensions.integration.ModPresence;
import com.wintercogs.beyonddimensions.integration.OtherModIds;
import com.wintercogs.beyonddimensions.integration.module.jei.BDjeiPlugin;
import com.wintercogs.beyonddimensions.network.packet.c2s.OpenNetGuiPacket;
import com.wintercogs.beyonddimensions.network.packet.c2s.OpenPrimaryNetSwitcherPacket;
import com.wintercogs.beyonddimensions.util.UIDataHelper;
import dev.emi.emi.api.EmiApi;
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
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

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
    protected IconButton primaryNetSwitcherButton;
    protected BigScroller scroller;

    public DimensionsNetGUI(T container, Inventory playerInventory, Component title)
    {
        super(container, playerInventory, title);
    }

    @Override
    protected void init()
    {
        super.init();

        clearWidgets();

        if (UIDataHelper.isTransfer)
        {
            menu.lineData = UIDataHelper.currentPage;
            if (UIDataHelper.lastMousePos != null)
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
        if (maxLines < menu.getLines())
        {
            // 自动计算不主动持久化参数
            if (maxLines < 2)
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
        this.leftPos = (this.width - 176) / 2;
        this.topPos = (this.height - imageHeight) / 2;

        // Label的渲染函数使用drawString，默认以topPos为起点
        rebuildLabelHeight();


        // 初始化按钮组件
        //排序按钮
        sortButton = new SortMethodButton(this.leftPos - 18, this.topPos + 6, button ->
        {
            sortButton.toggleState();
            CommonConfigRuntime.uiSortButton = (ButtonState) sortButton.currentState;
            Config.INSTANCE.commonConfig.UI_SORT_BUTTON.set((ButtonState) sortButton.currentState);
            Config.INSTANCE.commonConfig.UI_SORT_BUTTON.save();
            menu.buildIndexList();
        });
        addRenderableWidget(sortButton);
        // 第二搜索策略按钮
        secondSortButton = new SortMethodButton(this.leftPos - 18, this.topPos + 6 + 18, button ->
        {
            secondSortButton.toggleState();
            CommonConfigRuntime.uiSecondSortButton = (ButtonState) secondSortButton.currentState;
            Config.INSTANCE.commonConfig.UI_SECOND_SORT_BUTTON.set((ButtonState) secondSortButton.currentState);
            Config.INSTANCE.commonConfig.UI_SECOND_SORT_BUTTON.save();
            menu.buildIndexList();
        })
        {
            @Override
            protected void initButton()
            {
                iconMap.put(ButtonState.SORT_CREATIVE_TAB, ResourceLocation.tryBuild(BDConstants.MODID, "widget/sort_creative_tab"));
                iconMap.put(ButtonState.SORT_MAX_STACK, ResourceLocation.tryBuild(BDConstants.MODID, "widget/sort_max_stack"));
                iconMap.put(ButtonState.SORT_QUANTITY, ResourceLocation.tryBuild(BDConstants.MODID, "widget/sort_quantity"));
                iconMap.put(ButtonState.SORT_NAME, ResourceLocation.tryBuild(BDConstants.MODID, "widget/sort_name"));
                iconMap.put(ButtonState.SORT_MODID, ResourceLocation.tryBuild(BDConstants.MODID, "widget/sort_modid"));
                iconMap.put(ButtonState.SORT_INSERTED_TIME, ResourceLocation.tryBuild(BDConstants.MODID, "widget/sort_inserted_time"));
                iconMap.put(ButtonState.SORT_MODIFIED_TIME, ResourceLocation.tryBuild(BDConstants.MODID, "widget/sort_modified_time"));

                tooltipMap.put(ButtonState.SORT_CREATIVE_TAB, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.sort_creative_tab_second")));
                tooltipMap.put(ButtonState.SORT_MAX_STACK, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.sort_max_stack_second")));
                tooltipMap.put(ButtonState.SORT_QUANTITY, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.sort_quantity_second")));
                tooltipMap.put(ButtonState.SORT_NAME, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.sort_name_second")));
                tooltipMap.put(ButtonState.SORT_MODID, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.sort_modid_second")));
                tooltipMap.put(ButtonState.SORT_INSERTED_TIME, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.sort_inserted_time_second")));
                tooltipMap.put(ButtonState.SORT_MODIFIED_TIME, Tooltip.create(Component.translatable(("tooltip.button.beyonddimensions.sort_modified_time_second"))));

                for (Enum<?> state : iconMap.keySet())
                {
                    this.states.add(state);
                }
                setState(CommonConfigRuntime.uiSecondSortButton);
            }
        };
        addRenderableWidget(secondSortButton);
        // 倒序切换按钮
        reverseButton = new ReverseButton(this.leftPos - 18, this.topPos + 6 + 18 * 2, button ->
        {
            reverseButton.toggleState();
            CommonConfigRuntime.uiReverseButton = (ButtonState) reverseButton.currentState;
            Config.INSTANCE.commonConfig.UI_REVERSE_BUTTON.set((ButtonState) reverseButton.currentState);
            Config.INSTANCE.commonConfig.UI_REVERSE_BUTTON.save();
            menu.buildIndexList();
        });
        addRenderableWidget(reverseButton);
        // 搜索切换按钮
        searchToggleButton = new SearchToggleButton(this.leftPos - 18, this.topPos + 6 + 18 * 3, button -> {
            searchToggleButton.toggleState();
            CommonConfigRuntime.uiSearchButton = (ButtonState) searchToggleButton.currentState;
            Config.INSTANCE.commonConfig.UI_SEARCH_BUTTON.set((ButtonState) searchToggleButton.currentState);
            Config.INSTANCE.commonConfig.UI_SEARCH_BUTTON.save();
        });
        addRenderableWidget(searchToggleButton);

        //页面增减按钮
        addPageButton = new IconButton(this.leftPos - 18, this.topPos + 6 + 18 * 4, 16, 16, ResourceLocation.tryBuild(BDConstants.MODID, "widget/up_arrow"), button ->
        {
            if (this.height - 36 <= (rebuildImageHeight() + MID_SLOTS_HEIGHT)
                    || menu.getLines() >= 99)
            {
                return;
            }
            menu.addLines();
            CommonConfigRuntime.uiPageNum = menu.getLines();
            Config.INSTANCE.commonConfig.UI_PAGE_NUM.set(menu.getLines());
            Config.INSTANCE.commonConfig.UI_PAGE_NUM.save();
            CommonConfigRuntime.uiSearch = searchField.getValue();
            this.imageHeight = rebuildImageHeight();
            menu.rebuildSlots();
            menu.buildIndexList();
            init();
        });
        addPageButton.setTooltip(Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.add_page")));
        addRenderableWidget(addPageButton);

        removePageButton = new IconButton(this.leftPos - 18, this.topPos + 6 + 18 * 5, 16, 16, ResourceLocation.tryBuild(BDConstants.MODID, "widget/down_arrow"), button ->
        {
            if (menu.getLines() <= 2)
                return;
            menu.reduceLines();
            CommonConfigRuntime.uiPageNum = menu.getLines();
            Config.INSTANCE.commonConfig.UI_PAGE_NUM.set(menu.getLines());
            Config.INSTANCE.commonConfig.UI_PAGE_NUM.save();
            CommonConfigRuntime.uiSearch = searchField.getValue();
            this.imageHeight = rebuildImageHeight();
            menu.rebuildSlots();
            menu.buildIndexList();
            init();
        });
        removePageButton.setTooltip(Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.remove_page")));
        addRenderableWidget(removePageButton);

        addCraftButton();
        addPrimaryNetSwitcherButton();


        // 初始化搜索方案
        this.searchField = new EditBox(getFont(), this.leftPos + 60, this.topPos + 7, 120, this.getFont().lineHeight + 5, Component.translatable("wintercogs.beyonddimensions.dimensionsguisearch"));
        this.searchField.setMaxLength(200);
        this.searchField.setBordered(true);
        this.searchField.setVisible(true);
        this.searchField.setTextColor(16777215);
        this.searchField.setTooltip(Tooltip.create(Component.translatable("tooltip.editbox.beyonddimensions.search")));
        this.searchField.setResponder(text -> {
            if (text.isEmpty())
            {
                searchField.setSuggestion(Component.translatable("wintercogs.beyonddimensions.dimensionsguisearch").getString());
            }
            else
            {
                searchField.setSuggestion(null);
            }
            menu.loadSearchText(text);
            CommonConfigRuntime.uiSearch = text;
            menu.markForceAllUpdateClientView();
            menu.updateViewerStorage(false);
            lastSearchText = text;

            // 文本同步
            if (CommonConfigRuntime.searchTextWithJEIEMI)
            {
                // JEI
                if (ModPresence.isLoaded(OtherModIds.JEI))
                {
                    BDjeiPlugin.runtime().ifPresent(rt -> {
                        var overlay = rt.getIngredientFilter();
                        String current = overlay.getFilterText();
                        if (!Objects.equals(current, text))
                        {
                            overlay.setFilterText(text);
                        }
                    });
                }
                // 同步到EMI
                if (ModPresence.isLoaded(OtherModIds.EMI))
                {
                    String current = EmiApi.getSearchText();
                    if (!Objects.equals(current, text))
                    {
                        EmiApi.setSearchText(text);
                    }
                }
            }
        });
        if (!this.searchField.getValue().equals(""))
        {
            this.searchField.setSuggestion(null);
        }
        else
        {
            searchField.setSuggestion(Component.translatable("wintercogs.beyonddimensions.dimensionsguisearch").getString());
        }
        this.searchField.setValue(CommonConfigRuntime.uiSearch);
        addRenderableWidget(searchField);

        // 初始化滚动条
        int trackLength = 18 * menu.getLines() - 15 - 2;
        this.scroller = new BigScroller(
                this.leftPos + 174,
                this.topPos + TOP_BASE_HEIGHT + 1,
                trackLength,
                menu.lineData,
                menu.maxLineData,
                pos ->
                {
                    if (menu.lineData != pos)
                    {
                        menu.lineData = pos;
                        menu.buildIndexList();
                    }
                }
        );
        this.scroller.setStep(1);
        addRenderableWidget(scroller);

        lastSearchText = searchField.getValue();

    }

    @Override
    protected void containerTick()
    {
        super.containerTick();
        // 每tick从emi或jei同步一次文本
        if (CommonConfigRuntime.searchTextWithJEIEMI)
        {
            if (ModPresence.isLoaded(OtherModIds.JEI))
            {
                BDjeiPlugin.runtime().ifPresent(rt -> {
                    var overlay = rt.getIngredientFilter();
                    String current = overlay.getFilterText();
                    if (!Objects.equals(current, lastSearchText))
                    {
                        searchField.setValue(current);
                    }
                });
            }

            if (ModPresence.isLoaded(OtherModIds.EMI))
            {
                String current = EmiApi.getSearchText();
                if (!Objects.equals(current, lastSearchText))
                {
                    searchField.setValue(current);
                }
            }
        }

        // 更新滑动条信息
        scroller.updateScrollPosition(menu.lineData, menu.maxLineData); // 读取翻页数据并应用
    }

    // 用于让子类重写工艺槽位按钮的函数
    protected void addCraftButton()
    {
        craftButton = new IconButton(this.leftPos - 18, this.topPos + 6 + 18 * 6, 16, 16, ResourceLocation.tryBuild(BDConstants.MODID, "widget/craft_button"), button ->
        {
            saveTransferContext();

            if (menu instanceof DimensionsCraftMenu)
            {
                CommonConfigRuntime.uiCraftButton = ButtonState.DISABLED;
                Config.INSTANCE.commonConfig.UI_CRAFT_BUTTON.set(ButtonState.DISABLED);
                Config.INSTANCE.commonConfig.UI_CRAFT_BUTTON.save();
                PacketDistributor.sendToServer(new OpenNetGuiPacket(menu.player.getStringUUID(), NetMenuType.NET_MENU));
            }
            else
            {
                CommonConfigRuntime.uiCraftButton = ButtonState.ENABLED;
                Config.INSTANCE.commonConfig.UI_CRAFT_BUTTON.set(ButtonState.ENABLED);
                Config.INSTANCE.commonConfig.UI_CRAFT_BUTTON.save();
                PacketDistributor.sendToServer(new OpenNetGuiPacket(menu.player.getStringUUID(), NetMenuType.NET_CRAFT_MENU));
            }
        });
        craftButton.setTooltip(Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.craft_toggle")));
        addRenderableWidget(craftButton);
    }

    protected void addPrimaryNetSwitcherButton()
    {
        primaryNetSwitcherButton = new IconButton(this.leftPos - 18, this.topPos + 6 + 18 * 7, 16, 16, ResourceLocation.tryBuild(BDConstants.MODID, "widget/opposite_arrow"), button ->
        {
            saveTransferContext();
            PacketDistributor.sendToServer(new OpenPrimaryNetSwitcherPacket());
        });
        primaryNetSwitcherButton.setTooltip(Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.open_primary_net_switcher")));
        addRenderableWidget(primaryNetSwitcherButton);
    }

    private void saveTransferContext()
    {
        UIDataHelper.currentPage = menu.lineData;

        double[] xpos = new double[1];
        double[] ypos = new double[1];
        GLFW.glfwGetCursorPos(Minecraft.getInstance().getWindow().getWindow(), xpos, ypos);
        UIDataHelper.lastMousePos = new Vec2(
                (float) xpos[0],
                (float) ypos[0]
        );

        UIDataHelper.isTransfer = true;
    }

    protected int rebuildImageHeight()
    {
        return TOP_BASE_HEIGHT + TOP_SLOTS_HEIGHT + (menu.getLines() - 2) * MID_SLOTS_HEIGHT + BOTTOM_SLOTS_HEIGHT + PLAYER_INV_HEIGHT;
    }

    protected void rebuildLabelHeight()
    {
        this.titleLabelY = 8;
        this.inventoryLabelY = TOP_BASE_HEIGHT + menu.getLines() * 18 + 5;
    }

    protected int calMaxLines()
    {
        return (int) ((this.height - 36 - (TOP_BASE_HEIGHT + TOP_SLOTS_HEIGHT + BOTTOM_SLOTS_HEIGHT + PLAYER_INV_HEIGHT)) / (float) MID_SLOTS_HEIGHT + 2);
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
        for (int i = 0; i < menu.getLines() - 2; i++)
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
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks)
    {
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY)
    {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY)
    {
        boolean result = super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        if (!result) // 让滑动条全局可滑
            result = scroller.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        return result;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        boolean result = super.mouseClicked(mouseX, mouseY, button);

        // 处理对搜索框的焦点取消
        boolean flag = searchField.active && searchField.visible && mouseX >= (double) searchField.getX() && mouseY >= (double) searchField.getY() && mouseX < (double) (searchField.getX() + searchField.getWidth()) && mouseY < (double) (searchField.getY() + searchField.getHeight());
        if (!flag)
        {
            if (this.getFocused() != null)
            {
                if (this.getFocused() == searchField)
                {   // 在未命中搜索框情况下 焦点不为空 且焦点为搜索框，则取消搜索框的焦点身份
                    searchField.setFocused(false);
                    this.setFocused(null);
                }
            }
        }
        else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) // 右键点击搜索框则清空搜索框内容
        {
            searchField.setValue("");
        }

        return result;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers)
    {
        // 先处理menu相关数据
        if (keyCode == GLFW.GLFW_KEY_LEFT_SHIFT || keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT)
            menu.hasShiftDown = true;

        InputConstants.Key mouseKey = InputConstants.getKey(keyCode, scanCode);

        // 如果搜索框有效，拦截，然后让搜索框接管处理
        if (searchField != null && searchField.canConsumeInput() && mouseKey.getValue() != GLFW.GLFW_KEY_ESCAPE)
        {
            // 无论如何都不继续后续逻辑
            // 等以后可能改为重写searchField以获得更稳定的效果
            searchField.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }

        // 处理shift + z切换
        if (hasShiftDown() && mouseKey.getValue() == GLFW.GLFW_KEY_Z)
        {
            boolean current = CommonConfigRuntime.searchTextWithJEIEMI;
            CommonConfigRuntime.searchTextWithJEIEMI = !current;
            Config.INSTANCE.commonConfig.SEARCH_TEXT_WITH_JEI_EMI.set(!current);
            Config.INSTANCE.commonConfig.SEARCH_TEXT_WITH_JEI_EMI.save();
            return true;
        }

        // 处理背包关闭热键
        if (this.minecraft.options.keyInventory.isActiveAndMatches(mouseKey) ||
                BDShortKeys.OPEN_GUI_KEY.getKey() == mouseKey)
        {
            onClose();
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers)
    {
        boolean result = super.keyReleased(keyCode, scanCode, modifiers);

        if (keyCode == GLFW.GLFW_KEY_LEFT_SHIFT || keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT)
        {
            menu.markForceAllUpdateClientView();
            menu.updateViewerStorage(false);
            menu.hasShiftDown = false;
        }

        return result;
    }

    @Override
    public void removed()
    {
        super.removed();

        if (searchField != null)
        {
            if (searchField.getValue().length() > 0 && CommonConfigRuntime.uiSearchButton == ButtonState.ENABLED)
            {
                CommonConfigRuntime.uiSearch = searchField.getValue();
                Config.INSTANCE.commonConfig.UI_SEARCH.set(searchField.getValue());
                Config.INSTANCE.commonConfig.UI_SEARCH.save();
            }
            else
            {
                CommonConfigRuntime.uiSearch = "";
                Config.INSTANCE.commonConfig.UI_SEARCH.set("");
                Config.INSTANCE.commonConfig.UI_SEARCH.save();
            }
        }

    }

    public Font getFont()
    {
        return font;
    }

}
