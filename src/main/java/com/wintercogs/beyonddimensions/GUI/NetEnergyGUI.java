package com.wintercogs.beyonddimensions.GUI;

import com.mojang.blaze3d.systems.RenderSystem;
import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.DataBase.ButtonState;
import com.wintercogs.beyonddimensions.GUI.Widget.Button.ReverseButton;
import com.wintercogs.beyonddimensions.Menu.NetEnergyMenu;
import com.wintercogs.beyonddimensions.Packet.CallSeverStoragePacket;
import com.wintercogs.beyonddimensions.Packet.PopModeButtonPacket;
import com.wintercogs.beyonddimensions.Unit.StringFormat;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.network.PacketDistributor;

public class NetEnergyGUI extends AbstractContainerScreen<NetEnergyMenu>
{
    private static final ResourceLocation GUI_TEXTURE = ResourceLocation.parse("beyonddimensions:textures/gui/net_energy_storage.png");

    public ReverseButton popButton; // 使用倒序按钮来临时替代弹出模式


    public NetEnergyGUI(NetEnergyMenu container, Inventory playerInventory, Component title)
    {
        super(container, playerInventory, title);
        // 去除空白的真实部分，用于计算图片显示的最佳位置
        this.imageWidth = 176;
        this.imageHeight = 175;
    }



    @Override
    protected void init() {
        // 如果以后图片大小有变，显示中心所期望的大小仍然是x:176,y:235用于计算
        this.leftPos = (this.width - 176)/2;
        this.topPos = (this.height - 235)/2;


        popButton = new ReverseButton(this.leftPos+72+18*4-5,this.topPos+6, button ->
        {
            popButton.toggleState();
            menu.popMode = !menu.popMode;
            PacketDistributor.sendToServer(new PopModeButtonPacket(menu.popMode));
        });
        addRenderableWidget(popButton);


        menu.suppressRemoteUpdates();
        BeyondDimensions.LOGGER.info("客户端发送数据请求");
        PacketDistributor.sendToServer(new CallSeverStoragePacket());
    }

    @Override
    protected void containerTick() {

        if(menu.popMode)
        {
            popButton.setState(ButtonState.DISABLED);
        }
        else
        {
            popButton.setState(ButtonState.ENABLED);
        }
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
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        popButton.render(guiGraphics,mouseX,mouseY,partialTicks);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
        this.renderEnergyBar(guiGraphics,this.leftPos+8,this.topPos + 35);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY)
    {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY+20, 4210752);

        guiGraphics.drawString(this.font, StringFormat.formatCount(menu.energyStored)+"/"+StringFormat.formatCount(menu.energyCapacity), this.inventoryLabelX, this.inventoryLabelY-20, 4210752);
    }

    @Override
    protected void renderSlot(GuiGraphics guiGraphics, Slot slot)
    {
        super.renderSlot(guiGraphics,slot);
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int x, int y)
    {

    }

    protected void renderEnergyBar(GuiGraphics guiGraphics, int xStart, int yStart)
    {
        int areaWidth = 160;
        int areaHeight = 16;
        final int stripeWidth = 1; // 单个条纹宽度

        // 背景条纹绘制（低亮度版本）
        for (int i = 0; i < areaWidth; i += stripeWidth) {
            int color = ((i / stripeWidth) % 2 == 0) ? 0xFF400000 : 0xFF200000; // 暗红交替
            int width = Math.min(stripeWidth, areaWidth - i);
            guiGraphics.fill(xStart + i, yStart,
                    xStart + i + width, yStart + areaHeight,
                    color);
        }
        // 计算能量填充比例
        float energyRatio = (float) menu.energyStored / menu.energyCapacity;
        int filledWidth = (int)(energyRatio * areaWidth);

        // 前景条纹绘制（亮色版本）
        for (int i = 0; i < filledWidth; i += stripeWidth)
        {
            int color = ((i / stripeWidth) % 2 == 0) ? 0xFFFF0000 : 0xFF800000; // 红/暗红交替
            int drawWidth = Math.min(stripeWidth, filledWidth - i);
            guiGraphics.fill(xStart + i, yStart,
                    xStart + i + drawWidth, yStart + areaHeight,
                    color);

        }
    }

    public Font getFont() {
        return font;
    }

}
