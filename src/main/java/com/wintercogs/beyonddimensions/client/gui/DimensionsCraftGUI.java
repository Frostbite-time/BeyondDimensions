package com.wintercogs.beyonddimensions.client.gui;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.Config;
import com.wintercogs.beyonddimensions.api.ButtonState;
import com.wintercogs.beyonddimensions.client.gui.widget.shared.IconButton;
import com.wintercogs.beyonddimensions.client.gui.widget.shared.StatusButton;
import com.wintercogs.beyonddimensions.common.menu.DimensionsCraftMenu;
import com.wintercogs.beyonddimensions.config.CommonConfigRuntime;
import com.wintercogs.beyonddimensions.network.packet.c2s.ClickTransferCraftButtonPacket;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jetbrains.annotations.NotNull;


public class DimensionsCraftGUI<T extends DimensionsCraftMenu> extends DimensionsNetGUI<T>
{

    private static final Identifier GUI_TEXTURE_CRAFT_SLOTS = Identifier.parse("beyonddimensions:textures/gui/craft_slots.png");
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
        transferCraftToInvButton = new IconButton(this.leftPos + 90, this.topPos + TOP_BASE_HEIGHT + menu.getLines() * 18 + 10, 8, 8, BeyondDimensions.makeId("widget/down_arrow"), button ->
        {
            ClientPacketDistributor.sendToServer(new ClickTransferCraftButtonPacket(false));
        });
        transferCraftToInvButton.setTooltip(Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.transfer_to_inv")));
        addRenderableWidget(transferCraftToInvButton);


        transferCraftToStorageButton = new IconButton(this.leftPos + 81, this.topPos + TOP_BASE_HEIGHT + menu.getLines() * 18 + 10, 8, 8, BeyondDimensions.makeId("widget/up_arrow"), button ->
        {
            ClientPacketDistributor.sendToServer(new ClickTransferCraftButtonPacket(true));
        });
        transferCraftToStorageButton.setTooltip(Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.transfer_to_storage")));
        addRenderableWidget(transferCraftToStorageButton);

        // 槽位优先转移切换按钮
        menu.writeAndSendQuickData();
        craftReturnButton = new StatusButton(this.leftPos + 99, this.topPos + TOP_BASE_HEIGHT + menu.getLines() * 18 + 10, 8, 8, button -> {
            craftReturnButton.toggleState();
            CommonConfigRuntime.uiCraftReturnButton = (ButtonState) craftReturnButton.currentState;
            Config.INSTANCE.commonConfig.UI_CRAFT_RETURN_BUTTON.set((ButtonState) craftReturnButton.currentState);
            Config.INSTANCE.commonConfig.UI_CRAFT_RETURN_BUTTON.save();
            menu.writeAndSendQuickData();
        })
        {

            @Override
            protected void initButton()
            {
                iconMap.put(ButtonState.ENABLED, BeyondDimensions.makeId("widget/sort_asc"));
                iconMap.put(ButtonState.DISABLED, BeyondDimensions.makeId("widget/sort_desc"));

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
    protected void extractMenuBackground(@NotNull GuiGraphicsExtractor guiGraphics)
    {
        int drawY = this.topPos; // 用于动态控制绘制
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE_TOP_BASE, this.leftPos, drawY, 0F, 0F, TOP_BASE_WIDTH, TOP_BASE_HEIGHT, TOP_BASE_WIDTH, TOP_BASE_HEIGHT);
        drawY += TOP_BASE_HEIGHT;

        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE_TOP_SLOTS, this.leftPos, drawY, 0F, 0F, TOP_SLOTS_WIDTH, TOP_SLOTS_HEIGHT, TOP_SLOTS_WIDTH, TOP_SLOTS_HEIGHT);
        drawY += TOP_SLOTS_HEIGHT;

        for (int i = 0; i < menu.getLines() - 2; i++)
        {
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE_MID_SLOTS, this.leftPos, drawY, 0F, 0F, MID_SLOTS_WIDTH, MID_SLOTS_HEIGHT, MID_SLOTS_WIDTH, MID_SLOTS_HEIGHT);
            drawY += MID_SLOTS_HEIGHT;
        }

        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE_BOTTOM_SLOTS, this.leftPos, drawY, 0F, 0F, BOTTOM_SLOTS_WIDTH, BOTTOM_SLOTS_HEIGHT, BOTTOM_SLOTS_WIDTH, BOTTOM_SLOTS_HEIGHT);
        drawY += BOTTOM_SLOTS_HEIGHT;

        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE_CRAFT_SLOTS, this.leftPos, drawY, 0F, 0F, CRAFT_SLOTS_WIDTH, CRAFT_SLOTS_HEIGHT, CRAFT_SLOTS_WIDTH, CRAFT_SLOTS_HEIGHT);
        drawY += CRAFT_SLOTS_HEIGHT;

        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE_PLAYER_INV, this.leftPos, drawY, 0F, 0F, PLAYER_INV_WIDTH, PLAYER_INV_HEIGHT, PLAYER_INV_WIDTH, PLAYER_INV_HEIGHT);
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

}
