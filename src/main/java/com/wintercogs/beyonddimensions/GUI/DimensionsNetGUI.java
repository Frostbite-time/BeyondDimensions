package com.wintercogs.beyonddimensions.GUI;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.wintercogs.beyonddimensions.DataBase.ButtonName;
import com.wintercogs.beyonddimensions.DataBase.ButtonState;
import com.wintercogs.beyonddimensions.GUI.Widget.Button.ReverseButton;
import com.wintercogs.beyonddimensions.GUI.Widget.Button.SortMethodButton;
import com.wintercogs.beyonddimensions.GUI.Widget.Scroller.BigScroller;
import com.wintercogs.beyonddimensions.Menu.DimensionsNetMenu;
import com.wintercogs.beyonddimensions.Menu.Slot.StoredItemStackSlot;
import com.wintercogs.beyonddimensions.Packet.CallSeverClickPacket;
import com.wintercogs.beyonddimensions.Packet.CallSeverStoragePacket;
import com.wintercogs.beyonddimensions.Unit.StringFormat;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;


public class DimensionsNetGUI extends AbstractContainerScreen<DimensionsNetMenu>
{

    private static final ResourceLocation GUI_TEXTURE = ResourceLocation.parse("beyonddimensions:textures/gui/dimensions_net.png");
    private EditBox searchField;
    private HashMap<ButtonName, ButtonState> buttonStateMap = new HashMap<>();
    private HashMap<ButtonName,ButtonState> lastButtonStateMap = new HashMap<>();
    private String lastSearchText = "";
    private ReverseButton reverseButton;
    private SortMethodButton sortButton;
    private BigScroller scroller;

    public DimensionsNetGUI(DimensionsNetMenu container, Inventory playerInventory, Component title)
    {
        super(container, playerInventory, title);
        // 去除空白的真实部分，用于计算图片显示的最佳位置
        this.imageWidth = 198;
        this.imageHeight = 235;
    }



    @Override
    protected void init() {
        // 如果以后图片大小有变，显示中心所期望的大小仍然是x:176,y:235用于计算
        this.leftPos = (this.width - 176)/2;
        this.topPos = (this.height - 235)/2;

        // 初始化按钮组件
        sortButton = new SortMethodButton(this.leftPos+72+18*5-5,this.topPos+6,button ->
        {
            sortButton.toggleState();
            buttonStateMap.put(sortButton.getName(),sortButton.currentState);
        });
        addRenderableWidget(sortButton);

        reverseButton = new ReverseButton(this.leftPos+72+18*4-5,this.topPos+6,button ->
        {
            reverseButton.toggleState();
            buttonStateMap.put(reverseButton.getName(),reverseButton.currentState);
        });
        addRenderableWidget(reverseButton);

        buttonStateMap.put(sortButton.getName(),sortButton.currentState);
        buttonStateMap.put(reverseButton.getName(),reverseButton.currentState);

        // 初始化搜索方案
        this.searchField = new EditBox(getFont(), this.leftPos+20+26, this.topPos+4, 89, this.getFont().lineHeight+2, Component.translatable("wintercogs.BeyondDimensions.DimensionsGuiSearch"));
        this.searchField.setMaxLength(100);
        this.searchField.setBordered(true);
        this.searchField.setVisible(true);
        this.searchField.setTextColor(16777215);
        addRenderableWidget(searchField);

        // 初始化滚动按钮
        this.scroller = new BigScroller(this.leftPos+175,this.topPos+23,99,0,menu.maxLineData);
        addRenderableWidget(scroller);

        lastButtonStateMap = new HashMap<>(buttonStateMap);
        lastSearchText = searchField.getValue();

        menu.itemStorage.getStorage().clear();
        menu.suppressRemoteUpdates();
        PacketDistributor.sendToServer(new CallSeverStoragePacket());
    }

    @Override
    protected void containerTick() {
        //父类无操作
        //每tick自动更新搜索方案
        if(!lastButtonStateMap.equals(buttonStateMap) || !Objects.equals(lastSearchText, searchField.getValue()))
        {
            menu.loadSearchText(searchField.getValue());
            menu.loadButtonState(buttonStateMap);
            menu.buildIndexList(new ArrayList<>(menu.viewerItemStorage.getStorage()));
            lastButtonStateMap = new HashMap<>(buttonStateMap);
            lastSearchText = searchField.getValue();
        }
        scroller.updateScrollPosition(menu.lineData,menu.maxLineData);// 读取翻页数据并应用
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
    protected void renderSlot(GuiGraphics guiGraphics, Slot slot) {
        if(slot instanceof StoredItemStackSlot sSlot)
        {
            int x = slot.x;
            int y = slot.y;
            ItemStack itemstack = slot.getItem();
            if(itemstack != null)
            {
                var poseStack = guiGraphics.pose();
                poseStack.pushPose();

                var displayStack = itemstack.copy();
                guiGraphics.renderItem(displayStack, x, y);
                guiGraphics.renderItemDecorations(minecraft.font, displayStack, x, y, "");

                poseStack.popPose();

                int count = sSlot.getItemCount();
                if(count<=0)
                {
                    return;
                }
                String countText = StringFormat.formatCount(count);

                var stack = guiGraphics.pose();
                stack.pushPose();
                // According to ItemRenderer, text is 200 above items.
                stack.translate(0, 0, 200);
                stack.scale(0.666f, 0.666f, 0.666f);

                RenderSystem.disableBlend();
                final int X = (int) ((x + -1 + 16.0f + 2.0f - this.font.width(countText) * 0.666f)
                        * 1.0f / 0.666f);
                final int Y = (int) ((y + -1 + 16.0f - 5.0f * 0.666f) * 1.0f / 0.666f);
                MultiBufferSource.BufferSource buffer = MultiBufferSource.immediate(new ByteBufferBuilder(512));
                this.font.drawInBatch(countText, X + 1, Y + 1, 0x413f54, false, stack.last().pose(), buffer, Font.DisplayMode.NORMAL, 0,
                        15728880);
                this.font.drawInBatch(countText, X, Y, 0xffffff, false, stack.last().pose(), buffer, Font.DisplayMode.NORMAL, 0, 15728880);
                buffer.endBatch();
                RenderSystem.enableBlend();

                stack.popPose();
            }

        }
        else
        {
            super.renderSlot(guiGraphics,slot);
        }

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
        menu.ScrollTo();
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
        menu.ScrollTo();
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

        // 处理点击槽位
        Slot slot = findSlot(mouseX,mouseY);
        if(slot != null)
        {
            if (!menu.isHanding)
            {
                int slotId = slot.index;
                ItemStack clickItem;
                if(hasShiftDown())
                {
                    if(slot instanceof StoredItemStackSlot sSlot)
                    {
                        clickItem = sSlot.getVanillaActualStack();
                    }
                    else
                    {
                        clickItem = slot.getItem();
                    }
                    menu.isHanding = true;
                    PacketDistributor.sendToServer(new CallSeverClickPacket(slotId,clickItem,button,true));
                }
                else
                {
                    if(slot instanceof StoredItemStackSlot sSlot)
                    {
                        clickItem = sSlot.getVanillaActualStack();
                        menu.isHanding = true;
                        PacketDistributor.sendToServer(new CallSeverClickPacket(slotId,clickItem,button,false));
                    }
                }
            }
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

    public Font getFont() {
        return font;
    }

}