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
    private static final ResourceLocation GUI_TEXTURE = ResourceLocation.parse("beyonddimensions:textures/gui/net_interface.png");

    public ReverseButton popButton; // 使用倒序按钮来临时替代弹出模式


    public NetEnergyGUI(NetEnergyMenu container, Inventory playerInventory, Component title)
    {
        super(container, playerInventory, title);
        // 去除空白的真实部分，用于计算图片显示的最佳位置
        this.imageWidth = 176;
        this.imageHeight = 207;
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

    public Font getFont() {
        return font;
    }

}
