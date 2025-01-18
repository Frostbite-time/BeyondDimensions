package com.wintercogs.beyonddimensions.GUI;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import com.wintercogs.beyonddimensions.DataBase.ButtonState;
import com.wintercogs.beyonddimensions.GUI.Widget.Button.ReverseButton;
import com.wintercogs.beyonddimensions.GUI.Widget.Button.SortMethodButton;
import com.wintercogs.beyonddimensions.Packet.SearchAndButtonGuiPacket;
import net.minecraft.client.gui.Font;
import com.wintercogs.beyonddimensions.Menu.DimensionsNetMenu;
import com.wintercogs.beyonddimensions.Packet.ScrollGuiPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
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

    public DimensionsNetGUI(DimensionsNetMenu container, Inventory playerInventory, Component title)
    {
        super(container, playerInventory, title);
        this.imageWidth = 256;
        this.imageHeight = 256;
    }



    @Override
    protected void init() {
        // 原父类方法--由于太少，显式写出来
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;

        // 初始化按钮组件
        sortButton = new SortMethodButton(this.leftPos+100+18*5,40,button ->
        {
            sortButton.toggleState();
            buttonStateMap.put(sortButton.getName(),sortButton.currentState);
        });
        addRenderableWidget(sortButton);

        reverseButton = new ReverseButton(this.leftPos+100+18*4,40,button ->
        {
            reverseButton.toggleState();
            buttonStateMap.put(reverseButton.getName(),reverseButton.currentState);
        });
        addRenderableWidget(reverseButton);

        // 初始化搜索方案
        buttonStateMap.put(sortButton.getName(),sortButton.currentState);
        buttonStateMap.put(reverseButton.getName(),reverseButton.currentState);

        // 重写部分
        this.searchField = new EditBox(getFont(), this.leftPos+48+36, this.topPos+26, 89, this.getFont().lineHeight, Component.translatable("wintercogs.BeyondDimensions.DimensionsGuiSearch"));
        this.searchField.setMaxLength(100);
        this.searchField.setBordered(true);
        this.searchField.setVisible(true);
        this.searchField.setTextColor(16777215);
        addRenderableWidget(searchField);
    }

    @Override
    protected void containerTick() {
        //父类无操作
        PacketDistributor.sendToServer(new SearchAndButtonGuiPacket(searchField.getValue(),buttonStateMap));
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY)
    {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, GUI_TEXTURE);
        guiGraphics.blit(GUI_TEXTURE, this.leftPos, this.topPos, -28, -20, this.imageWidth, this.imageHeight);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks)
    {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTicks);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
        searchField.render(guiGraphics,mouseX,mouseY,partialTicks);
        reverseButton.render(guiGraphics,mouseX,mouseY,partialTicks);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY)
    {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX+28, this.titleLabelY+20, 4210752);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX+28, this.inventoryLabelY+56+13, 4210752);
    }


    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY)
    {
        PacketDistributor.sendToServer(new ScrollGuiPacket(scrollY));
        return true;
    }

    public Font getFont() {
        return font;
    }


}