package com.wintercogs.beyonddimensions.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.client.gui.widget.shared.RightTabButton;
import com.wintercogs.beyonddimensions.common.machine.PopMode;
import com.wintercogs.beyonddimensions.common.machine.RedStoneControlMode;
import com.wintercogs.beyonddimensions.common.menu.NetEnergyMenu;
import com.wintercogs.beyonddimensions.util.StringFormat;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

public class NetEnergyGUI extends BDBaseGUI<NetEnergyMenu>
{
    private static final ResourceLocation GUI_TEXTURE = ResourceLocation.tryParse("beyonddimensions:textures/gui/net_energy_storage.png");

    public RightTabButton popButton; // 弹出模式
    public RightTabButton controlModeButton; // 红石控制模式按钮


    public NetEnergyGUI(NetEnergyMenu container, Inventory playerInventory, Component title)
    {
        super(container, playerInventory, title);
        // 去除空白的真实部分，用于计算图片显示的最佳位置
    }


    @Override
    protected void init()
    {
        super.init();

        // 如果以后图片大小有变，显示中心所期望的大小仍然是x:176,y:235用于计算
        this.imageWidth = 176;
        this.imageHeight = 175;
        this.leftPos = (this.width - imageWidth) / 2;
        this.topPos = (this.height - imageHeight) / 2;


        popButton = new RightTabButton(this.leftPos + 176, this.topPos + 6, 23, 26,
                this.leftPos + 176 + 3, this.topPos + 6 + 4, 16, 16, button ->
        {
            popButton.toggleState();
            menu.be.setPopMode((PopMode) popButton.currentState);
            menu.writeAndSendQuickData();
        })
        {
            @Override
            protected void initButton()
            {
                iconMap.put(PopMode.OPEN, ResourceLocation.tryBuild(BDConstants.MODID, "textures/gui/sprites/widget/popmode_up.png"));
                iconMap.put(PopMode.STOP, ResourceLocation.tryBuild(BDConstants.MODID, "textures/gui/sprites/widget/popmode_down.png"));

                tooltipMap.put(PopMode.OPEN, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.popmode_on")));
                tooltipMap.put(PopMode.STOP, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.popmode_off")));


                for (Enum<?> state : iconMap.keySet())
                {
                    this.states.add(state);
                }

                setState(menu.be.getPopMode());
            }
        };
        addRenderableWidget(popButton);

        controlModeButton = new RightTabButton(leftPos + 176, topPos + 36, 23, 26,
                leftPos + 176 + 3, topPos + 36 + 4, 16, 16, button -> {
            controlModeButton.toggleState();
            menu.be.controlMode = (RedStoneControlMode) controlModeButton.currentState;
            menu.writeAndSendQuickData();
        })
        {
            @Override
            protected void initButton()
            {
                iconMap.put(RedStoneControlMode.IGNORE, ResourceLocation.tryBuild(BDConstants.MODID, "textures/gui/sprites/widget/control_mode_ignore.png"));
                iconMap.put(RedStoneControlMode.NOT_WORKING, ResourceLocation.tryBuild(BDConstants.MODID, "textures/gui/sprites/widget/control_mode_not_working.png"));
                iconMap.put(RedStoneControlMode.POWERED, ResourceLocation.tryBuild(BDConstants.MODID, "textures/gui/sprites/widget/control_mode_powered.png"));
                iconMap.put(RedStoneControlMode.UNPOWERED, ResourceLocation.tryBuild(BDConstants.MODID, "textures/gui/sprites/widget/control_mode_unpowered.png"));


                tooltipMap.put(RedStoneControlMode.IGNORE, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.control_mode_ignore")));
                tooltipMap.put(RedStoneControlMode.NOT_WORKING, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.control_mode_not_working")));
                tooltipMap.put(RedStoneControlMode.POWERED, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.control_mode_powered")));
                tooltipMap.put(RedStoneControlMode.UNPOWERED, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.control_mode_unpowered")));


                for (Enum<?> state : iconMap.keySet())
                {
                    this.states.add(state);
                }

                setState(menu.be.controlMode);
            }
        };
        addRenderableWidget(controlModeButton);
    }

    @Override
    protected void containerTick()
    {
        super.containerTick();

        if (popButton.currentState != menu.be.getPopMode())
            popButton.setState(menu.be.getPopMode());
        if (controlModeButton.currentState != menu.be.controlMode)
            controlModeButton.setState(menu.be.controlMode);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY)
    {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, GUI_TEXTURE);
        guiGraphics.blit(GUI_TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks)
    {
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        popButton.render(guiGraphics, mouseX, mouseY, partialTicks);
        this.renderEnergyBar(guiGraphics, this.leftPos + 8, this.topPos + 35);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY)
    {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY + 10, 4210752, false);

        guiGraphics.drawString(this.font, StringFormat.formatCount(menu.lastEnergyStored) + "/" + StringFormat.formatCount(menu.lastEnergyCapacity), this.inventoryLabelX, this.inventoryLabelY - 20, 4210752, false);
        guiGraphics.drawString(this.font, StringFormat.formatChange(menu.lastEnergySpeedState) + " FE/t", this.inventoryLabelX, this.inventoryLabelY - 10, 4210752, false);
    }

    protected void renderEnergyBar(GuiGraphics guiGraphics, int xStart, int yStart)
    {
        int areaWidth = 160;
        int areaHeight = 16;
        final int stripeWidth = 1;

        // 预计算每行的亮度系数（使用二次曲线实现平滑衰减）
        float[] brightnessFactors = new float[areaHeight];
        for (int y = 0; y < areaHeight; y++)
        {
            float normalizedY = (y - areaHeight / 2.0f) / (areaHeight / 2.0f);
            brightnessFactors[y] = 1.0f - normalizedY * normalizedY;
        }

        // 背景绘制保持不变
        for (int i = 0; i < areaWidth; i += stripeWidth)
        {
            int color = ((i / stripeWidth) % 2 == 0) ? 0xFF400000 : 0xFF200000;
            int width = Math.min(stripeWidth, areaWidth - i);
            guiGraphics.fill(xStart + i, yStart,
                    xStart + i + width, yStart + areaHeight,
                    color);
        }

        float energyRatio = (float) menu.lastEnergyStored / menu.lastEnergyCapacity;
        int filledWidth = (int) (energyRatio * areaWidth);

        // 前景绘制添加垂直渐变效果
        for (int i = 0; i < filledWidth; i += stripeWidth)
        {
            int baseColor = ((i / stripeWidth) % 2 == 0) ? 0xFFFF0000 : 0xFF800000;
            int drawWidth = Math.min(stripeWidth, filledWidth - i);

            // 分解颜色通道
            int alpha = (baseColor >> 24) & 0xFF;
            int red = (baseColor >> 16) & 0xFF;
            int green = (baseColor >> 8) & 0xFF;
            int blue = baseColor & 0xFF;

            // 逐行绘制带亮度变化的条纹
            for (int y = 0; y < areaHeight; y++)
            {
                // 应用亮度系数并重新组合颜色
                int adjustedAlpha = (int) (alpha * brightnessFactors[y]);
                int adjustedColor = (adjustedAlpha << 24) | (red << 16) | (green << 8) | blue;

                guiGraphics.fill(xStart + i, yStart + y,
                        xStart + i + drawWidth, yStart + y + 1,
                        adjustedColor);
            }
        }
    }


    public Font getFont()
    {
        return font;
    }

}
