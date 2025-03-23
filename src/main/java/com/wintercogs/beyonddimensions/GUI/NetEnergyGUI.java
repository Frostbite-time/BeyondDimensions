package com.wintercogs.beyonddimensions.GUI;

import com.mojang.blaze3d.systems.RenderSystem;
import com.wintercogs.beyonddimensions.DataBase.ButtonState;
import com.wintercogs.beyonddimensions.GUI.Widget.Button.ReverseButton;
import com.wintercogs.beyonddimensions.Menu.NetEnergyMenu;
import com.wintercogs.beyonddimensions.Packet.CallSeverStoragePacket;
import com.wintercogs.beyonddimensions.Packet.PopModeButtonPacket;
import com.wintercogs.beyonddimensions.Unit.StringFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

public class NetEnergyGUI extends BDBaseGUI<NetEnergyMenu>
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
        this.font = Minecraft.getInstance().font;


        popButton = new ReverseButton(this.leftPos+72+18*4-5,this.topPos+6, button ->
        {
            popButton.toggleState();
            menu.popMode = !menu.popMode;
            PacketDistributor.sendToServer(new PopModeButtonPacket(menu.popMode));
        });
        addRenderableWidget(popButton);


        menu.suppressRemoteUpdates();
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
        this.renderEnergyBar(guiGraphics,this.leftPos+8,this.topPos + 35);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY)
    {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752,false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY+10, 4210752,false);

        guiGraphics.drawString(this.font, StringFormat.formatCount(menu.energyStored)+"/"+StringFormat.formatCount(menu.energyCapacity), this.inventoryLabelX, this.inventoryLabelY-20, 4210752,false);
    }

    protected void renderEnergyBar(GuiGraphics guiGraphics, int xStart, int yStart) {
        int areaWidth = 160;
        int areaHeight = 16;
        final int stripeWidth = 1;

        // 预计算每行的亮度系数（使用二次曲线实现平滑衰减）
        float[] brightnessFactors = new float[areaHeight];
        for (int y = 0; y < areaHeight; y++) {
            float normalizedY = (y - areaHeight / 2.0f) / (areaHeight / 2.0f);
            brightnessFactors[y] = 1.0f - normalizedY * normalizedY;
        }

        // 背景绘制保持不变
        for (int i = 0; i < areaWidth; i += stripeWidth) {
            int color = ((i / stripeWidth) % 2 == 0) ? 0xFF400000 : 0xFF200000;
            int width = Math.min(stripeWidth, areaWidth - i);
            guiGraphics.fill(xStart + i, yStart,
                    xStart + i + width, yStart + areaHeight,
                    color);
        }

        float energyRatio = (float) menu.energyStored / menu.energyCapacity;
        int filledWidth = (int)(energyRatio * areaWidth);

        // 前景绘制添加垂直渐变效果
        for (int i = 0; i < filledWidth; i += stripeWidth) {
            int baseColor = ((i / stripeWidth) % 2 == 0) ? 0xFFFF0000 : 0xFF800000;
            int drawWidth = Math.min(stripeWidth, filledWidth - i);

            // 分解颜色通道
            int alpha = (baseColor >> 24) & 0xFF;
            int red = (baseColor >> 16) & 0xFF;
            int green = (baseColor >> 8) & 0xFF;
            int blue = baseColor & 0xFF;

            // 逐行绘制带亮度变化的条纹
            for (int y = 0; y < areaHeight; y++) {
                // 应用亮度系数并重新组合颜色
                int adjustedAlpha = (int)(alpha * brightnessFactors[y]);
                int adjustedColor = (adjustedAlpha << 24) | (red << 16) | (green << 8) | blue;

                guiGraphics.fill(xStart + i, yStart + y,
                        xStart + i + drawWidth, yStart + y + 1,
                        adjustedColor);
            }
        }
    }


    public Font getFont() {
        return font;
    }

}
