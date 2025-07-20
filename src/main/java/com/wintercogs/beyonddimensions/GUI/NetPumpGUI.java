package com.wintercogs.beyonddimensions.GUI;

import com.mojang.blaze3d.systems.RenderSystem;
import com.wintercogs.beyonddimensions.Menu.NetPumpMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class NetPumpGUI extends BDBaseGUI<NetPumpMenu>
{
    public NetPumpGUI(NetPumpMenu menu, Inventory playerInventory, Component title)
    {
        super(menu, playerInventory, title);
    }

    @Override
    protected void init()
    {
        this.imageWidth = 176;
        this.imageHeight = rebuildImageHeight();
        rebuildLabelHeight();
        this.leftPos = (this.width - imageWidth)/2;
        this.topPos = (this.height - imageHeight)/2;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float v, int i, int i1)
    {
        int[] drawY = new int[]{this.topPos}; // 用于动态控制绘制
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        CommonTexturesRender.renderTopBaseCommon(guiGraphics,this.leftPos,drawY);
        CommonTexturesRender.renderFilterSlots(guiGraphics,this.leftPos,drawY);
        CommonTexturesRender.renderFilterSlots(guiGraphics,this.leftPos,drawY);
        CommonTexturesRender.renderFilterSlots(guiGraphics,this.leftPos,drawY);
        CommonTexturesRender.renderFilterSlots(guiGraphics,this.leftPos,drawY);
        CommonTexturesRender.renderCommonConnection(guiGraphics,this.leftPos,drawY);
        CommonTexturesRender.renderPlayerInv(guiGraphics,this.leftPos,drawY);
    }

    protected int rebuildImageHeight()
    {
        return CommonTextures.TOP_BASE_COMMON_HEIGHT + CommonTextures.FILTER_SLOTS_HEIGHT*4 + CommonTextures.COMMON_CONNECTION_HEIGHT + CommonTextures.PLAYER_INV_HEIGHT;
    }

    protected void rebuildLabelHeight()
    {
        this.titleLabelY = 8;
        this.inventoryLabelY = CommonTextures.TOP_BASE_COMMON_HEIGHT + CommonTextures.FILTER_SLOTS_HEIGHT*4+4;
    }
}
