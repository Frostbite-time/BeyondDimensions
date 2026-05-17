package com.wintercogs.beyonddimensions.api.dimensionnet;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public record PrimaryNetOption(int netId, @NotNull NetPermissionlevel permission, @NotNull String customName)
{
    private static final String NET_ID = "net_id";
    private static final String PERMISSION = "permission";
    private static final String CUSTOM_NAME = "custom_name";

    public CompoundTag save()
    {
        CompoundTag tag = new CompoundTag();
        tag.putInt(NET_ID, netId);
        tag.putString(PERMISSION, permission.name());
        if (!customName.isEmpty())
            tag.putString(CUSTOM_NAME, customName);
        return tag;
    }

    public Component getNetworkName()
    {
        return DimensionsNet.getNetworkName(netId, customName);
    }

    public static PrimaryNetOption load(CompoundTag tag)
    {
        return new PrimaryNetOption(
                tag.getInt(NET_ID),
                tag.contains(PERMISSION) ? NetPermissionlevel.valueOf(tag.getString(PERMISSION)) : NetPermissionlevel.Member,
                tag.contains(CUSTOM_NAME) ? tag.getString(CUSTOM_NAME) : ""
        );
    }
}
