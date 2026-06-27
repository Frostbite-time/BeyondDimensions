package com.wintercogs.beyonddimensions.client.gui;

import com.wintercogs.beyonddimensions.api.dimensionnet.NetControlAction;
import com.wintercogs.beyonddimensions.api.dimensionnet.NetPermissionlevel;
import com.wintercogs.beyonddimensions.api.dimensionnet.PlayerPermissionInfo;
import com.wintercogs.beyonddimensions.client.gui.widget.button.PermissionInfoButton;
import com.wintercogs.beyonddimensions.common.menu.NetControlMenu;
import com.wintercogs.beyonddimensions.network.packet.c2s.NetControlActionPacket;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.UUID;

public class NetControlGUI extends BDBaseGUI<NetControlMenu> {
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

    private static final Identifier GUI_TEXTURE = Identifier.parse("beyonddimensions:textures/gui/net_control.png");

    public NetControlGUI(NetControlMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);

        // 去除空白的真实部分，用于计算图片显示的最佳位置
        this.imageWidth = 256;
        this.imageHeight = 235;
    }

    private void updatePlayerWidget() {
        ArrayList<PermissionInfoButton> cacheList = new ArrayList<>();
        for (Map.Entry<UUID, PlayerPermissionInfo> entry : menu.playerInfo.entrySet()) {
            UUID key = entry.getKey();
            PlayerPermissionInfo value = entry.getValue();

            cacheList.add(new PermissionInfoButton(0, 0, 84, 10, key, value, Component.literal("test"), button -> {
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
        for (PermissionInfoButton button : cacheList) {
            button.setX(leftPos + 11);
            button.setY(topPos + 18 + (nowShowPlayer - nowTopShowPlayer) * 10);
            button.setMessage(Component.literal(button.getPermissionInfo().name()));
            nowShowPlayer++;
            if (nowShowPlayer - nowTopShowPlayer >= maxShowPlayers) {
                break;
            }
        }
        for (PermissionInfoButton button : permissionInfoButtons) {
            removeWidget(button);
        }
        permissionInfoButtons = cacheList;
        nowShowPlayer = 0;
        for (PermissionInfoButton button : permissionInfoButtons) {
            if (nowShowPlayer - nowTopShowPlayer < 0) {
                nowShowPlayer++;
                continue;
            }
            addRenderableWidget(button);
            nowShowPlayer++;
            if (nowShowPlayer - nowTopShowPlayer >= maxShowPlayers) {
                break;
            }
        }

        boolean flag = false;
        // 更新按钮之后，从当前id中搜索对应按钮，重新读取名称
        for (PermissionInfoButton button : permissionInfoButtons) {
            if (button.getPlayerId().equals(currentPlayerId)) {
                currentPlayerName = button.getPermissionInfo().name();
                currentPlayerPermissionLevel = button.getPermissionInfo().level();
                flag = true;
                break;
            }
        }
        if (!flag) {
            currentPlayerName = "";
            currentPlayerPermissionLevel = null; // 已做null处理
        }
    }

    @Override
    protected void init() {
        super.init();

        this.leftPos = (this.width - 256) / 2;
        this.topPos = (this.height - 235) / 2;

        ownerButton = Button.builder(
                Component.translatable("menu.button.beyonddimensions.setowner"),
                button -> {
                    if (currentPlayerId != null) {
                        ClientPacketDistributor.sendToServer(new NetControlActionPacket(currentPlayerId, NetControlAction.SetOwner));
                    }
                }
        ).pos(leftPos + 110, topPos + 60).size(100, 20).build();
        addRenderableWidget(ownerButton);

        managerButton = Button.builder(
                Component.translatable("menu.button.beyonddimensions.setmanager"),
                button -> {
                    if (currentPlayerId != null) {
                        ClientPacketDistributor.sendToServer(new NetControlActionPacket(currentPlayerId, NetControlAction.SetManager));
                    }
                }
        ).pos(leftPos + 110, topPos + 60 + 25).size(100, 20).build();
        addRenderableWidget(managerButton);

        removeManagerButton = Button.builder(
                Component.translatable("menu.button.beyonddimensions.removemanager"),
                button -> {
                    if (currentPlayerId != null) {
                        ClientPacketDistributor.sendToServer(new NetControlActionPacket(currentPlayerId, NetControlAction.RemoveManager));
                    }
                }
        ).pos(leftPos + 110, topPos + 60 + 50).size(100, 20).build();
        addRenderableWidget(removeManagerButton);

        removeMemberButton = Button.builder(
                Component.translatable("menu.button.beyonddimensions.removemember"),
                button -> {
                    if (currentPlayerId != null) {
                        ClientPacketDistributor.sendToServer(new NetControlActionPacket(currentPlayerId, NetControlAction.RemovePlayer));
                    }
                }
        ).pos(leftPos + 110, topPos + 60 + 75).size(100, 20).build();
        addRenderableWidget(removeMemberButton);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updatePlayerWidget();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        if (scrollY > 0) {
            nowTopShowPlayer--;
        } else if (scrollY < 0) {
            nowTopShowPlayer++;
        }
        if (permissionInfoButtons.size() - maxShowPlayers <= nowTopShowPlayer) {
            nowTopShowPlayer = permissionInfoButtons.size() - maxShowPlayers;
        }
        if (nowTopShowPlayer < 0) {
            nowTopShowPlayer = 0;
        }
        updatePlayerWidget();
        return true;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float a) {
        super.extractBackground(guiGraphics, mouseX, mouseY, a);
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, this.leftPos, this.topPos, 0F, 0F, this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);
    }

    @Override
    public void extractContents(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTicks) {
        super.extractContents(guiGraphics, mouseX, mouseY, partialTicks);
        nowShowPlayer = 0;
        for (PermissionInfoButton button : permissionInfoButtons) {
            if (nowShowPlayer - nowTopShowPlayer < 0) {
                nowShowPlayer++;
                continue;
            }
            button.extractRenderState(guiGraphics, mouseX, mouseY, partialTicks);
            nowShowPlayer++;
            if (nowShowPlayer - nowTopShowPlayer >= maxShowPlayers) {
                break;
            }
        }
        ownerButton.extractRenderState(guiGraphics, mouseX, mouseY, partialTicks);
        managerButton.extractRenderState(guiGraphics, mouseX, mouseY, partialTicks);
        removeManagerButton.extractRenderState(guiGraphics, mouseX, mouseY, partialTicks);
        removeMemberButton.extractRenderState(guiGraphics, mouseX, mouseY, partialTicks);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor guiGraphics, int xm, int ym) {
        guiGraphics.text(this.font, this.title, this.titleLabelX + 3, this.titleLabelY, -12566464, false);
        guiGraphics.text(this.font, Component.translatable("menu.text.beyonddimensions.name.player", Component.literal(currentPlayerName)), 110, 25, -12566464, false);
        if (currentPlayerPermissionLevel == null) {
            guiGraphics.text(this.font, Component.translatable("menu.text.beyonddimensions.permission.level.zero"), 110, 10, -12566464, false);
        } else {
            guiGraphics.text(this.font, Component.translatable("menu.text.beyonddimensions.permission.level.prefix", Component.literal(currentPlayerPermissionLevel.name())), 110, 10, -12566464, false);
        }
    }
}
