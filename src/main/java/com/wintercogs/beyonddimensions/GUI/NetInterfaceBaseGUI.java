package com.wintercogs.beyonddimensions.GUI;

import com.mojang.blaze3d.systems.RenderSystem;
import com.wintercogs.beyonddimensions.Api.DataBase.ButtonName;
import com.wintercogs.beyonddimensions.Api.DataBase.ButtonState;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.IStackType;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.ItemStackType;
import com.wintercogs.beyonddimensions.Api.Registry.StackTypeRegistry;
import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.Config;
import com.wintercogs.beyonddimensions.GUI.SharedWidget.StatusButton;
import com.wintercogs.beyonddimensions.Integration.AE.AEHelper;
import com.wintercogs.beyonddimensions.Integration.EMI.SlotHandler.SlotDragHandler;
import com.wintercogs.beyonddimensions.Menu.NetInterfaceBaseMenu;
import com.wintercogs.beyonddimensions.Menu.Slot.StoredStackSlot;
import com.wintercogs.beyonddimensions.Packet.FlagSlotSetPacket;
import com.wintercogs.beyonddimensions.Packet.PopModeButtonPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;


public class NetInterfaceBaseGUI extends BDBaseGUI<NetInterfaceBaseMenu>
{

    public StatusButton popButton; // 使用倒序按钮来临时替代弹出模式

    private SlotDragHandler dragHandler; // 仅在emi加载时可用

    private dev.emi.emi.api.stack.EmiIngredient dragIngredient; // 仅在emi加载时可用
    private boolean isDragging = false;


    public NetInterfaceBaseGUI(NetInterfaceBaseMenu container, Inventory playerInventory, Component title)
    {
        super(container, playerInventory, title);
        // 去除空白的真实部分，用于计算图片显示的最佳位置

    }



    @Override
    protected void init() {
        // 如果以后图片大小有变，显示中心所期望的大小仍然是x:176,y:235用于计算
        this.imageWidth = 176;
        this.imageHeight = rebuildImageHeight();
        rebuildLabelHeight();
        this.leftPos = (this.width - imageWidth)/2;
        this.topPos = (this.height - imageHeight)/2;

        // 初始化emi dragHandler
        if(BeyondDimensions.EMILoaded)
        {
            dragHandler = new SlotDragHandler(
                slot -> {
                    if(slot instanceof StoredStackSlot sSlot)
                    {
                        return sSlot.isFake();
                    }
                    return false;
                },

                (slot, ingredient) -> {
                    // stackKey 是如 Item Fluid的类
                    Object stackKey = ingredient.getEmiStacks().get(0).getKey();
                    DataComponentPatch dataComponentPatch = ingredient.getEmiStacks().get(0).getComponentChanges();

                    IStackType dragging = new ItemStackType();
                    for(IStackType type : StackTypeRegistry.getAllTypes())
                    {
                        if(type.getSourceClass().isAssignableFrom(stackKey.getClass()))
                        {

                            dragging = type.fromObject(stackKey,1,dataComponentPatch);
                            break;

                        }
                    }

                    // AE2通用包裹支持
                    if(BeyondDimensions.AELoaded)
                    {
                        if(dragging instanceof ItemStackType draggingItem && !dragging.isEmpty())
                        {
                            appeng.api.stacks.GenericStack genericContent = appeng.api.stacks.GenericStack.fromItemStack(draggingItem.getStack());

                            if(genericContent != null)
                            {
                                dragging = AEHelper.fromAEKeyToIStack(genericContent.what(), 1).orElse(new ItemStackType());
                            }

                        }
                    }


                    StoredStackSlot sSlot = (StoredStackSlot) slot;
                    IStackType clickItem = sSlot.getVanillaActualStack();
                    // button的数字0代表左键
                    PacketDistributor.sendToServer(new FlagSlotSetPacket(sSlot.index,clickItem,dragging));
                }

            );
        }


        popButton = new StatusButton(this.leftPos+72+18*4-5,this.topPos+6,16,16, ButtonName.ReverseButton, button ->
        {
            popButton.toggleState();
            menu.popMode = !menu.popMode;
            PacketDistributor.sendToServer(new PopModeButtonPacket(menu.popMode));
        })
        {
            @Override
            protected void initButton()
            {
                iconMap.put(ButtonState.ENABLED, ResourceLocation.tryBuild(BeyondDimensions.MODID,"widget/sort_asc"));
                iconMap.put(ButtonState.DISABLED,ResourceLocation.tryBuild(BeyondDimensions.MODID,"widget/sort_desc"));

                tooltipMap.put(ButtonState.ENABLED, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.popmode_on")));
                tooltipMap.put(ButtonState.DISABLED, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.popmode_off")));


                for(ButtonState state : iconMap.keySet())
                {
                    this.states.add(state);
                }
                setState(Config.uiReverseButton);
            }
        };
        addRenderableWidget(popButton);

    }

    protected int rebuildImageHeight()
    {
        return CommonTextures.TOP_BASE_COMMON_HEIGHT + CommonTextures.COMMON_SLOTS_HEIGHT*3 + CommonTextures.FILTER_SLOTS_HEIGHT*3+CommonTextures.COMMON_CONNECTION_HEIGHT + CommonTextures.PLAYER_INV_HEIGHT;
    }

    protected void rebuildLabelHeight()
    {
        this.titleLabelY = 8;
        this.inventoryLabelY = CommonTextures.TOP_BASE_COMMON_HEIGHT + CommonTextures.COMMON_SLOTS_HEIGHT*3 + CommonTextures.FILTER_SLOTS_HEIGHT*3+4;
    }

    @Override
    protected void containerTick()
    {
        super.containerTick();
        //父类无操作
        //每tick自动更新搜索方案

        if(menu.popMode)
        {
            popButton.setState(ButtonState.ENABLED);
        }
        else
        {
            popButton.setState(ButtonState.DISABLED);
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY)
    {
        int[] drawY = new int[]{this.topPos}; // 用于动态控制绘制
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        CommonTexturesRender.renderTopBaseCommon(guiGraphics,this.leftPos,drawY);
        CommonTexturesRender.renderFilterSlots(guiGraphics,this.leftPos,drawY);
        CommonTexturesRender.renderCommonSlots(guiGraphics,this.leftPos,drawY);
        CommonTexturesRender.renderFilterSlots(guiGraphics,this.leftPos,drawY);
        CommonTexturesRender.renderCommonSlots(guiGraphics,this.leftPos,drawY);
        CommonTexturesRender.renderFilterSlots(guiGraphics,this.leftPos,drawY);
        CommonTexturesRender.renderCommonSlots(guiGraphics,this.leftPos,drawY);
        CommonTexturesRender.renderCommonConnection(guiGraphics,this.leftPos,drawY);
        CommonTexturesRender.renderPlayerInv(guiGraphics,this.leftPos,drawY);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks)
    {
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        popButton.render(guiGraphics,mouseX,mouseY,partialTicks);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY)
    {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752,false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752,false);
    }


    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        super.mouseDragged(mouseX,mouseY,button,dragX,dragY);

        // 获取拖动物品
        if(BeyondDimensions.EMILoaded && !isDragging)
        {
            dragIngredient = dev.emi.emi.api.EmiApi.getHoveredStack((int) mouseX, (int) mouseY,true).getStack();
            if(dragIngredient != null)
                isDragging = true;
        }

        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button)
    {
        super.mouseReleased(mouseX, mouseY, button);
        if(BeyondDimensions.EMILoaded)
        {
            if(dragIngredient != null)
                this.dragHandler.dropStack(this, dragIngredient,(int)mouseX,(int)mouseY);
            dragIngredient = null;
        }
        isDragging = false;
        return true;
    }

}
