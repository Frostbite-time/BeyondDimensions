package com.wintercogs.beyonddimensions.GUI;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.wintercogs.beyonddimensions.DataBase.ButtonName;
import com.wintercogs.beyonddimensions.DataBase.ButtonState;
import com.wintercogs.beyonddimensions.GUI.Widget.Button.ReverseButton;
import com.wintercogs.beyonddimensions.GUI.Widget.Button.SortMethodButton;
import com.wintercogs.beyonddimensions.GUI.Widget.Scroller.BigScroller;
import com.wintercogs.beyonddimensions.Menu.DimensionsCraftMenu;
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

public class DimensionsCraftGUI extends DimensionsNetGUI<DimensionsCraftMenu>
{

    private static final ResourceLocation GUI_TEXTURE_CRAFT_SLOTS = ResourceLocation.tryParse("beyonddimensions:textures/gui/craft_slots.png");
    private static final int CRAFT_SLOTS_WIDTH = 176;
    private static final int CRAFT_SLOTS_HEIGHT = 62;

    public DimensionsCraftGUI(DimensionsCraftMenu container, Inventory playerInventory, Component title)
    {
        super(container, playerInventory, title);
    }



    @Override
    protected void init()
    {
        super.init();
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

        RenderSystem.setShaderTexture(0, GUI_TEXTURE_CRAFT_SLOTS);
        guiGraphics.blit(GUI_TEXTURE_CRAFT_SLOTS, this.leftPos, drawY, 0, 0, CRAFT_SLOTS_WIDTH, CRAFT_SLOTS_HEIGHT, CRAFT_SLOTS_WIDTH, CRAFT_SLOTS_HEIGHT);
        drawY += CRAFT_SLOTS_HEIGHT;

        RenderSystem.setShaderTexture(0, GUI_TEXTURE_PLAYER_INV);
        guiGraphics.blit(GUI_TEXTURE_PLAYER_INV, this.leftPos, drawY, 0, 0, PLAYER_INV_WIDTH, PLAYER_INV_HEIGHT, PLAYER_INV_WIDTH, PLAYER_INV_HEIGHT);
        //drawY += PLAYER_INV_HEIGHT;
    }

    @Override
    protected int rebuildImageHeight()
    {
        return TOP_BASE_HEIGHT + TOP_SLOTS_HEIGHT + (menu.getLines()-2) * MID_SLOTS_HEIGHT + BOTTOM_SLOTS_HEIGHT + CRAFT_SLOTS_HEIGHT + PLAYER_INV_HEIGHT;
    }

    @Override
    protected void rebuildLabelHeight()
    {
        this.titleLabelY = 8;
        this.inventoryLabelY = TOP_BASE_HEIGHT + menu.getLines()*18+5 + CRAFT_SLOTS_HEIGHT;
    }

    @Override
    protected int calMaxLines()
    {
        return (int)((this.height -36 - (TOP_BASE_HEIGHT+TOP_SLOTS_HEIGHT+BOTTOM_SLOTS_HEIGHT+CRAFT_SLOTS_HEIGHT+PLAYER_INV_HEIGHT))/(float)MID_SLOTS_HEIGHT +2);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks)
    {
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

}