package com.wintercogs.beyonddimensions.GUI.Widget.Button;

import com.wintercogs.beyonddimensions.DataBase.PlayerPermissionInfo;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import java.util.UUID;

public class PermissionInfoButton extends Button
{
    private UUID playerId;
    private PlayerPermissionInfo permissionInfo;

    public PermissionInfoButton(int x, int y, int width, int height, UUID playerId, PlayerPermissionInfo playerPermissionInfo,Component message, OnPress onPress)
    {
        super(x, y, width, height, message, onPress, Button.DEFAULT_NARRATION);
        this.playerId = playerId;
        this.permissionInfo = playerPermissionInfo;
    }

    public PlayerPermissionInfo getPermissionInfo()
    {
        return this.permissionInfo;
    }

    public void setPermissionInfo(PlayerPermissionInfo permissionInfo)
    {
        this.permissionInfo = permissionInfo;
    }

    public UUID getPlayerId()
    {
        return playerId;
    }

    public void setPlayerId(UUID playerId)
    {
        this.playerId = playerId;
    }
}
