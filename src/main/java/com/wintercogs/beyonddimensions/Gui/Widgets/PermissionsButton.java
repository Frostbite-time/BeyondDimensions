package com.wintercogs.beyonddimensions.Gui.Widgets;

import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.wintercogs.beyonddimensions.DataBase.PlayerPermissionInfo;

public class PermissionsButton extends ButtonWidget<PermissionsButton>
{
    public PlayerPermissionInfo permission;

    public PermissionsButton(PlayerPermissionInfo info)
    {
        this.permission = info;
    }
}
