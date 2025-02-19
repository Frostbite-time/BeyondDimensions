package com.wintercogs.beyonddimensions.GUI;

import com.mojang.blaze3d.systems.RenderSystem;
import com.wintercogs.beyonddimensions.Menu.NetControlMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class NetControlGUI extends AbstractContainerScreen<NetControlMenu>
{

    private static final ResourceLocation GUI_TEXTURE = ResourceLocation.parse("beyonddimensions:textures/gui/net_control.png");

    public NetControlGUI(NetControlMenu menu, Inventory playerInventory, Component title)
    {
        super(menu, playerInventory, title);

        // 去除空白的真实部分，用于计算图片显示的最佳位置
        this.imageWidth = 256;
        this.imageHeight = 256;
    }

    @Override
    protected void init() {
        this.leftPos = (this.width - 256)/2;
        this.topPos = (this.height - 256)/2;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY)
    {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, GUI_TEXTURE);
        guiGraphics.blit(GUI_TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
    }
}
