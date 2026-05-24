package com.wintercogs.beyonddimensions.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.wintercogs.beyonddimensions.Config;
import com.wintercogs.beyonddimensions.api.ButtonState;
import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.client.gui.widget.shared.IconButton;
import com.wintercogs.beyonddimensions.client.gui.widget.shared.StatusButton;
import com.wintercogs.beyonddimensions.common.init.BDPackets;
import com.wintercogs.beyonddimensions.common.menu.DimensionsCraftMenu;
import com.wintercogs.beyonddimensions.config.CommonConfigRuntime;
import com.wintercogs.beyonddimensions.network.packet.c2s.ClickTransferCraftButtonPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;


public class DimensionsCraftGUI<T extends DimensionsCraftMenu> extends DimensionsNetGUI<T>
{

    private static final ResourceLocation GUI_TEXTURE_CRAFT_SLOTS = ResourceLocation.tryParse("beyonddimensions:textures/gui/craft_slots.png");
    private static final int CRAFT_SLOTS_WIDTH = 176;
    private static final int CRAFT_SLOTS_HEIGHT = 62;

    private IconButton transferCraftToInvButton;
    private IconButton transferCraftToStorageButton;
    protected StatusButton craftReturnButton;

    public DimensionsCraftGUI(T container, Inventory playerInventory, Component title)
    {
        super(container, playerInventory, title);
    }


    @Override
    protected void init()
    {
        super.init();

        //槽位转移按钮
        transferCraftToInvButton = new IconButton(this.leftPos + 90, this.topPos + TOP_BASE_HEIGHT + menu.getLines() * 18 + 10, 8, 8, ResourceLocation.tryBuild(BDConstants.MODID, "textures/gui/sprites/widget/down_arrow.png"), button ->
        {
            BDPackets.INSTANCE.sendToServer(new ClickTransferCraftButtonPacket(false));
        });
        transferCraftToInvButton.setTooltip(Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.transfer_to_inv")));
        addRenderableWidget(transferCraftToInvButton);


        transferCraftToStorageButton = new IconButton(this.leftPos + 81, this.topPos + TOP_BASE_HEIGHT + menu.getLines() * 18 + 10, 8, 8, ResourceLocation.tryBuild(BDConstants.MODID, "textures/gui/sprites/widget/up_arrow.png"), button ->
        {
            BDPackets.INSTANCE.sendToServer(new ClickTransferCraftButtonPacket(true));
        });
        transferCraftToStorageButton.setTooltip(Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.transfer_to_storage")));
        addRenderableWidget(transferCraftToStorageButton);

        // 槽位优先转移切换按钮
        menu.writeAndSendQuickData();
        craftReturnButton = new StatusButton(this.leftPos + 99, this.topPos + TOP_BASE_HEIGHT + menu.getLines() * 18 + 10, 8, 8, button -> {
            craftReturnButton.toggleState();
            CommonConfigRuntime.uiCraftReturnButton = (ButtonState) craftReturnButton.currentState;
            Config.INSTANCE.commonConfig.UI_CRAFT_RETURN_BUTTON.set((ButtonState) craftReturnButton.currentState);
            menu.writeAndSendQuickData();
        })
        {

            @Override
            protected void initButton()
            {
                iconMap.put(ButtonState.ENABLED, ResourceLocation.tryBuild(BDConstants.MODID, "textures/gui/sprites/widget/sort_asc.png"));
                iconMap.put(ButtonState.DISABLED, ResourceLocation.tryBuild(BDConstants.MODID, "textures/gui/sprites/widget/sort_desc.png"));

                tooltipMap.put(ButtonState.ENABLED, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.first_storage")));
                tooltipMap.put(ButtonState.DISABLED, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.first_inv")));

                for (Enum<?> state : iconMap.keySet())
                {
                    this.states.add(state);
                }
                setState(CommonConfigRuntime.uiCraftReturnButton);
            }
        };
        addRenderableWidget(craftReturnButton);
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
        return TOP_BASE_HEIGHT + TOP_SLOTS_HEIGHT + (menu.getLines() - 2) * MID_SLOTS_HEIGHT + BOTTOM_SLOTS_HEIGHT + CRAFT_SLOTS_HEIGHT + PLAYER_INV_HEIGHT;
    }

    @Override
    protected void rebuildLabelHeight()
    {
        this.titleLabelY = 8;
        this.inventoryLabelY = TOP_BASE_HEIGHT + menu.getLines() * 18 + 5 + CRAFT_SLOTS_HEIGHT;
    }

    @Override
    protected int calMaxLines()
    {
        return (int) ((this.height - 36 - (TOP_BASE_HEIGHT + TOP_SLOTS_HEIGHT + BOTTOM_SLOTS_HEIGHT + CRAFT_SLOTS_HEIGHT + PLAYER_INV_HEIGHT)) / (float) MID_SLOTS_HEIGHT + 2);
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks)
    {
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

}