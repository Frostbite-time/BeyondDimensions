package com.wintercogs.beyonddimensions.GUI;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import com.wintercogs.beyonddimensions.DataBase.NetControlAction;
import com.wintercogs.beyonddimensions.DataBase.NetPermissionlevel;
import com.wintercogs.beyonddimensions.DataBase.PlayerPermissionInfo;
import com.wintercogs.beyonddimensions.GUI.Widget.Button.PermissionInfoButton;
import com.wintercogs.beyonddimensions.Menu.NetControlMenu;
import com.wintercogs.beyonddimensions.Packet.CallServerPlayerInfoPacket;
import com.wintercogs.beyonddimensions.Packet.NetControlActionPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.UUID;

public class NetControlGUI extends AbstractContainerScreen<NetControlMenu>
{
    private static final Logger LOGGER = LogUtils.getLogger();

    private ArrayList<PermissionInfoButton> permissionInfoButtons = new ArrayList<>();
    private UUID currentPlayerId = null;
    private String currentPlayerName = "";
    private NetPermissionlevel currentPlayerPermissionLevel = null;

    private Button ownerButton;
    private Button managerButton;
    private Button removeManagerButton;
    private Button removeMemberButton;

    private final int maxShowPlayers = 20;
    private int nowShowPlayer = 0;
    private int nowTopShowPlayer = 0;

    private static final ResourceLocation GUI_TEXTURE = ResourceLocation.parse("beyonddimensions:textures/gui/net_control.png");

    public NetControlGUI(NetControlMenu menu, Inventory playerInventory, Component title)
    {
        super(menu, playerInventory, title);

        // 去除空白的真实部分，用于计算图片显示的最佳位置
        this.imageWidth = 256;
        this.imageHeight = 235;
    }

    private void updatePlayerWidget()
    {
        ArrayList<PermissionInfoButton> cacheList = new ArrayList<>();
        for (Map.Entry<UUID, PlayerPermissionInfo> entry : menu.playerInfo.entrySet()) {
            UUID key = entry.getKey();
            PlayerPermissionInfo value = entry.getValue();

            cacheList.add(new PermissionInfoButton(0,0,84,10, key, value, Component.literal("test"),button -> {
                PermissionInfoButton permissionInfoButton = (PermissionInfoButton) button;
                currentPlayerId = permissionInfoButton.getPlayerId();
                currentPlayerName = permissionInfoButton.getPermissionInfo().name();
                currentPlayerPermissionLevel = permissionInfoButton.getPermissionInfo().level();
            }));
        }
        cacheList.sort(
                Comparator.comparing((PermissionInfoButton button) -> button.getPermissionInfo().level()).thenComparing(
                        button -> button.getPermissionInfo().name()
                )
        );
        nowShowPlayer = 0;
        for(PermissionInfoButton button:cacheList)
        {
            button.setX(leftPos+11);
            button.setY(topPos+18+(nowShowPlayer-nowTopShowPlayer)*10);
            button.setMessage(Component.literal(button.getPermissionInfo().name()));
            nowShowPlayer++;
            if(nowShowPlayer-nowTopShowPlayer >= maxShowPlayers)
            {
                break;
            }
        }
        for (PermissionInfoButton button:permissionInfoButtons)
        {
            removeWidget(button);
        }
        permissionInfoButtons = cacheList;
        nowShowPlayer = 0;
        for (PermissionInfoButton button:permissionInfoButtons)
        {
            if(nowShowPlayer-nowTopShowPlayer<0)
            {
                nowShowPlayer++;
                continue;
            }
            addRenderableWidget(button);
            nowShowPlayer++;
            if(nowShowPlayer-nowTopShowPlayer >= maxShowPlayers)
            {
                break;
            }
        }
    }

    @Override
    protected void init() {
        this.leftPos = (this.width - 256)/2;
        this.topPos = (this.height - 235)/2;

        PacketDistributor.sendToServer(new CallServerPlayerInfoPacket());

        ownerButton = Button.builder(
                Component.literal("移交所有权"),
                button -> {
                    if(currentPlayerId != null)
                    {
                        PacketDistributor.sendToServer(new NetControlActionPacket(currentPlayerId, NetControlAction.SetOwner));
                    }
                }
        ).pos(leftPos+110,topPos+60).size(80,15).build();
        addRenderableWidget(ownerButton);

        managerButton = Button.builder(
                Component.literal("设为管理员"),
                button -> {
                    if(currentPlayerId != null)
                    {
                        PacketDistributor.sendToServer(new NetControlActionPacket(currentPlayerId, NetControlAction.SetManager));
                    }
                }
        ).pos(leftPos+110,topPos+60+25).size(80,15).build();
        addRenderableWidget(managerButton);

        removeManagerButton = Button.builder(
                Component.literal("移除管理员权限"),
                button -> {
                    if(currentPlayerId != null)
                    {
                        PacketDistributor.sendToServer(new NetControlActionPacket(currentPlayerId, NetControlAction.RemoveManager));
                    }
                }
        ).pos(leftPos+110,topPos+60+50).size(80,15).build();
        addRenderableWidget(removeManagerButton);

        removeMemberButton = Button.builder(
                Component.literal("移除成员"),
                button -> {
                    if(currentPlayerId != null)
                    {
                        PacketDistributor.sendToServer(new NetControlActionPacket(currentPlayerId, NetControlAction.RemovePlayer));
                    }
                }
        ).pos(leftPos+110,topPos+60+75).size(80,15).build();
        addRenderableWidget(removeMemberButton);
    }

    @Override
    protected void containerTick()
    {
        updatePlayerWidget();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY)
    {
        super.mouseScrolled(mouseX,mouseY,scrollX,scrollY);
        if (scrollY > 0)
        {
            nowTopShowPlayer--;
        } else if(scrollY < 0)
        {
            nowTopShowPlayer++;
        }
        if(permissionInfoButtons.size()- maxShowPlayers<=nowTopShowPlayer)
        {
            nowTopShowPlayer = permissionInfoButtons.size()- maxShowPlayers;
        }
        if(nowTopShowPlayer<0)
        {
            nowTopShowPlayer = 0;
        }
        updatePlayerWidget();
        return true;
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
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTicks);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
        nowShowPlayer = 0;
        for (PermissionInfoButton button:permissionInfoButtons)
        {
            if(nowShowPlayer-nowTopShowPlayer<0)
            {
                nowShowPlayer++;
                continue;
            }
            button.render(guiGraphics,mouseX,mouseY,partialTicks);
            nowShowPlayer++;
            if(nowShowPlayer-nowTopShowPlayer >= maxShowPlayers)
            {
                break;
            }
        }
        ownerButton.render(guiGraphics,mouseX,mouseY,partialTicks);
        managerButton.render(guiGraphics,mouseX,mouseY,partialTicks);
        removeManagerButton.render(guiGraphics,mouseX,mouseY,partialTicks);
        removeMemberButton.render(guiGraphics,mouseX,mouseY,partialTicks);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY)
    {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX+3, this.titleLabelY, 4210752);
        guiGraphics.drawString(this.font, Component.literal("名称: "+currentPlayerName), 110, 25, 4210752);
        if(currentPlayerPermissionLevel == null)
        {
            guiGraphics.drawString(this.font, Component.literal("权限级别: 无"), 110, 10, 4210752);
        }
        else {
            guiGraphics.drawString(this.font, Component.literal("权限级别: " + currentPlayerPermissionLevel.name()), 110, 10, 4210752);
        }


    }
}
